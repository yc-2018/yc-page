package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentBatchResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentQueryRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentReplyListResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinCommentResponse;
import ikun.yc.ycpage.service.MiniCheckinCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序打卡评论控制器
 *
 * @author cgl
 * @since 2026/11/03
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/checkinComment")
public class MiniCheckinCommentController {
    private final MiniCheckinCommentService miniCheckinCommentService;

    /**
     * 发表评论或回复
     *
     * @param request 评论创建请求
     * @return 新评论
     */
    @PostMapping
    @UserId(fieldName = "userOpenid")
    @CountControl(operationType = CountControlAspect.ADD, frequency = 5)
    public R<MiniCheckinCommentResponse> createComment(@RequestBody @Valid MiniCheckinCommentCreateRequest request) {
        return R.success(miniCheckinCommentService.createComment(request));
    }

    /**
     * 分页查询顶级评论
     *
     * @param recordId 打卡记录ID
     * @param page     第几页
     * @param request  查询请求，可传分享ID
     * @return 顶级评论分页
     */
    @PostMapping("/list/{recordId}/{page}")
    public R<MiniCheckinCommentListResponse> listComments(@PathVariable Integer recordId,
                                                          @PathVariable Integer page,
                                                          @RequestBody(required = false) MiniCheckinCommentQueryRequest request) {
        return R.success(miniCheckinCommentService.listComments(
                recordId, page, request == null ? null : request.getShareId()));
    }

    /**
     * 分页查询回复
     *
     * @param rootId  顶级评论ID
     * @param page    第几页
     * @param request 查询请求，可传分享ID
     * @return 回复分页
     */
    @PostMapping("/replies/{rootId}/{page}")
    public R<MiniCheckinCommentReplyListResponse> listReplies(@PathVariable Integer rootId,
                                                              @PathVariable Integer page,
                                                              @RequestBody(required = false) MiniCheckinCommentQueryRequest request) {
        return R.success(miniCheckinCommentService.listReplies(
                rootId, page, request == null ? null : request.getShareId()));
    }

    /**
     * 批量查询打卡记录的评论预览
     *
     * @param request 批量预览请求
     * @return 批量评论预览
     */
    @PostMapping("/batch")
    public R<MiniCheckinCommentBatchResponse> batchPreview(@RequestBody @Valid MiniCheckinCommentBatchRequest request) {
        return R.success(miniCheckinCommentService.batchPreview(request));
    }

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @return 是否删除成功
     */
    @PostMapping("/delete/{commentId}")
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 10)
    public R<Boolean> deleteComment(@PathVariable Integer commentId) {
        return R.success(miniCheckinCommentService.deleteComment(commentId));
    }
}
