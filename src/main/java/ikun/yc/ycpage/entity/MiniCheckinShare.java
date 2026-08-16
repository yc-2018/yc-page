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
 * 小程序打卡分享表实体
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
@TableName("mini_checkin_share")
public class MiniCheckinShare {
    /** 分享ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 创建人openid */
    @JsonIgnore
    private String userOpenid;

    /** 原打卡记录ID */
    private Integer recordId;

    /** 有效截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 分享类型 dynamic-动态 static-静态 */
    private String shareType;

    /** 是否包含备注 0-否 1-是 */
    private Integer includeRemark;

    /** 是否包含图片 0-否 1-是 */
    private Integer includeImgs;

    /** 动态分享存记录ID，静态分享存记录JSON */
    private String content;

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
