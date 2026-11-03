package ikun.yc.ycpage.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 小程序打卡评论批量预览响应
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentBatchResponse {
    /** 按请求顺序返回的记录评论预览 */
    private List<MiniCheckinCommentBatchItem> records;

    /** 是否允许评论 */
    private Boolean commentsEnabled;

    /** 是否公开全部评论 */
    private Boolean commentsPublic;

    /** 当前访问者是否记录主人 */
    private Boolean isOwner;
}
