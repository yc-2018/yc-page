package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.SearchEngineDataInitializer;
import ikun.yc.ycpage.common.exception.LoginException;
import ikun.yc.ycpage.entity.Bookmarks;
import ikun.yc.ycpage.entity.User;
import ikun.yc.ycpage.entity.UserConfig;
import ikun.yc.ycpage.mapper.UserMapper;
import ikun.yc.ycpage.service.BookmarksService;
import ikun.yc.ycpage.service.SearchEnginesService;
import ikun.yc.ycpage.service.UserConfigService;
import ikun.yc.ycpage.service.UserService;
import ikun.yc.ycpage.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static ikun.yc.ycpage.controller.BookmarksController.BOOKMARK_ROOT;

/**
 * 用户的服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final RedisTemplate<String, String> redisTemplate;
    private final SearchEnginesService searchEnginesService;
    private final SearchEngineDataInitializer searchEngineDataInitializer;
    private final UserConfigService userConfigService;
    private final BookmarksService bookmarksService;


    /**
     * 登录（同一个 ip一分钟内请求超过3次，封禁一个小时。）
     * @param request 用来获取用户的ip
     * @param key 验证码
     * @param expireTime 登录超时时间
     * @return 失败或jwt
     */
    @Override
    @Transactional
    public R<?> login(HttpServletRequest request, String key, String expireTime){
        try {
            String ip = extractClientIp(request);     // IP地址请求
            String banKey = "ban_" + ip;             // 用来标识被封禁 IP 的键
            String attemptKey = "attempt_" + ip;    // 用来跟踪每个 IP 地址登录尝试次数的键

            // 检查IP是否被封禁
            if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey)))
                return R.error("您因频繁登录 目前已被封禁1小时，请稍后再试");

            // 增加登录尝试次数，如果 attemptKey 不存在，则 Redis 会创建它值为 1然后返回 本来存在就会自增
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            if (attempts!=null) {   // 其实不可能等于null,只是好烦这个 idea代码提示就先写上了。
                if (attempts == 1)
                    // 如果是第一次尝试，设置过期时间为1分钟
                    redisTemplate.expire(attemptKey, 1, TimeUnit.MINUTES);

                // 检查尝试次数是否超过限制
                if (attempts > 3) {
                    // 封禁IP一个小时
                    redisTemplate.opsForValue().set(banKey, "banned", 1, TimeUnit.HOURS);
                    redisTemplate.delete(attemptKey); // 重置尝试次数
                    return R.error("尝试次数过多，您已被封禁");
                }
            }

            // ---------------正常登录逻辑--------------
            // 校验验证码
            String userId = redisTemplate.opsForValue().get(key);
            if (userId == null) {
                return R.error("验证码不存在");
            }

            // 校验验证码成功，判断是否是新用户
            User user = this.getById(userId);
            if (user == null) {
                user = new User();
                log.info("{}是新用户",userId);
                this.save(user.setId(userId));
                bookmarksService.save(new Bookmarks().setUserId(userId).setType(BOOKMARK_ROOT));  // 保存默认的书签根
                userConfigService.save(new UserConfig().setUserId(userId));   // 保存默认用户参数
                searchEnginesService.saveBatch(searchEngineDataInitializer.getInitialSearchEngines(userId));  // 保存初始搜索引擎
            }

            log.info("用户：{},登录", userId);
            User loginUser = user;     // 不重新赋值会报错 有点神奇
            String generateJwt = JwtUtils.generateJwt(new HashMap<String, Object>() {{
                put("userId", userId);
                put("username", loginUser.getUsername());
                put("avatar", loginUser.getAvatar());
            }}, expireTime);
            return R.success(generateJwt);
        } catch (Exception e) {
            log.error("登录异常失败", e);
            throw new LoginException("😢登录居然失败，这种情况非常罕见，请联系站长或请重新试试");
        }
    }


    /**
     * 如果没用 Nginx 转发的话，request.getRemoteAddr()直接就能获取到地址，
     * 但是用 Nginx 转发，就算 Nginx 配置了
     *    proxy_set_header Host $host;
     *    proxy_set_header X-Real-IP $remote_addr;
     *    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     *    还是拿不到，就用这个方法。(不安全啊。使用请求头肯定可以被前端给改的。最好就是不搞代理，但是要处理跨越。)
     * @param request 用来获取用户的ip
     * @return 原始客户端IP
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");

        if (xForwardedForHeader == null) {
            String xRealIp = request.getHeader("X-Real-IP");
            return xRealIp != null ? xRealIp : request.getRemoteAddr();
        } else
            // X-Forwarded-For可能包含多个IP地址，第一个是原始客户端IP
            return xForwardedForHeader.split(",")[0];
    }
}