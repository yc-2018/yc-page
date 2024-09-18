package ikun.yc.ycpage.entity.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 查看时间类型
 *
 * @author ChenGuangLong
 * @since  2024/09/18 21:24:18
 */
@Getter
@AllArgsConstructor
public enum SeeTimeType {
    DAY(1),
    WEEK(2),
    MONTH(3),
    YEAR(4);

    private final int value;
}
