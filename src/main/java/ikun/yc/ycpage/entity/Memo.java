package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ikun.yc.ycpage.common.BaseContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 备忘录 实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("to_do_items")
public class Memo extends Model<Memo> implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 备忘录事项ID */
    @TableId(type = IdType.AUTO)
	  private Integer id;

    /** 用户ID */
    @JsonIgnore
    @TableField(select = false)
    private String userId;
    /** 备忘录待办类型 (0:普通待办，1：循环待办，2：长期待办，3：紧急待办，4：备忘英语，5、日记待办，6、公事待办*/
    private Integer itemType;
    /** 内容 */
    private String content;
    /** 是否已完成 （1：完成，0未完成） */
    private Integer completed;
    /** 重复次数(循环代办专属) */
    private Integer numberOfRecurrences;
    /** 完成时记录备注文本 */
    private String okText;
    /** 创建时间 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 修改时间 */
    @TableField(fill = FieldFill.UPDATE)//, update = "now()")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	  private LocalDateTime updateTime;

    public Memo(String userId, String content, Integer itemType) {
        this.userId = userId;
        this.content = content;
        this.itemType = itemType;
    }

    public Memo(String userId, Integer itemType) {
        this.userId = userId;
        this.itemType = itemType;
    }

    /**
     * 修改待办信息 防止恶意修改
     */
    public void toReviseInfo(){
        userId = BaseContext.getCurrentId();
        // 更新时间 不为空的情况下允许在30+1天内，不在30+1天内不允许 设置为空
        if (updateTime != null && (updateTime.isBefore(LocalDateTime.now().minusDays(31))|| updateTime.isAfter(LocalDateTime.now().plusDays(1))) ) {
            updateTime = null;
        }
    }


}