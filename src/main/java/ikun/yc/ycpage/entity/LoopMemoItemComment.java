package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 循环备忘记录评论
 *
 * @author codex
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("loop_memo_item_comment")
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "LoopMemoItemComment对象", description = "循环备忘记录的第三层评论。")
public class LoopMemoItemComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "备忘主键")
    private Integer memoId;

    @ApiModelProperty(value = "循环记录主键")
    private Integer loopItemId;

    @ApiModelProperty(value = "评论时间")
    private LocalDateTime commentDate;

    @ApiModelProperty(value = "评论文本")
    private String commentText;

    @ApiModelProperty(value = "评论图片用,分割")
    private String imgArr;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @ApiModelProperty(value = "当前循环记录评论总数")
    private Long commentTotal;
}
