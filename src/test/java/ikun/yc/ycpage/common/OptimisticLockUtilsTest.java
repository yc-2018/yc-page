package ikun.yc.ycpage.common;

import ikun.yc.ycpage.common.exception.OptimisticLockException;
import ikun.yc.ycpage.common.exception.ParamException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 乐观锁公共校验测试。 */
class OptimisticLockUtilsTest {

    /** 缺少版本号时应拒绝写入。 */
    @Test
    void requireVersionRejectsNullVersion() {
        ParamException exception = assertThrows(ParamException.class,
                () -> OptimisticLockUtils.requireVersion(null));
        assertEquals("版本号不能为空", exception.getMessage());
    }

    /** 更新零行时应统一抛出冲突异常。 */
    @Test
    void requireUpdatedRejectsConflict() {
        OptimisticLockException exception = assertThrows(OptimisticLockException.class,
                () -> OptimisticLockUtils.requireUpdated(false));
        assertEquals("数据已被修改，请刷新后重试", exception.getMessage());
    }

    /** 更新成功时不应抛出异常。 */
    @Test
    void requireUpdatedAcceptsSuccessfulUpdate() {
        assertDoesNotThrow(() -> OptimisticLockUtils.requireUpdated(true));
    }

    /** 仅改变顺序时应允许提交。 */
    @Test
    void requireSameIdsAcceptsReorder() {
        assertDoesNotThrow(() -> OptimisticLockUtils.requireSameIds(
                Arrays.asList("1", "2", "3"), Arrays.asList("3", "1", "2")));
    }

    /** 排序中出现重复ID时应拒绝提交。 */
    @Test
    void requireSameIdsRejectsDuplicateId() {
        assertThrows(ParamException.class, () -> OptimisticLockUtils.requireSameIds(
                Arrays.asList("1", "2", "3"), Arrays.asList("1", "1", "2")));
    }

    /** 排序中缺失或混入其他ID时应拒绝提交。 */
    @Test
    void requireSameIdsRejectsChangedIdSet() {
        assertThrows(ParamException.class, () -> OptimisticLockUtils.requireSameIds(
                Arrays.asList("1", "2", "3"), Arrays.asList("1", "2", "4")));
    }
}
