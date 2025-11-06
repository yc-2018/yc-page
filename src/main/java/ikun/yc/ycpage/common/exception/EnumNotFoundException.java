package ikun.yc.ycpage.common.exception;

/**
 * 枚举没此参数异常类
 */
public class EnumNotFoundException extends RuntimeException{
    public EnumNotFoundException(String message) {
        super(message);
    }
}
