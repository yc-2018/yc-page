package ikun.yc.ycpage.entity.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 微信 access_token 返回参数
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
@Data
public class WechatAccessTokenDTO {
    @JSONField(name = "access_token")
    private String accessToken; // 接口调用凭证

    @JSONField(name = "expires_in")
    private Integer expiresIn;  // 凭证有效期，单位秒

    private Integer errcode;    // 微信错误码

    private String errmsg;      // 微信错误信息
}
