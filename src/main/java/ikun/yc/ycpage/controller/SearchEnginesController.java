package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.SearchEngineDataInitializer;
import ikun.yc.ycpage.service.SearchEnginesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 服务控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/searchEngines")
public class SearchEnginesController {
    private final SearchEngineDataInitializer searchEngineDataInitializer;

    @GetMapping
    public R<?> get() throws Exception {
        return R.success(searchEngineDataInitializer.getInitialSearchEngines(BaseContext.getCurrentId()));
    }

}