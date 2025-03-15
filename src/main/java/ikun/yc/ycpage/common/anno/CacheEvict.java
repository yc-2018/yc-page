package ikun.yc.ycpage.common.anno;// CacheEvict.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 清除缓存
 *
 * @author DeepSeek
 * @since 2025/03/15 10:00:15
 */
@Target(ElementType.METHOD) // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface CacheEvict {
    /** 要清理的缓存区域（必须与@RedisCache的value对应） */
    String value();
}