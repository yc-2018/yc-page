package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.entity.PageParameters;
import ikun.yc.ycpage.service.PageParametersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


/**
 * 页面参数控制器
 *
 * @author ChenGuangLong
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/pageParameters")
public class PageParametersController {
    private final PageParametersService pageParametersService;

    /**
     * 修改用户页面配置信息
     * @param entity 用户页面信息
     * @return 成功与否
     */
    @Log
    @PutMapping
    public R<?> updatePageParameters(@RequestBody PageParameters entity) {
        LambdaUpdateWrapper<PageParameters> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PageParameters::getUserId, BaseContext.getCurrentId());
        boolean updateSuccess = pageParametersService.update(entity, wrapper);
        return updateSuccess? R.success(true): R.error("保存失败");
    }

    /** @return 返回用户页面配置信息 */
    @GetMapping
    public R<PageParameters> getPageParameters() {
        return R.success( pageParametersService
                .getOne(new LambdaUpdateWrapper<PageParameters>()
                        .eq(PageParameters::getUserId, BaseContext.getCurrentId())));
    }
}