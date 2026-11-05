package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniCheckinComment;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.MiniUser;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentResponse;
import ikun.yc.ycpage.mapper.MiniCheckinCommentMapper;
import ikun.yc.ycpage.service.MiniCheckinRecordsService;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import ikun.yc.ycpage.service.MiniSubscribeMessageService;
import ikun.yc.ycpage.service.MiniUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 小程序打卡评论 Service 单元测试
 *
 * @author cgl
 * @since 2026/11/03
 */
class MiniCheckinCommentServiceImplTest {
    private MiniCheckinCommentServiceImpl service; // 被测评论服务
    private MiniCheckinCommentMapper commentMapper; // 模拟评论数据访问层
    private MiniCheckinRecordsService recordsService; // 模拟打卡记录服务
    private MiniCheckinShareService shareService; // 模拟分享服务
    private MiniUserService userService; // 模拟用户服务
    private WechatMiniAuthService wechatMiniAuthService; // 模拟微信内容安全服务
    private MiniSubscribeMessageService miniSubscribeMessageService; // 模拟订阅消息服务

    /** 初始化测试依赖和当前登录用户 */
    @BeforeEach
    void setUp() {
        commentMapper = mock(MiniCheckinCommentMapper.class);
        recordsService = mock(MiniCheckinRecordsService.class);
        shareService = mock(MiniCheckinShareService.class);
        userService = mock(MiniUserService.class);
        wechatMiniAuthService = mock(WechatMiniAuthService.class);
        miniSubscribeMessageService = mock(MiniSubscribeMessageService.class);
        service = new MiniCheckinCommentServiceImpl(recordsService, shareService, userService,
                wechatMiniAuthService, miniSubscribeMessageService);
        ReflectionTestUtils.setField(service, "baseMapper", commentMapper);
        BaseContext.setCurrentId("user-1");
    }

    /** 清理测试线程中的用户信息 */
    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    /** 未设置昵称或头像的用户不能评论 */
    @Test
    void createCommentShouldRejectIncompleteProfile() {
        when(recordsService.getOne(any())).thenReturn(buildRecord(10, "user-1"));
        when(userService.getOne(any())).thenReturn(buildUser("user-1", null, "avatar.jpg"));
        MiniCheckinCommentCreateRequest request = buildCreateRequest(10, null, "你好");

        ParamException exception = assertThrows(ParamException.class, () -> service.createComment(request));

        assertEquals("请先设置头像和昵称后再评论", exception.getMessage());
        verify(wechatMiniAuthService, never()).checkText(any(), any());
    }

    /** 资料完整的记录主人可以发表顶级评论 */
    @Test
    void createCommentShouldSaveRootComment() {
        when(recordsService.getOne(any())).thenReturn(buildRecord(10, "user-1"));
        when(userService.getOne(any())).thenReturn(buildUser("user-1", "小明", "avatar.jpg"));
        when(commentMapper.insert(any(MiniCheckinComment.class))).thenAnswer(invocation -> {
            MiniCheckinComment comment = invocation.getArgument(0); // 实际插入的评论
            comment.setId(99);
            return 1;
        });
        MiniCheckinCommentCreateRequest request = buildCreateRequest(10, null, "  你好  ");

        MiniCheckinCommentResponse response = service.createComment(request);

        assertEquals(99, response.getId());
        assertEquals("你好", response.getContent());
        assertEquals(Integer.valueOf(0), response.getRootId());
        assertEquals(Integer.valueOf(0), response.getReplyToCommentId());
        assertTrue(response.getIsMine());
        assertTrue(response.getCanDelete());
        verify(wechatMiniAuthService).checkText("你好", "user-1");
        verifyNoInteractions(miniSubscribeMessageService);
    }

    /** 非主人发表顶级评论后通知记录主人 */
    @Test
    void createCommentShouldNotifyOwnerForNonOwnerRootComment() {
        BaseContext.setCurrentId("user-2");
        MiniCheckinRecords record = buildRecord(10, "owner");
        record.setName("西湖");
        when(recordsService.getOne(any())).thenReturn(record);
        when(shareService.getById(5)).thenReturn(buildShare(5, 10, "owner", true, true));
        when(userService.getOne(any())).thenReturn(buildUser("user-2", "小红", "avatar.jpg"));
        when(userService.list(any(Wrapper.class))).thenReturn(List.of(buildUser("user-2", "小红", "avatar.jpg")));
        when(commentMapper.insert(any(MiniCheckinComment.class))).thenAnswer(invocation -> {
            MiniCheckinComment comment = invocation.getArgument(0); // 实际插入的评论
            comment.setId(99);
            return 1;
        });
        MiniCheckinCommentCreateRequest request = buildCreateRequest(10, 5, "你好");

        service.createComment(request);

        verify(miniSubscribeMessageService).notifyNewComment(eq("owner"), eq("西湖"), eq("小红"),
                eq("你好"), any(LocalDateTime.class), eq(5), eq("dynamic"), eq(10));
        verify(miniSubscribeMessageService, never()).notifyReply(any(), any(), any(), any(), any(), any(), any(), any());
    }

    /** 回复其他用户后同时通知被回复人和记录主人 */
    @Test
    void createCommentShouldNotifyReplyTargetAndOwnerForReply() {
        BaseContext.setCurrentId("user-2");
        MiniCheckinRecords record = buildRecord(10, "owner");
        record.setName("西湖");
        when(recordsService.getOne(any())).thenReturn(record);
        when(shareService.getById(5)).thenReturn(buildShare(5, 10, "owner", true, true));
        when(userService.getOne(any())).thenReturn(buildUser("user-2", "小红", "avatar.jpg"));
        when(userService.list(any(Wrapper.class))).thenReturn(List.of(
                buildUser("user-2", "小红", "avatar.jpg"),
                buildUser("user-3", "小蓝", "avatar2.jpg")));
        when(commentMapper.selectById(50)).thenReturn(buildReply(50, 30, 10, "user-3"));
        when(commentMapper.selectById(30)).thenReturn(buildRoot(30, 10, "owner"));
        when(commentMapper.selectCount(any())).thenReturn(1L, 2L);
        when(commentMapper.selectOne(any())).thenReturn(null);
        when(commentMapper.insert(any(MiniCheckinComment.class))).thenAnswer(invocation -> {
            MiniCheckinComment comment = invocation.getArgument(0); // 实际插入的回复
            comment.setId(99);
            return 1;
        });
        MiniCheckinCommentCreateRequest request = buildCreateRequest(10, 5, "你好");
        request.setReplyToCommentId(50);

        service.createComment(request);

        verify(miniSubscribeMessageService).notifyReply(eq("user-3"), eq("西湖"), eq("小红"),
                eq("你好"), any(LocalDateTime.class), eq(5), eq("dynamic"), eq(10));
        verify(miniSubscribeMessageService).notifyNewComment(eq("owner"), eq("西湖"), eq("小红"),
                eq("你好"), any(LocalDateTime.class), eq(5), eq("dynamic"), eq(10));
    }

    /** 主人未回复前，非主人最多连续评论三条 */
    @Test
    void createCommentShouldRejectFourthNonOwnerCommentWithoutOwnerReply() {
        BaseContext.setCurrentId("user-2");
        when(recordsService.getOne(any())).thenReturn(buildRecord(10, "owner"));
        when(shareService.getById(5)).thenReturn(buildShare(5, 10, "owner", true, false));
        when(userService.getOne(any())).thenReturn(buildUser("user-2", "小红", "avatar.jpg"));
        when(commentMapper.selectById(50)).thenReturn(buildReply(50, 30, 10, "user-3"));
        when(commentMapper.selectById(30)).thenReturn(buildRoot(30, 10, "owner"));
        when(commentMapper.selectCount(any())).thenReturn(1L, 2L, 3L);
        when(commentMapper.selectOne(any())).thenReturn(null);
        MiniCheckinCommentCreateRequest request = buildCreateRequest(10, 5, "继续回复");
        request.setReplyToCommentId(50);

        ParamException exception = assertThrows(ParamException.class, () -> service.createComment(request));

        assertEquals("请等待主人回复后再继续评论", exception.getMessage());
        verify(wechatMiniAuthService, never()).checkText(any(), any());
    }

    /** 分享关闭评论时读取结果为空 */
    @Test
    void listCommentsShouldReturnEmptyWhenCommentsDisabled() {
        BaseContext.setCurrentId("user-2");
        when(recordsService.getOne(any())).thenReturn(buildRecord(10, "owner"));
        when(shareService.getById(5)).thenReturn(buildShare(5, 10, "owner", false, false));

        MiniCheckinCommentListResponse response = service.listComments(10, 1, 5);

        assertEquals(false, response.getCommentsEnabled());
        assertTrue(response.getList().isEmpty());
        assertEquals(0L, response.getTotal());
        assertEquals(false, response.getHasMore());
        verify(commentMapper, never()).selectPage(any(), any());
    }

    /** 评论作者删除自己的评论时只做软删除 */
    @Test
    void deleteCommentShouldSoftDeleteForAuthor() {
        BaseContext.setCurrentId("user-2");
        when(commentMapper.selectById(7)).thenReturn(buildReply(7, 30, 10, "user-2"));
        when(recordsService.getById(10)).thenReturn(buildRecord(10, "owner"));
        when(commentMapper.updateById(any(MiniCheckinComment.class))).thenReturn(1);

        assertTrue(service.deleteComment(7));

        ArgumentCaptor<MiniCheckinComment> updateCaptor = ArgumentCaptor.forClass(MiniCheckinComment.class); // 软删除更新实体
        verify(commentMapper).updateById(updateCaptor.capture());
        assertEquals(Integer.valueOf(7), updateCaptor.getValue().getId());
        assertEquals(Integer.valueOf(1), updateCaptor.getValue().getIsDeleted());
        verify(commentMapper, never()).delete(any(Wrapper.class));
    }

    /** 记录主人删除顶级评论时硬删除整条评论串 */
    @Test
    void deleteCommentShouldHardDeleteThreadForOwner() {
        when(commentMapper.selectById(7)).thenReturn(buildRoot(7, 10, "user-1"));
        when(recordsService.getById(10)).thenReturn(buildRecord(10, "user-1"));
        when(commentMapper.delete(any(Wrapper.class))).thenReturn(1);

        assertTrue(service.deleteComment(7));

        verify(commentMapper).delete(any(Wrapper.class));
        verify(commentMapper, never()).updateById(any(MiniCheckinComment.class));
    }

    /** 已软删除的评论对非主人不返回内容 */
    @Test
    void buildResponseShouldHideContentForDeletedComment() {
        MiniCheckinComment comment = buildRoot(7, 10, "user-2");
        comment.setIsDeleted(1);
        comment.setContent("违规内容");

        MiniCheckinCommentResponse response = (MiniCheckinCommentResponse) ReflectionTestUtils.invokeMethod(
                service, "buildResponse", comment, "user-1", "user-1", java.util.Collections.emptyMap());

        assertTrue(response.getIsDeleted());
        assertNull(response.getContent());
        assertTrue(response.getCanDelete());
    }

    /**
     * 构造评论创建请求
     *
     * @param recordId 记录ID
     * @param shareId  分享ID
     * @param content  评论内容
     * @return 评论创建请求
     */
    private MiniCheckinCommentCreateRequest buildCreateRequest(Integer recordId, Integer shareId, String content) {
        MiniCheckinCommentCreateRequest request = new MiniCheckinCommentCreateRequest();
        request.setRecordId(recordId);
        request.setShareId(shareId);
        request.setContent(content);
        return request;
    }

    /**
     * 构造打卡记录
     *
     * @param id          记录ID
     * @param userOpenid  记录主人openid
     * @return 打卡记录
     */
    private MiniCheckinRecords buildRecord(Integer id, String userOpenid) {
        MiniCheckinRecords record = new MiniCheckinRecords();
        record.setId(id);
        record.setUserOpenid(userOpenid);
        record.setIsDeleted(0);
        return record;
    }

    /**
     * 构造分享
     *
     * @param id              分享ID
     * @param recordId        记录ID
     * @param ownerOpenid     分享主人openid
     * @param commentsEnabled 是否允许评论
     * @param commentsPublic  是否公开全部评论
     * @return 分享记录
     */
    private MiniCheckinShare buildShare(Integer id, Integer recordId, String ownerOpenid,
                                        boolean commentsEnabled, boolean commentsPublic) {
        MiniCheckinShare share = new MiniCheckinShare();
        share.setId(id);
        share.setRecordId(recordId);
        share.setUserOpenid(ownerOpenid);
        share.setShareType("dynamic");
        share.setExpireTime(LocalDateTime.now().plusDays(1));
        share.setCommentsEnabled(commentsEnabled ? 1 : 0);
        share.setCommentsPublic(commentsPublic ? 1 : 0);
        return share;
    }

    /**
     * 构造用户
     *
     * @param openid    用户openid
     * @param nickname  昵称
     * @param avatarUrl 头像
     * @return 用户
     */
    private MiniUser buildUser(String openid, String nickname, String avatarUrl) {
        MiniUser user = new MiniUser();
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        return user;
    }

    /**
     * 构造顶级评论
     *
     * @param id         评论ID
     * @param recordId   记录ID
     * @param userOpenid 评论人openid
     * @return 顶级评论
     */
    private MiniCheckinComment buildRoot(Integer id, Integer recordId, String userOpenid) {
        MiniCheckinComment comment = new MiniCheckinComment();
        comment.setId(id);
        comment.setRecordId(recordId);
        comment.setUserOpenid(userOpenid);
        comment.setContent("顶级评论");
        comment.setRootId(0);
        comment.setReplyToCommentId(0);
        comment.setReplyToOpenid("");
        comment.setIsDeleted(0);
        comment.setCreateTime(LocalDateTime.now());
        return comment;
    }

    /**
     * 构造回复
     *
     * @param id         回复ID
     * @param rootId     顶级评论ID
     * @param recordId   记录ID
     * @param userOpenid 回复人openid
     * @return 回复
     */
    private MiniCheckinComment buildReply(Integer id, Integer rootId, Integer recordId, String userOpenid) {
        MiniCheckinComment comment = new MiniCheckinComment();
        comment.setId(id);
        comment.setRecordId(recordId);
        comment.setUserOpenid(userOpenid);
        comment.setContent("回复内容");
        comment.setRootId(rootId);
        comment.setReplyToCommentId(rootId);
        comment.setReplyToOpenid("owner");
        comment.setIsDeleted(0);
        comment.setCreateTime(LocalDateTime.now());
        return comment;
    }
}
