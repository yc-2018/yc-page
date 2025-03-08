package ikun.yc.ycpage.entity.dto;

import lombok.Data;

/**
 * 小程序登录返回参数
 *
 * @author ChenGuangLong
 * @since 2025/03/08 15:51:21
 */
@Data
public class MiniSessionDTO {
    private String openid;
    private String session_key;
    private String unionid;  // 如果有unionid需求
    private Integer errcode;
    private String errmsg;
}