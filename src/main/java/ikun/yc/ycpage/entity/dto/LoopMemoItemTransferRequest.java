package ikun.yc.ycpage.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 循环备忘记录转移请求
 *
 * @author Codex
 * @since 2026/07/08
 */
@Data
@NoArgsConstructor
@ApiModel("循环备忘记录转移请求")
public class LoopMemoItemTransferRequest {

    @ApiModelProperty("源循环备忘主键")
    private Integer sourceMemoId; // 源循环备忘主键

    @ApiModelProperty("目标循环备忘主键")
    private Integer targetMemoId; // 目标循环备忘主键

    @ApiModelProperty("源循环备忘版本号")
    private Integer sourceMemoVersion; // 源循环备忘版本号

    @ApiModelProperty("目标循环备忘版本号")
    private Integer targetMemoVersion; // 目标循环备忘版本号

    @ApiModelProperty("要转移的循环记录主键列表")
    private List<Integer> loopItemIds; // 要转移的循环记录主键列表
}
