package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 微信小程序用户表
 * TableName mini_users
 */
@TableName(value ="mini_user")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class MiniUser extends Model<MiniUser> implements Serializable {
    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 微信开放ID（唯一标识） */
    private String openid;
    /** 微信昵称 */
    private String nickname;
    /** 微信头像URL */
    private String avatarUrl;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    /** 登录次数统计 */
    private Integer loginCount;
    /** 用户状态 0-禁用 1-正常 */
    private Integer status;
    /** 是否订阅消息 0-未订阅 1-已订阅 */
    private Integer isSubscribe;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
