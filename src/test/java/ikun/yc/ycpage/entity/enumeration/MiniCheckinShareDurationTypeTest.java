package ikun.yc.ycpage.entity.enumeration;

import ikun.yc.ycpage.common.exception.ParamException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 小程序打卡分享有效期类型测试
 *
 * @author cgl
 * @since 2026/06/18
 */
class MiniCheckinShareDurationTypeTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 6, 18, 12, 0, 0); // 固定测试基准时间

    /** 校验所有分享时长的过期时间计算 */
    @Test
    void resolveExpireTimeShouldMatchDurationType() {
        assertEquals(BASE_TIME.plusDays(1), MiniCheckinShareDurationType.ONE_DAY.resolveExpireTime(BASE_TIME));
        assertEquals(BASE_TIME.plusDays(3), MiniCheckinShareDurationType.THREE_DAYS.resolveExpireTime(BASE_TIME));
        assertEquals(BASE_TIME.plusDays(7), MiniCheckinShareDurationType.SEVEN_DAYS.resolveExpireTime(BASE_TIME));
        assertEquals(BASE_TIME.plusMonths(1), MiniCheckinShareDurationType.ONE_MONTH.resolveExpireTime(BASE_TIME));
        assertEquals(LocalDateTime.of(2099, 12, 31, 23, 59, 59), MiniCheckinShareDurationType.FOREVER.resolveExpireTime(BASE_TIME));
    }

    /** 校验前端编码解析兼容大小写和空格 */
    @Test
    void fromCodeShouldNormalizeCode() {
        assertEquals(MiniCheckinShareDurationType.ONE_DAY, MiniCheckinShareDurationType.fromCode(" one_day "));
    }

    /** 校验非法分享时长会抛出参数异常 */
    @Test
    void fromCodeShouldRejectInvalidCode() {
        assertThrows(ParamException.class, () -> MiniCheckinShareDurationType.fromCode("BAD"));
        assertThrows(ParamException.class, () -> MiniCheckinShareDurationType.fromCode(null));
    }
}
