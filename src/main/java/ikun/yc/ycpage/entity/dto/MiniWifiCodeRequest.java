package ikun.yc.ycpage.entity.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 小程序 WiFi 码生成请求参数
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
@Data
public class MiniWifiCodeRequest {
    @NotBlank(message = "WiFi名称不能为空")
    private String ssid;        // WiFi 名称

    @NotBlank(message = "WiFi密码不能为空")
    @Size(min = 8, message = "WiFi密码至少8位")
    private String password;    // WiFi 密码

    private String envVersion;  // 生成码打开的小程序环境
}
