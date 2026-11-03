package ikun.yc.ycpage.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 小程序打卡评论回复分页响应
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentReplyListResponse {
    /** 当前页回复，按时间正序 */
    private List<MiniCheckinCommentResponse> list;

    /** 当前页码 */
    private Long page;

    /** 每页数量 */
    private Long size;

    /** 是否还有下一页 */
    private Boolean hasMore;

    /** 回复总数 */
    private Long total;

    /** 是否允许评论 */
    private Boolean commentsEnabled;

    /** 是否公开全部评论 */
    private Boolean commentsPublic;

    /** 当前访问者是否记录主人 */
    private Boolean isOwner;
}
