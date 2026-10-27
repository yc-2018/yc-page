package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 小程序多地打卡分享创建请求
 *
 * @author cgl
 * @since 2026/10/27
 */
@Data
public class MiniCheckinShareMultiCreateRequest {
    /** 参与分享的打卡记录ID，按展示顺序传入 */
    @NotEmpty(message = "打卡记录不能为空")
    private List<Integer> recordIds;

    /** 有效期类型 */
    @NotBlank(message = "分享时长不能为空")
    private String durationType;

    /** 创建人openid，由登录态注入 */
    @JsonIgnore
    private String userOpenid;
}
