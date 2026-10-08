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
 * 小程序2FA验证码表实体
 *
 * @author cgl
 * @since 2026/10/06
 */
@Data
@TableName("mini_totp")
public class MiniTotp {
    /** 2FA条目ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属用户openid */
    @JsonIgnore
    private String userOpenid;

    /** 名称，导出链接时作为label前缀和issuer */
    private String name;

    /** 账号标签，导出链接时放在label冒号之后 */
    private String account;

    /** Base32密钥 */
    private String secret;

    /** 置顶时间戳(毫秒)，0表示未置顶，越大越靠前；只能通过置顶接口修改 */
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
