package ikun.yc.ycpage.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 小程序打卡评论批量预览请求
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentBatchRequest {
    /** 需要加载评论预览的打卡记录ID */
    @NotEmpty(message = "打卡记录不能为空")
    @Size(max = 100, message = "最多查询100条打卡记录")
    private List<Integer> recordIds;

    /** 分享ID，从打卡记录列表进入时为空 */
    private Integer shareId;

    /** 每条记录预览的顶级评论数量，默认3条 */
    @Min(value = 1, message = "预览评论数量最少1条")
    @Max(value = 5, message = "预览评论数量最多5条")
    private Integer previewSize;
}
