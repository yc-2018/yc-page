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
import java.util.List;

/**
 * 循环备忘记录
 *
 * @author chengguanglong
 * @since 2023-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("loop_memo_item")
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value="LoopMemoTime对象", description="目前作为:循环代办的历史时间。")
public class LoopMemoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 所属备忘录乐观锁版本号，仅用于请求传输 */
    @TableField(exist = false)
    private Integer memoVersion;

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

    @ApiModelProperty(value = "循环时可备注图片用,分割")
    private String imgArr;

    @TableField(exist = false)
    @ApiModelProperty(value = "前5条循环记录评论")
    private List<LoopMemoItemComment> comments;

    @TableField(exist = false)
    @ApiModelProperty(value = "循环记录评论总数")
    private Long commentTotal;

    @TableField(exist = false)
    @ApiModelProperty(value = "循环记录是否还有更多评论")
    private Boolean commentHasMore;
}
