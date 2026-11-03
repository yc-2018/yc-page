package ikun.yc.ycpage.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 单条打卡记录的评论预览
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentBatchItem {
    /** 打卡记录ID */
    private Integer recordId;

    /** 可见顶级评论总数 */
    private Long total;

    /** 预览评论列表 */
    private List<MiniCheckinCommentResponse> list;
}
