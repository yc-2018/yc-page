package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.PageParameters;
import ikun.yc.ycpage.service.PageParametersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 服务控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/pageParameters")
public class PageParametersController {
    private final PageParametersService pageParametersService;

    @PutMapping
    public R<?> updatePageParameters(@RequestBody PageParameters entity) {
        LambdaUpdateWrapper<PageParameters> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PageParameters::getUserId, BaseContext.getCurrentId());
        boolean updateSuccess = pageParametersService.update(entity, wrapper);
        return updateSuccess? R.success(true): R.error("保存失败");
    }

}