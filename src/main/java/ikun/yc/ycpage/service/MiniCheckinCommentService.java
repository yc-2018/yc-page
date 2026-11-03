package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.MiniCheckinComment;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentReplyListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentResponse;

/**
 * 小程序打卡评论 Service
 *
 * @author cgl
 * @since 2026/11/03
 */
public interface MiniCheckinCommentService extends IService<MiniCheckinComment> {
    /**
     * 发表评论或回复
     *
     * @param request 评论创建请求
     * @return 新评论
     */
    MiniCheckinCommentResponse createComment(MiniCheckinCommentCreateRequest request);

    /**
     * 分页查询顶级评论
     *
     * @param recordId 打卡记录ID
     * @param page     第几页
     * @param shareId  分享ID，从打卡记录列表进入时为空
     * @return 顶级评论分页
     */
    MiniCheckinCommentListResponse listComments(Integer recordId, Integer page, Integer shareId);

    /**
     * 分页查询回复，按时间正序
     *
     * @param rootId  顶级评论ID
     * @param page    第几页
     * @param shareId 分享ID，从打卡记录列表进入时为空
     * @return 回复分页
     */
    MiniCheckinCommentReplyListResponse listReplies(Integer rootId, Integer page, Integer shareId);

    /**
     * 批量查询打卡记录的评论预览
     *
     * @param request 批量预览请求
     * @return 批量评论预览
     */
    MiniCheckinCommentBatchResponse batchPreview(MiniCheckinCommentBatchRequest request);

    /**
     * 删除评论，记录主人硬删除，评论作者软删除
     *
     * @param commentId 评论ID
     * @return 是否删除成功
     */
    Boolean deleteComment(Integer commentId);
}
