package ikun.yc.ycpage.service.impl;

import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.dto.MiniWifiCodeRequest;
import ikun.yc.ycpage.entity.dto.MiniWifiCodeResponse;
import ikun.yc.ycpage.service.MiniWifiCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * 小程序 WiFi 码服务实现
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
@Service
@RequiredArgsConstructor
public class MiniWifiCodeServiceImpl implements MiniWifiCodeService {
    private static final int MAX_PAGE_PATH_LENGTH = 1024; // 微信 getQRCode path 最大长度
    private static final int QR_CODE_WIDTH = 430;         // 小程序码图片宽度
    private static final int MAX_DAILY_GENERATE_COUNT = 10; // 每日最多生成次数
    private static final String CONNECT_PAGE = "pages/wifiConnect/wifiConnect"; // WiFi 连接页路径
    private static final String DEFAULT_ENV_VERSION = "release";                // 默认打开正式版
    private static final String DAILY_COUNT_KEY_PREFIX = "wifiCode:daily:";      // WiFi 码每日计数 key 前缀

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WechatMiniAuthService wechatMiniAuthService;

    /**
     * 生成打开 WiFi 连接页的小程序码
     *
     * @param request WiFi 码请求参数
     * @return 小程序码图片信息
     */
    @Override
    public MiniWifiCodeResponse createWifiCode(MiniWifiCodeRequest request) {
        String pagePath = buildPagePath(request); // 小程序码携带的目标路径
        String envVersion = normalizeEnvVersion(request.getEnvVersion()); // 小程序目标环境
        String dailyCountKey = buildDailyCountKey(); // 当前用户当天计数 key
        increaseDailyCount(dailyCountKey);
        try {
            byte[] imageBytes = requestQrCode(pagePath, envVersion, true); // 微信返回的图片字节
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes); // 前端写临时文件用的 base64
            return new MiniWifiCodeResponse(imageBase64, MediaType.IMAGE_JPEG_VALUE, pagePath);
        } catch (RuntimeException exception) {
            decreaseDailyCount(dailyCountKey);
            throw exception;
        }
    }

    /**
     * 增加当天生成次数并限制上限
     *
     * @param key Redis 计数 key
     */
    private void increaseDailyCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key); // 当天生成次数
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(getSecondsUntilTomorrow()));
        }
        if (count != null && count > MAX_DAILY_GENERATE_COUNT) {
            throw new ParamException("今日最多生成10次WiFi码");
        }
    }

    /**
     * 生成失败时回退当天生成次数
     *
     * @param key Redis 计数 key
     */
    private void decreaseDailyCount(String key) {
        redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 构建当前用户当天 WiFi 码生成次数 key
     *
     * @return Redis 计数 key
     */
    private String buildDailyCountKey() {
        String userOpenid = BaseContext.getCurrentId(); // 当前小程序用户 openid
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // 当天日期
        return DAILY_COUNT_KEY_PREFIX + today + ":" + userOpenid;
    }

    /**
     * 计算距离明天零点的秒数
     *
     * @return Redis 过期时间
     */
    private long getSecondsUntilTomorrow() {
        LocalDateTime now = LocalDateTime.now(); // 当前时间
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(); // 明天零点
        return Duration.between(now, tomorrow).getSeconds();
    }

    /**
     * 构建包含 WiFi 参数的页面路径
     *
     * @param request WiFi 码请求参数
     * @return 微信小程序页面路径
     */
    private String buildPagePath(MiniWifiCodeRequest request) {
        String ssid = request.getSsid();                    // WiFi 名称原始值
        String password = request.getPassword();            // WiFi 密码原始值
        String pagePath = CONNECT_PAGE + "?s=" + encodeURIComponent(ssid) + "&p=" + encodeURIComponent(password);
        if (pagePath.length() > MAX_PAGE_PATH_LENGTH) {
            throw new ParamException("WiFi名称或密码过长，无法生成小程序码");
        }
        return pagePath;
    }

    /**
     * 调用微信 getQRCode 接口生成小程序码
     *
     * @param pagePath   小程序页面路径
     * @param envVersion 小程序环境
     * @param allowRetry access_token 失效时是否允许重试
     * @return 图片字节
     */
    private byte[] requestQrCode(String pagePath, String envVersion, boolean allowRetry) {
        String accessToken = wechatMiniAuthService.getAccessToken(); // 微信接口凭证
        String url = "https://api.weixin.qq.com/wxa/getwxacode?access_token={accessToken}";

        Map<String, Object> body = new HashMap<>();
        body.put("path", pagePath);
        body.put("width", QR_CODE_WIDTH);
        body.put("env_version", envVersion);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, body, byte[].class, accessToken);
        byte[] responseBody = response.getBody(); // 微信接口原始响应
        if (responseBody == null || responseBody.length == 0) {
            throw new RuntimeException("生成小程序码失败");
        }
        if (isJsonResponse(response.getHeaders(), responseBody)) {
            String errorMsg = new String(responseBody);
            if (allowRetry && (errorMsg.contains("40001") || errorMsg.contains("42001"))) {
                wechatMiniAuthService.clearAccessToken();
                return requestQrCode(pagePath, envVersion, false);
            }
            throw new RuntimeException("生成小程序码失败: " + errorMsg);
        }
        return responseBody;
    }

    /**
     * 识别微信返回是否为 JSON 错误体
     *
     * @param headers      响应头
     * @param responseBody 响应体
     * @return true 表示 JSON 错误体
     */
    private boolean isJsonResponse(HttpHeaders headers, byte[] responseBody) {
        MediaType contentType = headers.getContentType(); // 微信返回内容类型
        if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
            return true;
        }
        return responseBody[0] == '{';
    }

    /**
     * 规范化小程序环境参数
     *
     * @param envVersion 前端传入环境
     * @return 微信支持的环境值
     */
    private String normalizeEnvVersion(String envVersion) {
        if (!StringUtils.hasText(envVersion)) {
            return DEFAULT_ENV_VERSION;
        }
        if ("develop".equals(envVersion) || "trial".equals(envVersion) || "release".equals(envVersion)) {
            return envVersion;
        }
        return DEFAULT_ENV_VERSION;
    }

    /**
     * 按 encodeURIComponent 习惯编码路径参数
     *
     * @param value 原始参数值
     * @return 编码后的参数值
     */
    private String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException("WiFi信息编码失败", exception);
        }
    }
}
