package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 循环备忘记录
 *
 * @author chengguanglong
 * @since 2023-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("loop_memo_time")
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value="LoopMemoTime对象", description="目前作为:循环代办的历史时间。")
public class LoopMemoTime implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "备忘主键")
    private Integer memoId;

    @ApiModelProperty(value = "循环时间")
    @TableField(value = "memo_date")
    private LocalDateTime memoDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)    // 仅返回给前端，不接收前端传入的数据
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)    // 仅返回给前端，不接收前端传入的数据
    @TableField(fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "循环时可备注文本")
    @TableField(value = "loop_text")
    private String loopText;

    public LoopMemoTime(Memo item) {
        this.memoId = item.getId();
        this.memoDate = item.getUpdateTime();
        this.loopText = item.getOkText();
    }
}
