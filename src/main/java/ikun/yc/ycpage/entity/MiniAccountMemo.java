package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** 图片url,拼接 */
    private String imgs;
    /** 创建时间 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /** 更新时间 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    /** 是否删除: 0-否, 1-是 */
    @JsonIgnore
    private Integer isDeleted;
}