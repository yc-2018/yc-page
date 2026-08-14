package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 小程序打卡分享创建请求
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
public class MiniCheckinShareCreateRequest {
    /** 原打卡记录ID */
    @NotNull(message = "打卡记录不能为空")
    private Integer recordId;

    /** 有效期类型 */
    @NotBlank(message = "分享时长不能为空")
    private String durationType;

    /** 是否包含备注，默认包含 */
    private Boolean includeRemark;

    /** 是否包含图片，默认包含 */
    private Boolean includeImgs;

    /** 分享类型 dynamic-动态 static-静态，默认动态 */
    private String shareType;

    /** 静态分享备注覆盖值，允许空字符串；未传时使用原记录备注 */
    private String staticRemark;

    /** 静态分享图片覆盖值，多个地址用逗号拼接；允许空字符串；未传时使用原记录图片 */
    private String staticImgs;

    /** 创建人openid，由登录态注入 */
    @JsonIgnore
    private String userOpenid;
}
