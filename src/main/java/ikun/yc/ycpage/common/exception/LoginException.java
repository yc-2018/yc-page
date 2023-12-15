//仰晨study 创建时间2023/6/9 16:52 星期五
package ikun.yc.ycpage.common.exception;

/**
 * 登录业务异常类
 */
public class LoginException extends RuntimeException{
    public LoginException(String message) {
        super(message);
    }
}
