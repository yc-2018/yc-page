package ikun.yc.ycpage.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通行证令牌 注解 有这个注解的类或方法接口，不需要验证token
 *
 * @author ChenGuangLong
 * @since 2025/05/10 09:23:30
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PassToken {
}