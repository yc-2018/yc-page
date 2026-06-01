package ikun.yc.ycpage.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序 WiFi 码生成结果
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniWifiCodeResponse {
    private String imageBase64; // 小程序码图片 base64

    private String mimeType;    // 小程序码图片类型

    private String pagePath;    // 小程序码打开路径
}
