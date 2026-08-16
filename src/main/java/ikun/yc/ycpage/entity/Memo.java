package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


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
@TableName("memo")
public class Memo extends Model<Memo> implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 备忘录事项ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 用户ID */
    @JsonIgnore
    @TableField(select = false)
    private String userId;
    /** 备忘录待办类型 (0:普通待办，1：循环待办，2：长期待办，3：紧急待办，4：备忘英语，5、日记待办，6、公事待办 */
    private Integer itemType;
    /** 内容 */
    private String content;
    /** 是否已完成 （1：完成，0未完成） */
    private Integer completed;
    /** 重复次数(循环代办专属) */
    private Integer numberOfRecurrences;
    /** 完成时记录备注文本 */
    private String okText;
    /** 备忘图片地址，多个地址用逗号分隔 */
    private String imgArr;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime okTime;
    /** 备忘标签列表 */
    @TableField(exist = false)
    private List<MemoTag> tags;
    /** 备忘标签ID列表 */
    @TableField(exist = false)
    private List<Integer> tagIds;

    public Memo(String userId, String content, Integer itemType) {
        this.userId = userId;
        this.content = content;
        this.itemType = itemType;
    }

    public Memo(String userId, Integer itemType) {
        this.userId = userId;
        this.itemType = itemType;
    }

    /** 校验备忘图片字段长度 */
    public void validateImgArr() {
        if (imgArr != null && imgArr.length() > 999) {
            throw new ParamException("备忘图片地址总长度不能超过999个字符");
        }
    }

    /** 修改待办信息 防止恶意修改 */
    public void toReviseInfo() {
        userId = BaseContext.getCurrentId();
        // 更新时间 不为空的情况下允许在36+1天内，不在60+1天内不允许 设置为空
        if (okTime != null && (okTime.isBefore(LocalDateTime.now().minusDays(61)) || okTime.isAfter(LocalDateTime.now().plusDays(1)))) {
            okTime = null;
        }
    }


}
