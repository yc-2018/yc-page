package ikun.yc.ycpage.entity.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 小程序打卡搜索列表请求参数
 *
 * @author cgl
 * @since 2025/03/09 16:15:09
 */
@Getter
@Setter
public class MiniCheckinDto {
    private Integer startTime;
    private Integer endTime;

    private String address;
    private String remark;
    private String locationType;
}
