package ikun.yc.ycpage.entity.dto;

import lombok.Data;

/**
 * 小程序打卡评论查询请求
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentQueryRequest {
    /** 分享ID，从打卡记录列表进入时为空 */
    private Integer shareId;
}
