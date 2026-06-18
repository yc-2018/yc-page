package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小程序打卡分享详情响应
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
public class MiniCheckinShareDetailResponse {
    /** 分享ID */
    private Integer id;

    /** 原打卡记录ID */
    private Integer recordId;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 地点名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 图片url用逗号拼接 */
    private String imgs;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    /** 有效截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 分享类型 dynamic-动态 static-静态 */
    private String shareType;

    /** 当前访问者是否分享创建人 */
    private Boolean isOwner;

    /** 分享是否已过期 */
    private Boolean isExpired;
}
