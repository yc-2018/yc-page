package ikun.yc.ycpage.common.exception;

/**
 * 数据更新异常类
 *
 * @author cgl
 * date 2025/07/16 20:43:16
 */
public class SqlSaveException extends RuntimeException{
    public SqlSaveException(String message) {
        super(message);
    }
}
