package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序打卡分享创建响应
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
@AllArgsConstructor
public class MiniCheckinShareCreateResponse {
    /** 分享ID */
    private Integer id;

    /** 有效截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
