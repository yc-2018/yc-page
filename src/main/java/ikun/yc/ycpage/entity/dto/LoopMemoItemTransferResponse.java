package ikun.yc.ycpage.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 循环备忘记录转移响应
 *
 * @author Codex
 * @since 2026/07/08
 */
@Data
@NoArgsConstructor
@ApiModel("循环备忘记录转移响应")
public class LoopMemoItemTransferResponse {

    @ApiModelProperty("源循环备忘主键")
    private Integer sourceMemoId; // 源循环备忘主键

    @ApiModelProperty("目标循环备忘主键")
    private Integer targetMemoId; // 目标循环备忘主键

    @ApiModelProperty("已转移循环记录数量")
    private Integer movedCount; // 已转移循环记录数量

    @ApiModelProperty("源循环备忘最新循环次数")
    private Integer sourceNumberOfRecurrences; // 源循环备忘最新循环次数

    @ApiModelProperty("目标循环备忘最新循环次数")
    private Integer targetNumberOfRecurrences; // 目标循环备忘最新循环次数

    @ApiModelProperty("源循环备忘新版本号")
    private Integer sourceMemoVersion; // 源循环备忘新版本号

    @ApiModelProperty("目标循环备忘新版本号")
    private Integer targetMemoVersion; // 目标循环备忘新版本号
}
