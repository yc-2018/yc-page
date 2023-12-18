//仰晨study 创建时间2023/12/18 23:44 星期一
package ikun.yc.ycpage.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 控制用户一分钟之内不能添加超过十次待办事项，如果超过禁用五分钟。
 */
@Component
@RequiredArgsConstructor
public class ControlAddItemUtil {
    private static final long EXPIRE_TIME = 60;       // 1分钟
    private static final long BAN_DURATION = 5 * 60; // 5分钟
    private static final int MAX_REQUESTS = 10;     // 10次
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * @param userId 用户id
     * @return 如果被禁用了，返回真。没有被禁用，返回假。
     */
    public  boolean getOneMinuteAddItemById(String userId) {
        String key = "addCount:" + userId;

        // 检查用户是否被禁用
        if (Boolean.TRUE.equals(redisTemplate.hasKey("banAddItem:" + userId)))
            return true;

        // 增加请求计数
        Long requestCount = redisTemplate.opsForValue().increment(key);
        if (requestCount != null && requestCount == 1)
            redisTemplate.expire(key, EXPIRE_TIME, TimeUnit.SECONDS);

        // 如果超过请求限制，禁用用户请求
        if (requestCount != null && requestCount > MAX_REQUESTS) {
            redisTemplate.opsForValue().set("banAddItem:" + userId, "true", BAN_DURATION, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }
}
