//仰晨study 创建时间2023/4/25 20:45 星期二
package ikun.yc.ycpage.common.aop;

import com.alibaba.fastjson.JSONObject;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.entity.OperateLog;
import ikun.yc.ycpage.mapper.OperateLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@RequiredArgsConstructor
@Component
@Slf4j
@Aspect     //切面类
@Order(2) // 设置执行顺序，数值越小优先级越高
public class LogAspect {
    private final OperateLogMapper operateLogMapper;

    /**
     * 记录日志
     *
     * @param joinPoint 连接点
     * @return {@link Object }
     * @author ChenGuangLong
     * @since 2023/12/29 01:23:07
     */

    @Around("@annotation(ikun.yc.ycpage.common.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 操作人ID
        String userId = BaseContext.getCurrentId();

        //操作时间
        LocalDateTime operateTime = LocalDateTime.now();

        //操作类名
        String className = joinPoint.getTarget().getClass().getName();

        //操作方法名
        String methodName = joinPoint.getSignature().getName();

        //操作方法参数
        Object[] args = joinPoint.getArgs();
        String methodParams = Arrays.toString(args);

        long begin = System.currentTimeMillis();
        //调用原始方法
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();

        //方法返回值
        String returnValue = JSONObject.toJSONString(result);

        //操作时间
        Long countTime = end - begin;

        //记录操作日志
        operateLogMapper.insert(new OperateLog(null, userId,operateTime,className,methodName,methodParams,returnValue,countTime));

        return result;
    }
}
