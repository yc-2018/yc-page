package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import ikun.yc.ycpage.common.BaseContext;
import io.swagger.annotations.ApiModel;
import lombok.Data;
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
public class Users implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @TableId
	private String id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 电话号码 */
    private String phoneNumber;

    /** 密码 */
    private String password;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(update = "now()")
    private LocalDateTime updateTime;

    /** 是否已删除 */
    private Integer isDeleted;

    /** 头像 */
    private String avatar;

    public Users getNameAndAvatar() {
        return new Users()
                .setId(BaseContext.getCurrentId())
                .setUsername(username)
                .setAvatar(avatar);
    }
}