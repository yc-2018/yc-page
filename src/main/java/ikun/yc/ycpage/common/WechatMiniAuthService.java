package ikun.yc.ycpage.common;

import com.alibaba.fastjson.JSON;
import ikun.yc.ycpage.common.exception.LoginException;
import ikun.yc.ycpage.entity.dto.WechatAccessTokenDTO;
import ikun.yc.ycpage.entity.dto.MiniSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信认证服务
 *
 * @author ChenGuangLong
 * @since 2025/03/08 14:59:40
 */
@Component
@RequiredArgsConstructor
// 微信接口服务封装
public class WechatMiniAuthService {
  private static final String MINI_ACCESS_TOKEN_KEY = "mini:access_token"; // 小程序 access_token 缓存 key
  private static final int ACCESS_TOKEN_EXPIRE_BUFFER_SECONDS = 300;       // 提前过期缓冲秒数

  private final RestTemplate restTemplate;
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${mini.appid}")
  private String appid;
  @Value("${mini.secret}")
  private String secret;

  /**
   * 获取小程序接口调用凭证
   *
   * @return access_token
   */
  public String getAccessToken() {
    Object cachedToken = redisTemplate.opsForValue().get(MINI_ACCESS_TOKEN_KEY); // 已缓存的微信令牌
    if (cachedToken instanceof String && !((String) cachedToken).isEmpty()) {
      return (String) cachedToken;
    }
    if (cachedToken != null && !String.valueOf(cachedToken).isEmpty()) {
      return String.valueOf(cachedToken);
    }

    String url = "https://api.weixin.qq.com/cgi-bin/token" +
        "?grant_type=client_credential&appid={appid}&secret={secret}";
    Map<String, String> params = new HashMap<>();
    params.put("appid", appid);
    params.put("secret", secret);

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, params);
    WechatAccessTokenDTO tokenDTO = JSON.parseObject(response.getBody(), WechatAccessTokenDTO.class);
    if (tokenDTO == null || tokenDTO.getErrcode() != null) {
      throw new LoginException("获取微信access_token失败: " + (tokenDTO == null ? "空响应" : tokenDTO.getErrmsg()));
    }

    int expireSeconds = Math.max(60, tokenDTO.getExpiresIn() - ACCESS_TOKEN_EXPIRE_BUFFER_SECONDS); // 实际缓存秒数
    redisTemplate.opsForValue().set(MINI_ACCESS_TOKEN_KEY, tokenDTO.getAccessToken(), expireSeconds, TimeUnit.SECONDS);
    return tokenDTO.getAccessToken();
  }

  /** 清理小程序接口调用凭证缓存 */
  public void clearAccessToken() {
    redisTemplate.delete(MINI_ACCESS_TOKEN_KEY);
  }

  /**
   * 通过微信登录 code 获取小程序用户会话信息
   *
   * @param code 微信登录 code
   * @return 小程序会话信息
   */
  public MiniSessionDTO getSessionInfo(String code) {
    String url = "https://api.weixin.qq.com/sns/jscode2session" +
        "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    Map<String, String> params = new HashMap<>();
    params.put("appid", appid);
    params.put("secret", secret);
    params.put("code", code);

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, params);
    MiniSessionDTO session = JSON.parseObject(response.getBody(), MiniSessionDTO.class);

    if (session.getErrcode() != null) {
      throw new LoginException("微信登录失败: " + session.getErrmsg());
    }
    return session;

  }
}
