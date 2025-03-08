package ikun.yc.ycpage.entity.dto;

import lombok.Data;

/**
 * 微信小程序获取用户信息dto
 * 小程序前端通过wx.getUserProfile获取
 *
 * @author ChenGuangLong
 * @since 2025/03/08 16:01:06
 */
@Data
public class WechatInfoDTO {
    private String encryptedData;       // 包括敏感数据在内的完整用户信息的加密数据，详见 用户数据的签名验证和加解密
    private String iv;                  // 加密算法的初始向量，详见 用户数据的签名验证和加解密
    private String rawData;             // 不包括敏感信息的原始数据字符串，用于计算签名
    private String signature;           // 使用 sha1( rawData + sessionkey ) 得到字符串，用于校验用户信息，详见 用户数据的签名验证和加解密
}