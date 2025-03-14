package ikun.yc.ycpage.common.aop;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.anno.RedisCache;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Redis缓存切面
 * 参数名问题：确保编译时保留参数名（在Maven/Gradle中配置-parameters编译选项）。
 * 空值处理：当前逻辑不缓存null结果，若需防止缓存穿透可缓存空对象并设置较短过期时间。
 * Key冲突：复杂参数可能导致Key过长，建议对参数值进行摘要处理（如MD5），但需平衡可读性与性能。
 *
 * @author DeepSeek & yc
 * @since 2025/03/14 15:12:15
 */
@Aspect
@Order(5) // 设置执行顺序，数值越小优先级越高
@Component
@RequiredArgsConstructor
public class RedisCacheAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    // 参数名称发现器（Spring内置）
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(redisCache)")
    public Object around(ProceedingJoinPoint joinPoint, RedisCache redisCache) throws Throwable {
        // 生成唯一缓存Key
        String cacheKey = generateCacheKey(joinPoint);

        // 检查缓存是否存在
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) return cachedValue; // 命中缓存直接返回

        // 未命中缓存，执行原方法
        Object result = joinPoint.proceed();

        // 将结果存入Redis（非空且返回key不是X 时缓存）
        if (result != null && !"X".equals(cacheKey)) {
            redisTemplate.opsForValue().set(
                cacheKey, 
                result, 
                redisCache.expireTime(), 
                redisCache.timeUnit()
            );
        }

        return result;
    }

    /**
     * 获取生成缓存密钥
     * 构建缓存键，格式为userId:methodName:paramName1=value1:paramName2=value2...
     * 如果参数为空，则只返回用户ID和方法名
     * ### 不建议有对象 如果真有对象而且没重写toString方法，会没有意义，如果真要太长的可以把key使用md5
     *
     * @param joinPoint 连接点
     * @return {@link String }
     * @author DeepSeek & yc
     * @since 2025/03/14 14:37:01
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint) {
        // 获取用户ID
        String userId = BaseContext.getCurrentId();

        // 获取方法名
        String methodName = joinPoint.getSignature().getName();

        // 获取参数信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();   // 将切点签名转换为方法签名，用于获取具体方法信息
        Method method = methodSignature.getMethod();                                    // 从方法签名中获取被拦截的Method对象
        Object[] args = joinPoint.getArgs();                                            // 获取目标方法的入参列表

        // 获取参数名称（优先使用Spring的发现器）
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames == null) { // 处理无法获取参数名的情况   使用 JDK 7 或更低版本才会获取不到
            paramNames = new String[args.length];
            Arrays.setAll(paramNames, i -> "arg" + i);
        }

        // 构建Key字符串
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(userId).append(":").append(methodName);

        for (int i = 0; i < args.length; i++) {
            String paramName = (i < paramNames.length) ? paramNames[i] : "arg" + i;
            Object argValue = args[i];
            keyBuilder.append(":")
                .append(paramName)
                .append("=")
                .append(convertToString(argValue));
        }

        String string = keyBuilder.toString();
        if (string.length() > 512) return "X";  // 缓存键过长 就不要缓存了吧
        return string;
    }

    /**
     * 将任意对象转换为字符串，处理 null 和 数组类型 的特殊情况。
     *
     * @param obj 对象
     * @return {@link String }
     * @author DeepSeek & yc
     * @since 2025/03/14 15:10:18
     */
    private String convertToString(Object obj) {
        if (obj == null) return "null";     // 处理空值
        if (obj.getClass().isArray()) {     // 检查是否是数组
            return handleArray(obj);        // 调用专用方法处理数组
        }
        return obj.toString();              // 非数组对象直接调用toString()
    }

    /**
     * 统一处理所有类型的数组
     *
     * @param array 数组
     * @return {@link String }
     * @author DeepSeek & yc
     * @since 2025/03/14 15:08:00
     */
    private String handleArray(Object array) {
        if (array instanceof Object[]) {
            return Arrays.toString((Object[]) array);
        } else if (array instanceof int[]) {
            return Arrays.toString((int[]) array);
        } else if (array instanceof long[]) {
            return Arrays.toString((long[]) array);
        } else if (array instanceof double[]) {
            return Arrays.toString((double[]) array);
        } else if (array instanceof char[]) {
            return Arrays.toString((char[]) array);
        } else if (array instanceof byte[]) {
            return Arrays.toString((byte[]) array);
        } else if (array instanceof boolean[]) {
            return Arrays.toString((boolean[]) array);
        } else if (array instanceof float[]) {
            return Arrays.toString((float[]) array);
        } else if (array instanceof short[]) {
            return Arrays.toString((short[]) array);
        }
        return "UnsupportedArrayType";  // 未知数组类型（理论上不会触发）
    }
}