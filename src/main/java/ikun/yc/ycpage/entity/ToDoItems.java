package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * (to_do_items)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("to_do_items")
public class ToDoItems{


    /** 待办事项ID */
    @TableId(type = IdType.AUTO)
	private Integer id;

    /** 用户ID */
    private String userId;

    /** 待办类型 (0:普通待办，1：循环待办，2：长期待办，3：紧急待办，4：备忘英语，5、日记待办，6、公事待办*/
    private Integer itemType;

    /** 内容 */
    private String content;

    /** 是否已完成 （1：完成，0未完成） */
    private Integer completed;
    /** 重复次数(循环代办专属) */
    private Integer numberOfRecurrences;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(update = "now()")
	private LocalDateTime updateTime;

    public ToDoItems(String userId, String content, Integer itemType) {
        this.userId = userId;
        this.content = content;
        this.itemType = itemType;
    }

}