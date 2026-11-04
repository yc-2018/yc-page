package ikun.yc.ycpage.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 小程序订阅消息授权记录请求
 *
 * @author cgl
 * @since 2026/11/05
 */
@Data
public class MiniSubscribeMessageRecordRequest {
    /** 用户已同意接收的模板ID列表 */
    @NotEmpty(message = "订阅模板不能为空")
    @Size(max = 3, message = "订阅模板数量有误")
    private List<String> templateIds;
}
