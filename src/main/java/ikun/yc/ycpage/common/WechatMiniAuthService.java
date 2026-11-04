package ikun.yc.ycpage.common;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.exception.LoginException;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.dto.WechatAccessTokenDTO;
import ikun.yc.ycpage.entity.dto.MiniSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
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
  private final ObjectMapper objectMapper;

  @Value("${mini.appid}")
  private String appid;
  @Value("${mini.secret}")
  private String secret;
  @Value("${mini.subscribe.state:formal}")
  private String subscribeState; // 订阅消息打开的小程序版本

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

  /**
   * 校验用户文本是否包含违规内容
   *
   * @param content 待校验文本
   * @param openid  发表文本的用户openid
   */
  public void checkText(String content, String openid) {
    if (content == null || content.isBlank()) {
      throw new ParamException("评论内容不能为空");
    }
    String accessToken = getAccessToken(); // 当前微信接口调用凭证
    JsonNode result = requestTextCheck(content, openid, accessToken); // 微信内容安全校验结果
    Integer errcode = getErrcode(result); // 微信接口错误码
    if (errcode != null && (errcode == 40001 || errcode == 42001)) {
      clearAccessToken();
      accessToken = getAccessToken();
      result = requestTextCheck(content, openid, accessToken);
      errcode = getErrcode(result);
    }
    if (errcode == null || errcode != 0) {
      throw new ParamException("内容安全校验失败，请稍后重试");
    }
    JsonNode checkResult = result.get("result"); // 建议结果对象
    String suggest = checkResult == null ? null : checkResult.path("suggest").asText(null); // pass-通过 review-需人工复核 risky-违规
    if (!"pass".equals(suggest)) {
      throw new ParamException("评论内容可能包含敏感信息，请修改后重试");
    }
  }

  /**
   * 发送小程序一次性订阅消息
   *
   * @param openid     接收人openid
   * @param templateId 订阅消息模板ID
   * @param page       点击消息打开的小程序页面，可为空
   * @param data       模板字段值
   * @return 微信错误码，0表示成功；null表示请求异常
   */
  public Integer sendSubscribeMessage(String openid, String templateId, String page, Map<String, String> data) {
    String accessToken = getAccessToken(); // 当前微信接口调用凭证
    JsonNode result = requestSubscribeSend(accessToken, openid, templateId, page, data); // 首次发送结果
    Integer errcode = getErrcode(result); // 微信接口错误码
    if (errcode != null && (errcode == 40001 || errcode == 42001)) {
      clearAccessToken();
      accessToken = getAccessToken();
      result = requestSubscribeSend(accessToken, openid, templateId, page, data);
      errcode = getErrcode(result);
    }
    return errcode;
  }

  /**
   * 调用微信一次性订阅消息发送接口
   *
   * @param accessToken 微信接口调用凭证
   * @param openid      接收人openid
   * @param templateId  订阅消息模板ID
   * @param page        点击消息打开的页面
   * @param data        模板字段值
   * @return 微信返回的JSON结果，请求异常时为null
   */
  private JsonNode requestSubscribeSend(String accessToken, String openid, String templateId,
                                        String page, Map<String, String> data) {
    String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={accessToken}"; // 订阅消息发送地址
    Map<String, Object> body = new HashMap<>(); // 订阅消息请求体
    body.put("touser", openid);
    body.put("template_id", templateId);
    if (StringUtils.hasText(page)) {
      body.put("page", page);
    }
    if (StringUtils.hasText(subscribeState)) {
      body.put("miniprogram_state", subscribeState);
    }
    body.put("lang", "zh_CN");
    Map<String, Object> dataBody = new HashMap<>(); // 模板字段对象
    for (Map.Entry<String, String> entry : data.entrySet()) {
      String value = entry.getValue() == null ? "" : entry.getValue(); // 模板字段值不能为空
      dataBody.put(entry.getKey(), Map.of("value", value));
    }
    body.put("data", dataBody);
    try {
      ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class, accessToken);
      if (response == null || !StringUtils.hasText(response.getBody())) {
        return null;
      }
      JsonNode result = objectMapper.readTree(response.getBody()); // 微信发送响应
      return result == null || result.isNull() ? null : result;
    } catch (JsonProcessingException | RestClientException ex) {
      return null;
    }
  }

  /**
   * 读取微信响应中的错误码
   *
   * @param result 微信响应JSON
   * @return 错误码，未返回时为null
   */
  private Integer getErrcode(JsonNode result) {
    if (result == null || result.isNull()) {
      return null;
    }
    JsonNode errcodeNode = result.get("errcode"); // 错误码节点
    return errcodeNode == null || errcodeNode.isNull() ? null : errcodeNode.asInt();
  }

  /**
   * 调用微信文本内容安全接口
   *
   * @param content     待校验文本
   * @param openid      用户openid
   * @param accessToken 微信接口调用凭证
   * @return 微信返回的JSON结果
   */
  private JsonNode requestTextCheck(String content, String openid, String accessToken) {
    String url = "https://api.weixin.qq.com/wxa/msg_sec_check?access_token={accessToken}"; // 内容安全校验地址
    Map<String, Object> body = new HashMap<>(); // 校验请求体
    body.put("content", content);
    body.put("version", 2);
    body.put("scene", 2);
    body.put("openid", openid);
    try {
      ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class, accessToken);
      JsonNode result = objectMapper.readTree(response.getBody()); // 微信校验响应
      if (result == null || result.isNull()) {
        throw new ParamException("内容安全校验失败，请稍后重试");
      }
      return result;
    } catch (JsonProcessingException | RestClientException ex) {
      throw new ParamException("内容安全校验失败，请稍后重试");
    }
  }
}
