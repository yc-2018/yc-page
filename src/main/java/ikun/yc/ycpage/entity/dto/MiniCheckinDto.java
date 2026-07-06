package ikun.yc.ycpage.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 小程序打卡搜索列表请求参数
 *
 * @author cgl
 * @since 2025/03/09 16:15:09
 */
@Getter
@Setter
public class MiniCheckinDto {
    private Instant startTime;  // 开始时间，时间戳类型mysql对应秒 前端生成毫秒要除1000
    private Instant endTime;

    private String address;
    private String remark;
    private String locationType;

    private BigDecimal nearbyLongitude; // 附近搜索中心经度
    private BigDecimal nearbyLatitude;  // 附近搜索中心纬度
    private Integer nearbyRadius;       // 附近搜索半径，单位米
}
