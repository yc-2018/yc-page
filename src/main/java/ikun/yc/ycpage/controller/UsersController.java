package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.JwtUtils;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Users;
import ikun.yc.ycpage.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UsersController {
    private final UsersService usersService;
    public final RedisTemplate<String, String> redisTemplate;

    /**
     * 登录（同一个 ip一分钟内请求超过十次，封禁一个小时。）
     * @param request 用来获取用户的ip
     * @param key 验证码
     * @param expireTime 登录超时时间
     * @return jwt
     */
    @PostMapping("login")
    public R<?> login(HttpServletRequest request, String key, @RequestParam(defaultValue = "bt") String expireTime ) {
        log.info("用户登录ip{}",request.getRemoteAddr());
        String ip = extractClientIp(request);    // IP地址请求
        String banKey = "ban_" + ip;            // 用来标识被封禁 IP 的键
        String attemptKey = "attempt_" + ip;    // 用来跟踪每个 IP 地址登录尝试次数的键

        // 检查IP是否被封禁
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey)))
            return R.error("您因频繁登录ip已被封禁1小时，请稍后再试");

        // 增加登录尝试次数，如果 attemptKey 不存在，则 Redis 会创建它值为 1然后返回 本来存在就会自增
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts!=null) {   // 其实不可能等于null,只是好烦这个 idea代码提示就先写上了。
            if (attempts == 1)
                // 如果是第一次尝试，设置过期时间为1分钟
                redisTemplate.expire(attemptKey, 1, TimeUnit.MINUTES);

            // 检查尝试次数是否超过限制
            if (attempts > 10) {
                // 封禁IP一个小时
                redisTemplate.opsForValue().set(banKey, "banned", 1, TimeUnit.HOURS);
                redisTemplate.delete(attemptKey); // 重置尝试次数
                return R.error("尝试次数过多，您已被封禁");
            }
        }

        // 正常登录逻辑
        String user = redisTemplate.opsForValue().get(key);
        if (user == null) {
            return R.error("验证码不存在");
        }
        log.info("用户：{},登录", user);
        Users loginUser = usersService.getById(user);
        if (loginUser == null) {
            log.info("是新用户");
            usersService.save(new Users().setId(user));
        }
        return R.success(JwtUtils.generateJwt(new HashMap<String, Object>(){{put("userId", user);}},expireTime));
    }


    @GetMapping("/get")
    public Object get() {
        return usersService.list();
    }

    private String extractClientIp(HttpServletRequest request) {
        log.info("用户登录ip{}",request.getRemoteAddr());
        String ip = request.getRemoteAddr();                          // 直接获取IP地址试试
        if (!ip.equals("0:0:0:0:0:0:0:1")&&!ip.equals("127.0.0.1"))   // 排除非nginx转发
            return ip;

        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        log.info("xForwardedForHeader = " + xForwardedForHeader);
        if (xForwardedForHeader == null) {
            String xRealIp = request.getHeader("X-Real-IP");
            log.info("xRealIp = " + xRealIp);
            return xRealIp != null ? xRealIp : request.getRemoteAddr();
        } else {
            // X-Forwarded-For可能包含多个IP地址，第一个是原始客户端IP
            return xForwardedForHeader.split(",")[0];
        }
    }
}