package ikun.yc.ycpage.common;

import com.alibaba.fastjson.JSON;
import ikun.yc.ycpage.common.exception.LoginException;
import ikun.yc.ycpage.entity.dto.MiniSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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
  private final RestTemplate restTemplate;
  @Value("${mini.appid}")
  private String appid;
  @Value("${mini.secret}")
  private String secret;

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