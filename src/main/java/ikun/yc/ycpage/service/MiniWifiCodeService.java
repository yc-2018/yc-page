package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.dto.MiniWifiCodeRequest;
import ikun.yc.ycpage.entity.dto.MiniWifiCodeResponse;

/**
 * 小程序 WiFi 码服务
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
public interface MiniWifiCodeService {
    /**
     * 生成打开 WiFi 连接页的小程序码
     *
     * @param request WiFi 码请求参数
     * @return 小程序码图片信息
     */
    MiniWifiCodeResponse createWifiCode(MiniWifiCodeRequest request);
}
