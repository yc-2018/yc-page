//仰晨study 创建时间2023/6/7 22:38 星期三
package ikun.yc.ycpage.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class}) //加了这两个注解的方法的异常都会被捕捉到
@ResponseBody   //因为要返回json数据的所以需要这个注解来封装
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)   //只要抛出这个sql..的异常就会被捕捉到 进来这个方法
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        log.error(ex.getMessage());
        if (ex.getMessage().contains("Duplicate entry")) {  //如果报错信息包涵Duplicate entry 就能确定是唯一约束键已存在
            String[] split = ex.getMessage().split(" ");  //把报错信息里面的唯一键再分割出来
            String msg = split[2] + "已存在";                    // 唯一键在第二个位置
            return R.error(msg);
        }
        return R.error("失败的man:未知错误");
    }

    /**
     * 自定义的异常的 处理方法
     */
    @ExceptionHandler(LoginException.class)   //只要抛出这个sql..的异常就会被捕捉到 进来这个方法
    public R<String> exceptionHandler(LoginException ex) {
        log.error(ex.getMessage());

        return R.error(ex.getMessage());
    }
}
