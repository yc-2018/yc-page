package ikun.yc.ycpage.entity.enumeration;

import ikun.yc.ycpage.common.exception.ParamException;

import java.time.LocalDateTime;

/**
 * 小程序打卡分享有效期类型
 *
 * @author cgl
 * @since 2026/06/18
 */
public enum MiniCheckinShareDurationType {
    /** 一天 */
    ONE_DAY,
    /** 三天 */
    THREE_DAYS,
    /** 七天 */
    SEVEN_DAYS,
    /** 一个月 */
    ONE_MONTH,
    /** 永久有效到 2099 年 */
    FOREVER;

    /**
     * 根据前端传入编码解析有效期类型
     *
     * @param code 前端有效期编码
     * @return 有效期类型
     */
    public static MiniCheckinShareDurationType fromCode(String code) {
        if (code == null) {
            throw new ParamException("分享时长不能为空");
        }
        try {
            return MiniCheckinShareDurationType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ParamException("分享时长有误");
        }
    }

    /**
     * 计算过期时间
     *
     * @param now 当前服务器时间
     * @return 过期时间
     */
    public LocalDateTime resolveExpireTime(LocalDateTime now) {
        switch (this) {
            case ONE_DAY:
                return now.plusDays(1);
            case THREE_DAYS:
                return now.plusDays(3);
            case SEVEN_DAYS:
                return now.plusDays(7);
            case ONE_MONTH:
                return now.plusMonths(1);
            case FOREVER:
                return LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            default:
                throw new ParamException("分享时长有误");
        }
    }
}
