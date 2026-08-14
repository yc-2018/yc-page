//仰晨study 创建时间2023/6/9 16:52 星期五
package ikun.yc.ycpage.common.exception;

/**
 * 登录业务异常类
 */
public class LoginException extends RuntimeException{
    public LoginException(String message) {
        super(message);
    }

    /**
     * 创建保留原始异常链的登录异常。
     *
     * @param message 对外登录失败提示
     * @param cause 原始异常
     */
    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
