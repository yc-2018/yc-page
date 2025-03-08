package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.service.MiniUsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序控制器
 *
 * @author yc
 * @since 2025-3-8
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/mini")
public class MiniController {
    private final MiniUsersService miniUsersService;

    @PostMapping("/login")
    public R<String> wechatLogin(String code) {
        return R.success(miniUsersService.miniLogin(code));
    }
}