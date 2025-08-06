package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * (users)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("users")
@ApiModel("Users对象")
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)              // 如果为空字符串或 null，这个字段不会返回给前端
public class User extends Model<User> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
	private String id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 电话号码 */
    private String phoneNumber;

    /** 密码 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 创建时间 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(update = "now()")
    private LocalDateTime updateTime;

//    /** 是否已删除 */
//    private Integer isDeleted;

    /** 头像 */
    private String avatar;

}