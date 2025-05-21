//仰晨study 创建时间2023/6/7 22:38 星期三
package ikun.yc.ycpage.common.exception;

import ikun.yc.ycpage.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Objects;

/**
 * 全局异常处理
 * 1. 使用@RestControllerAdvice代替@ControllerAdvice+@ResponseBody(因为要返回json数据的所以需要这个注解来封装)
 * 2. 使用@Order控制处理器优先级
 */
@Slf4j
@RestControllerAdvice   // (annotations = {RestController.class, Controller.class}) //加了这两个注解的方法的异常都会被捕捉到
public class GlobalExceptionHandler {

    /** SQL完整性约束违规异常异常处理方法 */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)   //只要抛出这个sql..的异常就会被捕捉到 进来这个方法
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        log.error(ex.getMessage());
        if (ex.getMessage().contains("Duplicate entry")) {  //如果报错信息包涵Duplicate entry 就能确定是唯一约束键已存在
            String[] split = ex.getMessage().split(" ");  //把报错信息里面的唯一键再分割出来
            String msg = split[2] + "已存在";                    // 唯一键在第二个位置
            return R.error(msg);
        }
        return R.error("SQL完整性约束违规异常");
    }

    /** 参数验证异常处理方法 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMsg = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return R.error("参数错误: " + errorMsg);
    }

    /** 兜底异常处理方法 */
    @ExceptionHandler({RuntimeException.class}) // 捕获所有继承自RuntimeException的异常
    public R<String> exceptionHandler(RuntimeException ex) {
        log.error(ex.getMessage());
        return R.error(ex.getMessage());
    }
}
