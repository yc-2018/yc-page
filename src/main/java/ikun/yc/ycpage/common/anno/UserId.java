package ikun.yc.ycpage.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Target(ElementType.METHOD)         // 允许写在方法上
public @interface UserId {
    /**
     * 在那个参数上尝试注入用户id？ 0全部尝试（默认），1表示从第一个参数中尝试注入，以此类推
     * 给的数字不正确：全部尝试注入
     */
    int value() default 0;
}
