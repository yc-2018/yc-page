package ikun.yc.ycpage.common;

import ikun.yc.ycpage.common.exception.OptimisticLockException;
import ikun.yc.ycpage.common.exception.ParamException;

import java.util.Collection;
import java.util.HashSet;

/** 乐观锁公共校验工具。 */
public final class OptimisticLockUtils {
    private OptimisticLockUtils() {
    }

    /** 校验请求是否携带版本号。 */
    public static void requireVersion(Integer version) {
        if (version == null) throw new ParamException("版本号不能为空");
    }

    /** 校验 CAS 更新是否命中目标记录。 */
    public static void requireUpdated(boolean updated) {
        if (!updated) throw new OptimisticLockException();
    }

    /** 校验排序 ID 未缺失、未增加且没有重复。 */
    public static void requireSameIds(Collection<String> currentIds, Collection<String> submittedIds) {
        if (currentIds == null || submittedIds == null
                || currentIds.size() != submittedIds.size()
                || new HashSet<>(submittedIds).size() != submittedIds.size()
                || !new HashSet<>(currentIds).equals(new HashSet<>(submittedIds))) {
            throw new ParamException("排序数据有误，请刷新后重试");
        }
    }
}
