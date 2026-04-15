package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/** 账号备忘表 */
@TableName(value = "mini_account_memo")
@Data
public class MiniAccountMemo {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 关联用户openid */
    @JsonIgnore
    private String userOpenid;
    /** 网站logo图片URL */
    private String websiteLogo;
    /** 网站名称 */
    private String websiteName;
    /** 网站地址 */
    private String websiteUrl;
    /** 登录账号 */
    private String account;
    /** 登录密码 */
    private String password;
    /** 备注信息 */
    private String remark;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
    /** 是否删除: 0-否, 1-是 */
    @JsonIgnore
    private Integer isDeleted;
}