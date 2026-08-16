package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
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
public class LoopMemoItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 所属备忘录乐观锁版本号，仅用于请求传输 */
    @TableField(exist = false)
    private Integer memoVersion;

    private Integer memoId;

    @TableField(value = "memo_date")
    private LocalDateTime memoDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)    // 仅返回给前端，不接收前端传入的数据
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)    // 仅返回给前端，不接收前端传入的数据
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "loop_text")
    private String loopText;

    private String imgArr;

    @TableField(exist = false)
    private List<LoopMemoItemComment> comments;

    @TableField(exist = false)
    private Long commentTotal;

    @TableField(exist = false)
    private Boolean commentHasMore;
}
