package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.dto.MiniWifiCodeRequest;
import ikun.yc.ycpage.entity.dto.MiniWifiCodeResponse;
import ikun.yc.ycpage.service.MiniWifiCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 小程序 WiFi 码控制器
 *
 * @author ChenGuangLong
 * @since 2026/06/01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/wifi")
public class MiniWifiCodeController {
    private final MiniWifiCodeService miniWifiCodeService;

    /**
     * 生成 WiFi 连接页小程序码
     *
     * @param request WiFi 码请求参数
     * @return 小程序码图片信息
     */
    @PostMapping("/qrcode")
    public R<MiniWifiCodeResponse> createWifiQrCode(@RequestBody @Valid MiniWifiCodeRequest request) {
        return R.success(miniWifiCodeService.createWifiCode(request));
    }
}
