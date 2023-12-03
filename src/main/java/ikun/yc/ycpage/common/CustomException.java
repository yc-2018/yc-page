//仰晨study 创建时间2023/6/9 16:52 星期五
package ikun.yc.ycpage.common;

/**
 * 自定义业务异常类
 */
public class CustomException extends RuntimeException{
    public CustomException(String message) {
        super(message);
    }
}
