package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.UserConfig;
import ikun.yc.ycpage.service.UserConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


/**
 * 用户配置控制器
 *
 * @author ChenGuangLong
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/userConfig")
public class UserConfigController {
    private final UserConfigService userConfigService;

    /**
     * 修改用户信息
     *
     * @param userConfig 用户信息
     * @return 成功与否
     */
    @Log
    @PutMapping
    @CountControl(operationType = CountControlAspect.UPDATE)
    public R<?> updatePageParameters(@RequestBody UserConfig userConfig) {
        userConfig.setSortVersion(null); // 普通页面配置不允许覆盖搜索排序版本
        userConfigService.lambdaUpdate()
                .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                .update(userConfig);
        return R.success(userConfig);
    }

    /** @return 返回用户桌面背景图片 */
    @GetMapping("/getBg")
    public R<UserConfig> getBg() {
        return R.success(
                userConfigService.lambdaQuery()
                        .select(UserConfig::getBackgroundUrl)
                        .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                        .one()
        );
    }

}
