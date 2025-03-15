package ikun.yc.ycpage.common.aop;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CacheEvict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis缓存清理切面
 *
 * @author DeepSeek & yc
 * @since 2025/03/15 11:37:17
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheEvictAspect {
    private final RedisTemplate<String, Object> redisTemplate;

    /** 后置通知：在方法成功执行后清理缓存 */
    @AfterReturning(
        value = "@annotation(cacheEvict)",
        returning = "result" // 获取方法返回值
    )
    public void evictCache(CacheEvict cacheEvict, Object result) {
        // 1. 检查返回值是否为R类型
        if (result instanceof R) {
            R<?> rResult = (R<?>) result;

            // 2. 只有当msg为空且code=1时才清理
            if (rResult.getCode() == 1 && rResult.getMsg() == null) {
                doClean(cacheEvict);
            }
        }
        // 3. 非R类型直接清理（根据业务需求调整）
        else {
            doClean(cacheEvict);
        }
    }


    /**
     * 执行实际的缓存清理
     */
    private void doClean(CacheEvict cacheEvict) {
        String region = cacheEvict.value();
        String userId = BaseContext.getCurrentId();

        // 自动清理该用户在该区域的所有缓存
        String pattern = userId + ":" + region + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        log.info("条件满足，已清理缓存：{}:{}", region, userId);
    }
}