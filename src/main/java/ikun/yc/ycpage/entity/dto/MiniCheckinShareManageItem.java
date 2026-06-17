package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序打卡分享管理列表项
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
public class MiniCheckinShareManageItem {
    /** 分享ID */
    private Integer id;

    /** 原打卡记录ID */
    private Integer recordId;

    /** 地点名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    /** 有效截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 分享类型 dynamic-动态 static-静态 */
    private String shareType;

    /** 是否包含备注 */
    private Integer includeRemark;

    /** 是否包含图片 */
    private Integer includeImgs;

    /** 是否已过期或停用 */
    private Boolean isExpired;
}
