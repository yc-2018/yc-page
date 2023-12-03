package ikun.yc.ycpage.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * (users)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 * @description 由 Mybatisplus Code Generator 创建
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("users")
public class Users extends Model<Users> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
	private String id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 电话号码
     */
    private String phoneNumber;
    /**
     * 密码
     */
    private String password;
    /**
     * 创建时间
     */
    private Date creationTime;
    /**
     * 修改时间
     */
    private Date modificationTime;
    /**
     * 是否已删除
     */
    private Integer isDeleted;

}