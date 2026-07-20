package ikun.yc.ycpage.common.exception;

/** 乐观锁冲突异常。 */
public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException() {
        super("数据已被修改，请刷新后重试");
    }
}
