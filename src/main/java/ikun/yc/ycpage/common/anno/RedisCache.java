package ikun.yc.ycpage.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存 用于get请求缓存
 *
 * @author ChenGuangLong
 * @since 2025/03/14 14:24:42
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {
    int expireTime() default 1;                     // 缓存过期时间
    TimeUnit timeUnit() default TimeUnit.DAYS;      // 时间单位
}