// 设置用户id切面 2024-9-6 02:24
package ikun.yc.ycpage.common.aop;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.anno.UserId;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;

@Aspect
@Component
@Order(9) // 设置执行顺序，数值越小优先级越高
public class UserIdAspect {
    // 方法之前执行
    @Before("@annotation(userIdAnnotation)")
    public void setUserId(JoinPoint joinPoint, UserId userIdAnnotation) throws IllegalAccessException {
        Object[] args = joinPoint.getArgs();    // 获取方法参数
        int index = userIdAnnotation.value() - 1; // 获取注解中的value属性，减1以匹配数组索引
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature(); // 获取方法签名（包括方法的名称、修饰符、返回类型、参数类型等)
        Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();  // 获取方法参数类型

        // 给的参数索引不正常，则遍历所有参数
        if (index < 0 || index >= args.length)
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Class<?> parameterType = parameterTypes[i];
                Field idField = getIdField(parameterType);
                if (idField != null && arg != null) {
                    setId(idField, arg);
                }
            }
        else {  // 给的参数索引正常
            Object arg = args[index];
            Class<?> parameterType = parameterTypes[index];
            Field idField = getIdField(parameterType);
            if (idField != null && arg != null) {
                setId(idField, arg);
            }
        }
    }

    // 获取userId字段
    private Field getIdField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        return Arrays.stream(fields)
                .filter(field -> field.getName().equals("userId"))
                .findFirst()
                .orElse(null);
    }

    // 设置userId
    private void setId(Field idField, Object object) throws IllegalAccessException {
        idField.setAccessible(true);                    // 设置访问权限
        try {
            String userId = BaseContext.getCurrentId();  // 当前线程中获取当前用户id
            idField.set(object, userId);                 // 设置用户id
        } finally {
            idField.setAccessible(false);               // 恢复访问权限
        }
    }
}
