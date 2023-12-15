//仰晨 创建时间2023/12/16
package ikun.yc.ycpage.common.exception;

/**
 * 字段为空异常类
 */
public class FieldIsNullException extends RuntimeException{
    public FieldIsNullException(String message) {
        super(message);
    }
}
