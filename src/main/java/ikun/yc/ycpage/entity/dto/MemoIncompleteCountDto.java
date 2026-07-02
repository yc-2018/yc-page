package ikun.yc.ycpage.entity.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待办未完成预加载统计
 *
 * @author cgl
 * @since 2026/07/02
 */
@Data
@NoArgsConstructor
@ApiModel("待办未完成预加载统计")
public class MemoIncompleteCountDto {

    @ApiModelProperty("待办类型")
    private Integer itemType; // 待办类型

    @ApiModelProperty("未完成数量")
    private Long count; // 未完成数量
}
