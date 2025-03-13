package ikun.yc.ycpage.common.anno;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 计数控制：一些接口访问频率控制
 *
 * @author ChenGuangLong
 * @since 2024/01/23 10:03:30
 */
@Retention(RetentionPolicy.RUNTIME) //运行时有效
@Target(ElementType.METHOD)         //允许写在方法上
public @interface CountControl {
    /**
     * 操作类型
     * @author ChenGuangLong
     * @since 2024/01/23 11:35:49
     */
    int operationType() default 0;


    /**
     * 过期时间(redis的key过期时间 在这个有效期里面最多可以访问 “控制频率” 的次数)
     * @author ChenGuangLong
     * @since 2024/01/23 14:52:30
     */
    int expireTime() default 1;

    /**
     * 控制频率 x分钟/y次 中的y
     * @author ChenGuangLong
     * @since 2024/01/23 11:36:11
     */
    int frequency() default 5;

    /**
     * 禁用时间(分钟)
     * @author ChenGuangLong
     * @since 2024/01/23 14:49
     */
    int banMin() default 1;


    /**
     * 可以指定错误信息
     *
     * @author ChenGuangLong
     * @since 2025/03/13 10:50:18
     */
    String msg() default "";
}
