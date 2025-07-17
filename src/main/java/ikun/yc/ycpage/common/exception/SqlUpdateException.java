package ikun.yc.ycpage.common.exception;

/**
 * 数据更新异常类
 *
 * @author cgl
 * date 2025/07/16 20:43:16
 */
public class SqlUpdateException extends RuntimeException{
    public SqlUpdateException(String message) {
        super(message);
    }
}
