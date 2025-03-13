package ikun.yc.ycpage.common.aop;


import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.anno.CountControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 计数控制切面类
 *
 * @author ChenGuangLong
 * @since 2024/01/23 10:11:47
 */
@RequiredArgsConstructor
@Component
@Slf4j
@Aspect     //切面类
@Order(1) // 设置执行顺序，数值越小优先级越高
public class CountControlAspect {
    public static final int ADD = 1;
    public static final int UPDATE = 2;
    public static final int DELETE = 3;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 计数控制
     * redis按 类型+控制频率+禁用时间+超时时间+用户id
     * <br/>默认是 1分钟内超过5次禁用1分钟
     *
     * @param joinPoint 连接点
     * @author ChenGuangLong
     * @since 2024/01/23 15:09:45
     */

    @Around("@annotation(ikun.yc.ycpage.common.anno.CountControl)")
    public Object countingControl(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取被注解的方法上的注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        CountControl countControlAnnotation = methodSignature.getMethod().getAnnotation(CountControl.class);

        String userId = BaseContext.getCurrentId();

        // 获取注解的属性值
        int operationType = countControlAnnotation.operationType();         // 操作类型
        int controlFrequency = countControlAnnotation.frequency();          // 控制频率（次）
        int banTime = countControlAnnotation.banMin();                      // 禁用时间（分钟）
        int expireTime = countControlAnnotation.expireTime();               // 超时时间（分钟）
        String msg = countControlAnnotation.msg();                          // 提示信息
        String massage = msg.isEmpty() ? "请求次数超过限制，请" + banTime + "分钟后再试试" : msg;

        // 组装Redis的key
        String key =  "-"+ operationType + controlFrequency + banTime+ expireTime + userId;
        String banKey = "ban" + key;
        String countKey = "count" + key;
        // 检查用户是否被禁用
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey)))
            throw new RuntimeException(massage);

        // 使用RedisTemplate进行自增操作，不存在会当成0进行自增变成1
        Long requestCount = redisTemplate.opsForValue().increment(countKey);

        if (requestCount == 1) // 第一次请求，设置过期时间
            redisTemplate.expire(countKey, expireTime, TimeUnit.MINUTES);

        // 如果超过请求限制，禁用用户请求
        if (requestCount > controlFrequency) {
            redisTemplate.opsForValue().set(banKey, "true", banTime, TimeUnit.MINUTES);
            throw new RuntimeException("请求次数超过限制，请" + banTime + "分钟后再试试");
        }

        // 执行原始方法
        return joinPoint.proceed();
    }
}
