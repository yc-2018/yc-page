package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.MiniTotp;
import ikun.yc.ycpage.service.MiniTotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序2FA验证码控制器
 *
 * @author cgl
 * @since 2026/10/06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/totp")
public class MiniTotpController {
    private final MiniTotpService miniTotpService;

    /** 获取当前用户的全部2FA条目，小程序侧用这份数据展示验证码和导出 */
    @GetMapping("/list")
    public R<List<MiniTotp>> list() {
        return R.success(miniTotpService.listCurrentUserTotps());
    }

    /** 新增当前用户的2FA条目 */
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<Boolean> add(@RequestBody MiniTotp totp) {
        return miniTotpService.addCurrentUserTotp(totp)
                ? R.success(true)
                : R.error("新增失败");
    }

    /** 修改当前用户的2FA条目，名称、账号和密钥都可以改 */
    @PostMapping("/update")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> update(@RequestBody MiniTotp totp) {
        return miniTotpService.updateCurrentUserTotp(totp)
                ? R.success(true)
                : R.error("修改失败");
    }

    /** 置顶当前用户的指定2FA条目 */
    @PostMapping("/top/{id}")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> top(@PathVariable Integer id) {
        return miniTotpService.setCurrentUserTotpTop(id, true)
                ? R.success(true)
                : R.error("置顶失败");
    }

    /** 取消置顶当前用户的指定2FA条目 */
    @PostMapping("/cancelTop/{id}")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> cancelTop(@PathVariable Integer id) {
        return miniTotpService.setCurrentUserTotpTop(id, false)
                ? R.success(true)
                : R.error("取消置顶失败");
    }

    /** 删除当前用户的指定2FA条目 */
    @PostMapping("/delete/{id}")
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 10)
    public R<Boolean> delete(@PathVariable Integer id) {
        return miniTotpService.deleteCurrentUserTotp(id)
                ? R.success(true)
                : R.error("删除失败");
    }
}
