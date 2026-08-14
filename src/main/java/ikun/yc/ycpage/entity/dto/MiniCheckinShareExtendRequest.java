package ikun.yc.ycpage.entity.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 小程序打卡分享续期请求
 *
 * @author cgl
 * @since 2026/06/18
 */
@Data
public class MiniCheckinShareExtendRequest {
    /** 有效期类型 */
    @NotBlank(message = "分享时长不能为空")
    private String durationType;
}
