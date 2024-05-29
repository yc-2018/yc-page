package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * <p>
 * 目前作为:循环代办的历史时间。
 * </p>
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

    /** 循环代办时间id */
    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 待办事项id */
    @ApiModelProperty(value = "备忘组件")
    @TableField("to_do_item_id")
    private Integer toDoItemId;

    /** 备忘录日期  数据库有创建时赋当前为默认值 */
    @ApiModelProperty(value = "循环时间")
    @TableField(value = "memo_date")
    private LocalDateTime memoDate;

    public LoopMemoTime(Memo item) {
        this.toDoItemId = item.getId();
        this.memoDate = item.getUpdateTime();
    }
}
