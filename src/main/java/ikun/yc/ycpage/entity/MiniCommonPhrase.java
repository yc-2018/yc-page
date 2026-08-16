package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序打卡常用语表实体
 *
 * @author cgl
 * @since 2026/08/16
 */
@Data
@TableName("mini_common_phrase")
public class MiniCommonPhrase {
    /** 常用语ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属用户openid */
    @JsonIgnore
    private String userOpenid;

    /** 常用语内容 */
    private String content;

    /** 置顶排序值，越大越靠前 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
