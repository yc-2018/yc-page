package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.common.exception.SqlSaveException;
import ikun.yc.ycpage.common.exception.SqlUpdateException;
import ikun.yc.ycpage.entity.MiniCheckinComment;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.MiniUser;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchItem;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentReplyListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentResponse;
import ikun.yc.ycpage.mapper.MiniCheckinCommentMapper;
import ikun.yc.ycpage.service.MiniCheckinCommentService;
import ikun.yc.ycpage.service.MiniCheckinRecordsService;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import ikun.yc.ycpage.service.MiniSubscribeMessageService;
import ikun.yc.ycpage.service.MiniUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序打卡评论 Service 实现
 *
 * @author cgl
 * @since 2026/11/03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiniCheckinCommentServiceImpl extends ServiceImpl<MiniCheckinCommentMapper, MiniCheckinComment>
        implements MiniCheckinCommentService {
    private static final String SHARE_TYPE_DYNAMIC = "dynamic"; // 动态分享类型
    private static final String SHARE_TYPE_STATIC = "static"; // 静态分享类型
    private static final String SHARE_TYPE_MULTI = "multi"; // 多地分享类型
    private static final String SHARE_NOT_FOUND_MSG = "分享已过期或不存在"; // 分享不可访问的统一提示
    private static final String STATIC_SHARE_COMMENT_MSG = "静态分享不支持评论"; // 静态分享评论提示
    private static final String PROFILE_REQUIRED_MSG = "请先设置头像和昵称后再评论"; // 未完善资料提示
    private static final int ROOT_PAGE_SIZE = 5; // 顶级评论每页数量
    private static final int REPLY_PAGE_SIZE = 10; // 回复每页数量
    private static final int REPLY_PREVIEW_SIZE = 2; // 评论卡片内预览回复数量
    private static final int MAX_REPLIES_PER_ROOT = 50; // 单条顶级评论最多回复数量
    private static final int MAX_NON_OWNER_THREAD_COMMENTS = 3; // 主人未回复前非主人最多评论数量
    private static final int MAX_BATCH_RECORDS = 100; // 批量查询最多记录数量

    private final MiniCheckinRecordsService miniCheckinRecordsService;
    private final MiniCheckinShareService miniCheckinShareService;
    private final MiniUserService miniUserService;
    private final WechatMiniAuthService wechatMiniAuthService;
    private final MiniSubscribeMessageService miniSubscribeMessageService;

    /**
     * 发表评论或回复
     *
     * @param request 评论创建请求
     * @return 新评论
     */
    @Override
    public MiniCheckinCommentResponse createComment(MiniCheckinCommentCreateRequest request) {
        if (request == null) {
            throw new ParamException("参数不能为空");
        }
        String viewer = currentOpenid(); // 当前评论人openid
        String content = request.getContent() == null ? "" : request.getContent().trim(); // 清洗后的评论内容
        if (!StringUtils.hasText(content)) {
            throw new ParamException("评论内容不能为空");
        }
        if (content.length() > 200) {
            throw new ParamException("评论不能超过200字");
        }

        CommentContext context = resolveContext(request.getRecordId(), request.getShareId(), viewer, true); // 当前评论场景
        if (!context.commentsEnabled) {
            throw new ParamException("该分享未开启评论");
        }
        requireCompleteProfile(viewer);

        MiniCheckinComment replyToComment = null; // 直接回复的评论
        int rootId = 0; // 所属顶级评论ID
        if (request.getReplyToCommentId() != null && request.getReplyToCommentId() > 0) {
            replyToComment = getById(request.getReplyToCommentId());
            if (replyToComment == null || !Objects.equals(replyToComment.getRecordId(), request.getRecordId())) {
                throw new ParamException("回复的评论不存在");
            }
            if (Integer.valueOf(1).equals(replyToComment.getIsDeleted())) {
                throw new ParamException("回复的评论已删除");
            }
            rootId = Integer.valueOf(0).equals(replyToComment.getRootId())
                    ? replyToComment.getId()
                    : replyToComment.getRootId();
            MiniCheckinComment rootComment = Integer.valueOf(rootId).equals(replyToComment.getId())
                    ? replyToComment
                    : getById(rootId); // 回复所属的顶级评论
            if (rootComment == null || Integer.valueOf(1).equals(rootComment.getIsDeleted())) {
                throw new ParamException("评论不存在");
            }
            if (!context.isOwner && !context.commentsPublic) {
                QueryWrapper<MiniCheckinComment> rootWrapper = new QueryWrapper<>();
                rootWrapper.eq("id", rootId);
                applyRootVisibility(rootWrapper, false, context.ownerOpenid, false, viewer);
                if (count(rootWrapper) == 0) {
                    throw new ParamException("评论不存在");
                }
            }
            long replyCount = count(new QueryWrapper<MiniCheckinComment>().eq("root_id", rootId)); // 当前回复总数
            if (replyCount >= MAX_REPLIES_PER_ROOT) {
                throw new ParamException("该评论的回复已达上限");
            }
            if (!context.isOwner) {
                checkNonOwnerThreadLimit(rootId, viewer, context.ownerOpenid);
            }
        }

        wechatMiniAuthService.checkText(content, viewer);

        MiniCheckinComment comment = new MiniCheckinComment();
        comment.setRecordId(request.getRecordId());
        comment.setUserOpenid(viewer);
        comment.setContent(content);
        comment.setRootId(rootId);
        comment.setReplyToCommentId(replyToComment == null ? 0 : replyToComment.getId());
        comment.setReplyToOpenid(replyToComment == null ? "" : replyToComment.getUserOpenid());
        comment.setIsDeleted(0);
        if (!save(comment)) {
            throw new SqlSaveException("评论失败");
        }

        Set<String> openids = new HashSet<>(); // 本次响应需要展示资料的用户
        openids.add(viewer);
        if (replyToComment != null) {
            openids.add(replyToComment.getUserOpenid());
        }
        Map<String, MiniUser> users = loadUsers(openids); // 评论相关用户资料
        MiniCheckinCommentResponse response = buildResponse(comment, viewer, context.ownerOpenid, users); // 新评论响应
        response.setReplyCount(0);
        response.setHasMoreReplies(false);
        response.setReplies(Collections.emptyList());

        MiniUser author = users.get(viewer); // 当前评论人资料
        String authorNickname = author != null && StringUtils.hasText(author.getNickname())
                ? author.getNickname().trim()
                : "微信用户"; // 订阅消息中展示的评论人昵称
        sendCommentSubscribeMessages(comment, context, replyToComment, viewer, authorNickname, request.getShareId());
        return response;
    }

    /**
     * 发送评论相关订阅消息，失败不影响评论结果
     *
     * @param comment      新评论
     * @param context      当前评论场景
     * @param replyToComment 直接回复的评论，顶级评论为空
     * @param viewer       当前评论人openid
     * @param authorNickname 当前评论人昵称
     * @param shareId      分享ID，可为空
     */
    private void sendCommentSubscribeMessages(MiniCheckinComment comment, CommentContext context,
                                              MiniCheckinComment replyToComment, String viewer,
                                              String authorNickname, Integer shareId) {
        try {
            LocalDateTime noticeTime = comment.getCreateTime() == null
                    ? LocalDateTime.now()
                    : comment.getCreateTime(); // 订阅消息展示的评论时间
            if (replyToComment != null) {
                String targetOpenid = replyToComment.getUserOpenid(); // 被回复人openid
                if (StringUtils.hasText(targetOpenid) && !targetOpenid.equals(viewer)) {
                    miniSubscribeMessageService.notifyReply(targetOpenid, context.noticeProjectName,
                            authorNickname, comment.getContent(), noticeTime, shareId, context.shareType);
                }
                if (!context.ownerOpenid.equals(viewer) && !context.ownerOpenid.equals(targetOpenid)) {
                    miniSubscribeMessageService.notifyNewComment(context.ownerOpenid, context.noticeProjectName,
                            authorNickname, comment.getContent(), noticeTime, shareId, context.shareType);
                }
                return;
            }
            if (!context.ownerOpenid.equals(viewer)) {
                miniSubscribeMessageService.notifyNewComment(context.ownerOpenid, context.noticeProjectName,
                        authorNickname, comment.getContent(), noticeTime, shareId, context.shareType);
            }
        } catch (Exception ex) {
            log.warn("发送评论订阅消息失败, commentId={}", comment.getId(), ex);
        }
    }

    /**
     * 分页查询顶级评论
     *
     * @param recordId 打卡记录ID
     * @param page     第几页
     * @param shareId  分享ID，从打卡记录列表进入时为空
     * @return 顶级评论分页
     */
    @Override
    public MiniCheckinCommentListResponse listComments(Integer recordId, Integer page, Integer shareId) {
        String viewer = currentOpenid(); // 当前访问者openid
        CommentContext context = resolveContext(recordId, shareId, viewer, false); // 当前评论场景
        long currentPage = normalizePage(page); // 当前页码

        MiniCheckinCommentListResponse response = new MiniCheckinCommentListResponse();
        response.setPage(currentPage);
        response.setSize((long) ROOT_PAGE_SIZE);
        response.setCommentsEnabled(context.commentsEnabled);
        response.setCommentsPublic(context.commentsPublic);
        response.setIsOwner(context.isOwner);
        if (!context.commentsEnabled) {
            response.setList(Collections.emptyList());
            response.setTotal(0L);
            response.setHasMore(false);
            return response;
        }

        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.eq("record_id", recordId);
        applyRootVisibility(wrapper, context.isOwner, context.ownerOpenid, context.commentsPublic, viewer);
        wrapper.orderByDesc("create_time").orderByDesc("id"); // 顶级评论最新在前
        Page<MiniCheckinComment> commentPage = page(new Page<>(currentPage, ROOT_PAGE_SIZE), wrapper); // 顶级评论分页
        Map<Integer, MiniCheckinCommentResponse> responseMap = buildRootResponseMap(
                commentPage.getRecords(), context.isOwner, context.ownerOpenid, viewer, REPLY_PREVIEW_SIZE); // 评论ID到响应
        response.setList(commentPage.getRecords().stream()
                .map(comment -> responseMap.get(comment.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        response.setTotal(commentPage.getTotal());
        response.setHasMore(commentPage.getCurrent() * commentPage.getSize() < commentPage.getTotal());
        return response;
    }

    /**
     * 分页查询回复，按时间正序
     *
     * @param rootId  顶级评论ID
     * @param page    第几页
     * @param shareId 分享ID，从打卡记录列表进入时为空
     * @return 回复分页
     */
    @Override
    public MiniCheckinCommentReplyListResponse listReplies(Integer rootId, Integer page, Integer shareId) {
        if (rootId == null) {
            throw new ParamException("评论不存在");
        }
        String viewer = currentOpenid(); // 当前访问者openid
        MiniCheckinComment rootComment = getById(rootId); // 顶级评论
        if (rootComment == null || !Integer.valueOf(0).equals(rootComment.getRootId())) {
            throw new ParamException("评论不存在");
        }
        CommentContext context = resolveContext(rootComment.getRecordId(), shareId, viewer, false); // 当前评论场景
        long currentPage = normalizePage(page); // 当前页码

        MiniCheckinCommentReplyListResponse response = new MiniCheckinCommentReplyListResponse();
        response.setPage(currentPage);
        response.setSize((long) REPLY_PAGE_SIZE);
        response.setCommentsEnabled(context.commentsEnabled);
        response.setCommentsPublic(context.commentsPublic);
        response.setIsOwner(context.isOwner);
        if (!context.commentsEnabled) {
            response.setList(Collections.emptyList());
            response.setTotal(0L);
            response.setHasMore(false);
            return response;
        }

        QueryWrapper<MiniCheckinComment> rootWrapper = new QueryWrapper<>();
        rootWrapper.eq("id", rootId);
        applyRootVisibility(rootWrapper, context.isOwner, context.ownerOpenid, context.commentsPublic, viewer);
        if (count(rootWrapper) == 0) {
            throw new ParamException("评论不存在");
        }

        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.eq("root_id", rootId);
        if (!context.isOwner) {
            wrapper.eq("is_deleted", 0);
        }
        wrapper.orderByAsc("create_time").orderByAsc("id"); // 回复按时间正序
        Page<MiniCheckinComment> replyPage = page(new Page<>(currentPage, REPLY_PAGE_SIZE), wrapper); // 回复分页
        Map<String, MiniUser> users = loadUsers(collectOpenids(replyPage.getRecords())); // 回复人资料
        response.setList(replyPage.getRecords().stream()
                .map(reply -> buildResponse(reply, viewer, context.ownerOpenid, users))
                .collect(Collectors.toList()));
        response.setTotal(replyPage.getTotal());
        response.setHasMore(replyPage.getCurrent() * replyPage.getSize() < replyPage.getTotal());
        return response;
    }

    /**
     * 批量查询打卡记录的评论预览
     *
     * @param request 批量预览请求
     * @return 批量评论预览
     */
    @Override
    public MiniCheckinCommentBatchResponse batchPreview(MiniCheckinCommentBatchRequest request) {
        if (request == null) {
            throw new ParamException("参数不能为空");
        }
        List<Integer> requestedIds = normalizeRecordIds(request.getRecordIds()); // 去重后的记录ID
        if (requestedIds.isEmpty()) {
            throw new ParamException("打卡记录不能为空");
        }
        if (requestedIds.size() > MAX_BATCH_RECORDS) {
            throw new ParamException("最多查询" + MAX_BATCH_RECORDS + "条打卡记录");
        }
        int previewSize = normalizePreviewSize(request.getPreviewSize()); // 每条记录的预览评论数
        String viewer = currentOpenid(); // 当前访问者openid
        BatchContext context = resolveBatchContext(requestedIds, request.getShareId(), viewer); // 批量访问场景

        List<Integer> accessibleIds = requestedIds.stream()
                .filter(context.records::containsKey)
                .collect(Collectors.toList()); // 当前访问者可见的记录ID
        Map<Integer, Long> rootCounts = Collections.emptyMap(); // 每条记录的可见顶级评论数
        Map<Integer, List<MiniCheckinComment>> previewRootsByRecord = new HashMap<>(); // 每条记录的预览评论
        List<MiniCheckinComment> allPreviewRoots = new ArrayList<>(); // 所有记录的预览评论
        if (context.commentsEnabled && !accessibleIds.isEmpty()) {
            rootCounts = loadRootCounts(accessibleIds, context, viewer);
            for (Integer recordId : accessibleIds) {
                if (rootCounts.getOrDefault(recordId, 0L) <= 0) {
                    continue;
                }
                List<MiniCheckinComment> roots = loadRootPreview(recordId, context, viewer, previewSize);
                previewRootsByRecord.put(recordId, roots);
                allPreviewRoots.addAll(roots);
            }
        }
        Map<Integer, MiniCheckinCommentResponse> responseMap = buildRootResponseMap(
                allPreviewRoots, context.isOwner, context.ownerOpenid, viewer, REPLY_PREVIEW_SIZE); // 评论ID到响应

        List<MiniCheckinCommentBatchItem> items = new ArrayList<>(); // 按请求顺序返回的预览
        for (Integer recordId : accessibleIds) {
            MiniCheckinCommentBatchItem item = new MiniCheckinCommentBatchItem();
            item.setRecordId(recordId);
            item.setTotal(rootCounts.getOrDefault(recordId, 0L));
            item.setList(previewRootsByRecord.getOrDefault(recordId, Collections.emptyList()).stream()
                    .map(root -> responseMap.get(root.getId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            items.add(item);
        }

        MiniCheckinCommentBatchResponse response = new MiniCheckinCommentBatchResponse();
        response.setRecords(items);
        response.setCommentsEnabled(context.commentsEnabled);
        response.setCommentsPublic(context.commentsPublic);
        response.setIsOwner(context.isOwner);
        return response;
    }

    /**
     * 删除评论，记录主人硬删除，评论作者软删除
     *
     * @param commentId 评论ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(Integer commentId) {
        if (commentId == null) {
            throw new ParamException("评论不存在");
        }
        String viewer = currentOpenid(); // 当前访问者openid
        MiniCheckinComment comment = getById(commentId); // 待删除评论
        if (comment == null) {
            throw new ParamException("评论不存在");
        }
        MiniCheckinRecords record = miniCheckinRecordsService.getById(comment.getRecordId()); // 评论所属记录
        if (record == null) {
            throw new ParamException("评论不存在");
        }

        if (viewer.equals(record.getUserOpenid())) {
            if (Integer.valueOf(0).equals(comment.getRootId())) {
                remove(new QueryWrapper<MiniCheckinComment>()
                        .eq("id", commentId)
                        .or()
                        .eq("root_id", commentId));
            } else {
                removeByIds(collectDescendantIds(commentId));
            }
            return true;
        }
        if (!viewer.equals(comment.getUserOpenid())) {
            throw new ParamException("无权删除该评论");
        }
        if (Integer.valueOf(1).equals(comment.getIsDeleted())) {
            return true;
        }
        MiniCheckinComment updateEntity = new MiniCheckinComment();
        updateEntity.setId(commentId);
        updateEntity.setIsDeleted(1);
        if (!updateById(updateEntity)) {
            throw new SqlUpdateException("删除评论失败");
        }
        return true;
    }

    /**
     * 校验当前评论场景，返回主人身份和评论开关
     *
     * @param recordId 打卡记录ID
     * @param shareId  分享ID，可为空
     * @param viewer   当前访问者openid
     * @param forWrite 是否为写操作
     * @return 评论场景
     */
    private CommentContext resolveContext(Integer recordId, Integer shareId, String viewer, boolean forWrite) {
        if (recordId == null) {
            throw new ParamException("打卡记录不存在");
        }
        MiniCheckinRecords record = miniCheckinRecordsService.getOne(
                new QueryWrapper<MiniCheckinRecords>()
                        .eq("id", recordId)
                        .eq("is_deleted", 0)
                        .last("limit 1")); // 未删除的打卡记录
        if (record == null) {
            throw new ParamException("打卡记录不存在");
        }
        if (shareId == null) {
            if (!viewer.equals(record.getUserOpenid())) {
                throw new ParamException("无权查看该评论");
            }
            return new CommentContext(record.getUserOpenid(), true, true, true,
                    resolveNoticeProjectName(record), null);
        }

        MiniCheckinShare share = requireShare(shareId);
        if (!Objects.equals(share.getUserOpenid(), record.getUserOpenid())) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        if (!isRecordInShare(share, recordId)) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        boolean isOwner = viewer.equals(share.getUserOpenid()); // 当前访问者是否记录主人
        boolean expired = isShareExpired(share); // 分享是否已过期
        if (expired) {
            if (forWrite) {
                throw new ParamException("分享已过期，无法评论");
            }
            if (!isOwner) {
                throw new ParamException(SHARE_NOT_FOUND_MSG);
            }
        }
        boolean commentsEnabled = !Integer.valueOf(0).equals(share.getCommentsEnabled()); // 是否允许评论
        boolean commentsPublic = commentsEnabled && Integer.valueOf(1).equals(share.getCommentsPublic()); // 是否公开全部评论
        return new CommentContext(share.getUserOpenid(), isOwner, commentsEnabled, commentsPublic,
                resolveNoticeProjectName(record), share.getShareType());
    }

    /**
     * 生成订阅消息中的评论项目名称
     *
     * @param record 打卡记录
     * @return 优先使用地点名称，缺失时使用地址
     */
    private String resolveNoticeProjectName(MiniCheckinRecords record) {
        if (StringUtils.hasText(record.getName())) {
            return record.getName().trim();
        }
        if (StringUtils.hasText(record.getAddress())) {
            return record.getAddress().trim();
        }
        return "打卡分享";
    }

    /**
     * 校验批量预览场景
     *
     * @param requestedIds 请求的记录ID
     * @param shareId      分享ID，可为空
     * @param viewer       当前访问者openid
     * @return 批量访问场景
     */
    private BatchContext resolveBatchContext(List<Integer> requestedIds, Integer shareId, String viewer) {
        if (shareId == null) {
            List<MiniCheckinRecords> records = miniCheckinRecordsService.list(
                    new QueryWrapper<MiniCheckinRecords>()
                            .in("id", requestedIds)
                            .eq("user_openid", viewer)
                            .eq("is_deleted", 0)); // 当前用户自己的未删除记录
            return new BatchContext(toRecordMap(records), viewer, true, true, true);
        }

        MiniCheckinShare share = requireShare(shareId);
        boolean isOwner = viewer.equals(share.getUserOpenid()); // 当前访问者是否记录主人
        if (isShareExpired(share) && !isOwner) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        Set<Integer> allowedRecordIds = SHARE_TYPE_MULTI.equals(share.getShareType())
                ? new HashSet<>(miniCheckinShareService.parseShareRecordIds(share.getContent()))
                : Collections.singleton(share.getRecordId()); // 当前分享包含的记录ID
        List<Integer> accessibleIds = requestedIds.stream()
                .filter(allowedRecordIds::contains)
                .collect(Collectors.toList()); // 请求中属于分享的记录ID
        Map<Integer, MiniCheckinRecords> recordMap = Collections.emptyMap(); // 可访问的未删除记录
        if (!accessibleIds.isEmpty()) {
            recordMap = toRecordMap(miniCheckinRecordsService.list(
                    new QueryWrapper<MiniCheckinRecords>()
                            .in("id", accessibleIds)
                            .eq("user_openid", share.getUserOpenid())
                            .eq("is_deleted", 0)));
        }
        boolean commentsEnabled = !Integer.valueOf(0).equals(share.getCommentsEnabled()); // 是否允许评论
        boolean commentsPublic = commentsEnabled && Integer.valueOf(1).equals(share.getCommentsPublic()); // 是否公开全部评论
        return new BatchContext(recordMap, share.getUserOpenid(), isOwner, commentsEnabled, commentsPublic);
    }

    /**
     * 读取分享记录并校验评论场景
     *
     * @param shareId 分享ID
     * @return 分享记录
     */
    private MiniCheckinShare requireShare(Integer shareId) {
        MiniCheckinShare share = miniCheckinShareService.getById(shareId); // 分享记录
        if (share == null) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        if (SHARE_TYPE_STATIC.equals(share.getShareType())) {
            throw new ParamException(STATIC_SHARE_COMMENT_MSG);
        }
        if (!SHARE_TYPE_DYNAMIC.equals(share.getShareType()) && !SHARE_TYPE_MULTI.equals(share.getShareType())) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        return share;
    }

    /**
     * 判断打卡记录是否属于分享
     *
     * @param share    分享记录
     * @param recordId 打卡记录ID
     * @return 是否属于分享
     */
    private boolean isRecordInShare(MiniCheckinShare share, Integer recordId) {
        if (SHARE_TYPE_MULTI.equals(share.getShareType())) {
            return miniCheckinShareService.parseShareRecordIds(share.getContent()).contains(recordId);
        }
        return Objects.equals(share.getRecordId(), recordId);
    }

    /**
     * 判断分享是否已过期
     *
     * @param share 分享记录
     * @return 是否过期
     */
    private boolean isShareExpired(MiniCheckinShare share) {
        return share.getExpireTime() == null || !share.getExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 校验当前用户是否已设置头像和昵称
     *
     * @param openid 当前用户openid
     */
    private void requireCompleteProfile(String openid) {
        MiniUser user = miniUserService.getOne(new QueryWrapper<MiniUser>()
                .eq("openid", openid)
                .last("limit 1")); // 当前用户资料
        if (user == null || !StringUtils.hasText(user.getNickname()) || !StringUtils.hasText(user.getAvatarUrl())) {
            throw new ParamException(PROFILE_REQUIRED_MSG);
        }
    }

    /**
     * 校验非主人在主人未回复前的评论次数
     *
     * @param rootId      顶级评论ID
     * @param viewer      当前评论人openid
     * @param ownerOpenid 记录主人openid
     */
    private void checkNonOwnerThreadLimit(Integer rootId, String viewer, String ownerOpenid) {
        MiniCheckinComment lastOwnerComment = getOne(new QueryWrapper<MiniCheckinComment>()
                .select("id")
                .and(wrapper -> wrapper.eq("root_id", rootId).or().eq("id", rootId))
                .eq("user_openid", ownerOpenid)
                .orderByDesc("id")
                .last("limit 1")); // 主人在该评论串的最后一条评论
        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_openid", viewer)
                .and(condition -> condition.eq("root_id", rootId).or().eq("id", rootId));
        if (lastOwnerComment != null) {
            wrapper.gt("id", lastOwnerComment.getId());
        }
        long commentCount = count(wrapper); // 主人最后回复后当前用户的评论数
        if (commentCount >= MAX_NON_OWNER_THREAD_COMMENTS) {
            throw new ParamException("请等待主人回复后再继续评论");
        }
    }

    /**
     * 给顶级评论查询追加可见性条件
     *
     * @param wrapper       查询条件
     * @param isOwner       当前访问者是否记录主人
     * @param ownerOpenid   记录主人openid
     * @param commentsPublic 是否公开全部评论
     * @param viewer        当前访问者openid
     */
    private void applyRootVisibility(QueryWrapper<MiniCheckinComment> wrapper, boolean isOwner,
                                     String ownerOpenid, boolean commentsPublic, String viewer) {
        wrapper.eq("root_id", 0);
        if (isOwner) {
            return;
        }
        wrapper.eq("is_deleted", 0);
        if (commentsPublic) {
            return;
        }
        wrapper.and(condition -> condition
                .eq("user_openid", viewer)
                .or()
                .eq("user_openid", ownerOpenid)
                .or()
                .apply("id IN (SELECT root_id FROM mini_checkin_comment WHERE root_id <> 0 AND reply_to_openid = {0} AND is_deleted = 0)", viewer));
    }

    /**
     * 批量统计每条记录的可见顶级评论数
     *
     * @param recordIds 记录ID
     * @param context   批量访问场景
     * @param viewer    当前访问者openid
     * @return 记录ID到评论数的映射
     */
    private Map<Integer, Long> loadRootCounts(List<Integer> recordIds, BatchContext context, String viewer) {
        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.select("record_id", "COUNT(*) AS total")
                .in("record_id", recordIds);
        applyRootVisibility(wrapper, context.isOwner, context.ownerOpenid, context.commentsPublic, viewer);
        wrapper.groupBy("record_id");
        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper); // 分组统计结果
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object recordIdValue = getMapValue(row, "record_id"); // 记录ID列
            Object totalValue = getMapValue(row, "total"); // 评论数
            if (recordIdValue instanceof Number && totalValue instanceof Number) {
                result.put(((Number) recordIdValue).intValue(), ((Number) totalValue).longValue());
            }
        }
        return result;
    }

    /**
     * 查询单条记录的顶级评论预览
     *
     * @param recordId 记录ID
     * @param context  批量访问场景
     * @param viewer   当前访问者openid
     * @param limit    预览数量
     * @return 顶级评论列表
     */
    private List<MiniCheckinComment> loadRootPreview(Integer recordId, BatchContext context, String viewer, int limit) {
        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.eq("record_id", recordId);
        applyRootVisibility(wrapper, context.isOwner, context.ownerOpenid, context.commentsPublic, viewer);
        wrapper.orderByDesc("create_time").orderByDesc("id").last("limit " + limit);
        return list(wrapper);
    }

    /**
     * 批量统计顶级评论的回复数量
     *
     * @param rootIds 顶级评论ID
     * @param isOwner 当前访问者是否记录主人
     * @return 顶级评论ID到回复数的映射
     */
    private Map<Integer, Long> loadReplyCounts(List<Integer> rootIds, boolean isOwner) {
        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.select("root_id", "COUNT(*) AS total")
                .in("root_id", rootIds);
        if (!isOwner) {
            wrapper.eq("is_deleted", 0);
        }
        wrapper.groupBy("root_id");
        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper); // 分组统计结果
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object rootIdValue = getMapValue(row, "root_id"); // 顶级评论ID列
            Object totalValue = getMapValue(row, "total"); // 回复数
            if (rootIdValue instanceof Number && totalValue instanceof Number) {
                result.put(((Number) rootIdValue).intValue(), ((Number) totalValue).longValue());
            }
        }
        return result;
    }

    /**
     * 查询顶级评论的回复预览，按时间正序
     *
     * @param rootId 顶级评论ID
     * @param isOwner 当前访问者是否记录主人
     * @param limit   预览数量
     * @return 回复列表
     */
    private List<MiniCheckinComment> loadReplyPreview(Integer rootId, boolean isOwner, int limit) {
        QueryWrapper<MiniCheckinComment> wrapper = new QueryWrapper<>();
        wrapper.eq("root_id", rootId);
        if (!isOwner) {
            wrapper.eq("is_deleted", 0);
        }
        wrapper.orderByAsc("create_time").orderByAsc("id").last("limit " + limit);
        return list(wrapper);
    }

    /**
     * 构建顶级评论响应，并填充回复数量和回复预览
     *
     * @param roots           顶级评论
     * @param isOwner         当前访问者是否记录主人
     * @param ownerOpenid     记录主人openid
     * @param viewer          当前访问者openid
     * @param replyPreviewSize 回复预览数量
     * @return 评论ID到响应的映射
     */
    private Map<Integer, MiniCheckinCommentResponse> buildRootResponseMap(List<MiniCheckinComment> roots,
                                                                          boolean isOwner,
                                                                          String ownerOpenid,
                                                                          String viewer,
                                                                          int replyPreviewSize) {
        if (roots == null || roots.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> rootIds = roots.stream().map(MiniCheckinComment::getId).collect(Collectors.toList()); // 顶级评论ID
        Map<Integer, Long> replyCounts = loadReplyCounts(rootIds, isOwner); // 回复数量
        Map<Integer, List<MiniCheckinComment>> previewReplies = new HashMap<>(); // 回复预览
        for (Integer rootId : rootIds) {
            if (replyCounts.getOrDefault(rootId, 0L) > 0) {
                previewReplies.put(rootId, loadReplyPreview(rootId, isOwner, replyPreviewSize));
            }
        }

        Set<String> openids = new HashSet<>(collectOpenids(roots)); // 响应涉及的用户openid
        for (List<MiniCheckinComment> replies : previewReplies.values()) {
            openids.addAll(collectOpenids(replies));
        }
        Map<String, MiniUser> users = loadUsers(openids); // 用户资料

        Map<Integer, MiniCheckinCommentResponse> result = new LinkedHashMap<>();
        for (MiniCheckinComment root : roots) {
            MiniCheckinCommentResponse response = buildResponse(root, viewer, ownerOpenid, users); // 顶级评论响应
            long replyCount = replyCounts.getOrDefault(root.getId(), 0L); // 当前评论的回复数
            List<MiniCheckinComment> replies = previewReplies.getOrDefault(root.getId(), Collections.emptyList()); // 回复预览
            response.setReplyCount((int) replyCount);
            response.setHasMoreReplies(replyCount > replies.size());
            response.setReplies(replies.stream()
                    .map(reply -> buildResponse(reply, viewer, ownerOpenid, users))
                    .collect(Collectors.toList()));
            result.put(root.getId(), response);
        }
        return result;
    }

    /**
     * 构建单条评论响应
     *
     * @param comment     评论
     * @param viewer      当前访问者openid
     * @param ownerOpenid 记录主人openid
     * @param users       用户资料
     * @return 评论响应
     */
    private MiniCheckinCommentResponse buildResponse(MiniCheckinComment comment,
                                                     String viewer,
                                                     String ownerOpenid,
                                                     Map<String, MiniUser> users) {
        MiniCheckinCommentResponse response = new MiniCheckinCommentResponse();
        response.setId(comment.getId());
        response.setRecordId(comment.getRecordId());
        response.setRootId(comment.getRootId());
        response.setReplyToCommentId(comment.getReplyToCommentId());
        response.setCreateTime(comment.getCreateTime());
        boolean deleted = Integer.valueOf(1).equals(comment.getIsDeleted()); // 是否已软删除
        boolean isMine = viewer.equals(comment.getUserOpenid()); // 是否当前用户发表
        boolean isOwner = viewer.equals(ownerOpenid); // 是否记录主人
        response.setIsDeleted(deleted);
        response.setIsMine(isMine);
        response.setCanDelete(isOwner || (isMine && !deleted));
        if (deleted) {
            response.setContent(null);
            return response;
        }

        response.setContent(comment.getContent());
        MiniUser author = users.get(comment.getUserOpenid()); // 评论人资料
        if (author != null) {
            response.setNickname(author.getNickname());
            response.setAvatarUrl(author.getAvatarUrl());
        }
        if (StringUtils.hasText(comment.getReplyToOpenid())) {
            MiniUser replyToUser = users.get(comment.getReplyToOpenid()); // 被回复用户资料
            if (replyToUser != null) {
                response.setReplyToNickname(replyToUser.getNickname());
            }
        }
        return response;
    }

    /**
     * 收集评论涉及的用户openid
     *
     * @param comments 评论
     * @return 用户openid集合
     */
    private Set<String> collectOpenids(Collection<MiniCheckinComment> comments) {
        Set<String> openids = new HashSet<>();
        for (MiniCheckinComment comment : comments) {
            if (StringUtils.hasText(comment.getUserOpenid())) {
                openids.add(comment.getUserOpenid());
            }
            if (StringUtils.hasText(comment.getReplyToOpenid())) {
                openids.add(comment.getReplyToOpenid());
            }
        }
        return openids;
    }

    /**
     * 批量读取用户资料
     *
     * @param openids 用户openid
     * @return openid到用户资料的映射
     */
    private Map<String, MiniUser> loadUsers(Collection<String> openids) {
        if (openids == null || openids.isEmpty()) {
            return Collections.emptyMap();
        }
        return miniUserService.list(new QueryWrapper<MiniUser>().in("openid", openids))
                .stream()
                .collect(Collectors.toMap(MiniUser::getOpenid, user -> user, (left, right) -> left));
    }

    /**
     * 收集评论及其所有子回复ID，用于主人硬删除
     *
     * @param commentId 评论ID
     * @return 需要删除的评论ID
     */
    private List<Integer> collectDescendantIds(Integer commentId) {
        List<Integer> ids = new ArrayList<>();
        ids.add(commentId);
        List<Integer> parentIds = new ArrayList<>();
        parentIds.add(commentId);
        while (!parentIds.isEmpty()) {
            List<MiniCheckinComment> children = list(new QueryWrapper<MiniCheckinComment>()
                    .select("id")
                    .in("reply_to_comment_id", parentIds)); // 直接子回复
            List<Integer> nextParentIds = new ArrayList<>();
            for (MiniCheckinComment child : children) {
                if (!ids.contains(child.getId())) {
                    ids.add(child.getId());
                    nextParentIds.add(child.getId());
                }
            }
            parentIds = nextParentIds;
        }
        return ids;
    }

    /**
     * 将打卡记录转换为ID映射
     *
     * @param records 打卡记录
     * @return 记录ID到记录的映射
     */
    private Map<Integer, MiniCheckinRecords> toRecordMap(List<MiniCheckinRecords> records) {
        return records.stream()
                .collect(Collectors.toMap(MiniCheckinRecords::getId, record -> record, (left, right) -> left));
    }

    /**
     * 读取当前登录用户openid
     *
     * @return 当前用户openid
     */
    private String currentOpenid() {
        String openid = BaseContext.getCurrentId(); // 当前登录用户openid
        if (!StringUtils.hasText(openid)) {
            throw new ParamException("登录信息有误");
        }
        return openid;
    }

    /**
     * 标准化页码
     *
     * @param page 原始页码
     * @return 有效页码
     */
    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /**
     * 标准化预览评论数量
     *
     * @param previewSize 原始预览数量
     * @return 1到5之间的预览数量
     */
    private int normalizePreviewSize(Integer previewSize) {
        if (previewSize == null) {
            return 3;
        }
        return Math.max(1, Math.min(5, previewSize));
    }

    /**
     * 过滤空ID并去重，保持传入顺序
     *
     * @param recordIds 原始记录ID
     * @return 清洗后的记录ID
     */
    private List<Integer> normalizeRecordIds(List<Integer> recordIds) {
        if (recordIds == null) {
            return Collections.emptyList();
        }
        return recordIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 忽略大小写读取分组统计结果
     *
     * @param row 单行统计结果
     * @param key 字段名
     * @return 字段值
     */
    private Object getMapValue(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 单条评论场景
     */
    private static final class CommentContext {
        private final String ownerOpenid; // 记录主人openid
        private final boolean isOwner; // 当前访问者是否记录主人
        private final boolean commentsEnabled; // 是否允许评论
        private final boolean commentsPublic; // 是否公开全部评论
        private final String noticeProjectName; // 订阅消息中的评论项目名称
        private final String shareType; // 分享类型，用于订阅消息跳转，记录列表评论为空

        private CommentContext(String ownerOpenid, boolean isOwner, boolean commentsEnabled,
                               boolean commentsPublic, String noticeProjectName, String shareType) {
            this.ownerOpenid = ownerOpenid;
            this.isOwner = isOwner;
            this.commentsEnabled = commentsEnabled;
            this.commentsPublic = commentsPublic;
            this.noticeProjectName = noticeProjectName;
            this.shareType = shareType;
        }
    }

    /**
     * 批量评论预览场景
     */
    private static final class BatchContext {
        private final Map<Integer, MiniCheckinRecords> records; // 可访问的记录
        private final String ownerOpenid; // 记录主人openid
        private final boolean isOwner; // 当前访问者是否记录主人
        private final boolean commentsEnabled; // 是否允许评论
        private final boolean commentsPublic; // 是否公开全部评论

        private BatchContext(Map<Integer, MiniCheckinRecords> records, String ownerOpenid, boolean isOwner,
                             boolean commentsEnabled, boolean commentsPublic) {
            this.records = records;
            this.ownerOpenid = ownerOpenid;
            this.isOwner = isOwner;
            this.commentsEnabled = commentsEnabled;
            this.commentsPublic = commentsPublic;
        }
    }
}
