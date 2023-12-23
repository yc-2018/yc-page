package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.service.SearchEnginesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 《搜索引擎》服务控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/searchEngines")
public class SearchEnginesController {
    private final SearchEnginesService searchEnginesService;

    @GetMapping("/list")
    public R<List<SearchEngines>> getListByUserId() {
        return R.success(searchEnginesService.listByMap(Collections.singletonMap("user_id", BaseContext.getCurrentId())));
    }

    @PostMapping
    public R<Boolean> addSearchEngines(@RequestBody SearchEngines searchEngines) {
        if (searchEngines.getEngineUrl()== null) return R.error("URL不允许为空");
        if (searchEngines.getName()== null) return R.error("名称不允许为空");

        searchEngines.setUserId(BaseContext.getCurrentId());
        return R.success(searchEnginesService.save(searchEngines));
    }

    @PutMapping
    public R<Boolean> updateSearchEngines(@RequestBody List<SearchEngines> searchEngineList) {
        if (searchEngineList == null) return R.error("ID不允许为空");

        LambdaUpdateWrapper<SearchEngines> wrapper = new LambdaUpdateWrapper<>();
        for (SearchEngines searchEngines : searchEngineList) {
            wrapper .or()
                    .eq(SearchEngines::getId, searchEngines.getId())
                    .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                    .set(searchEngines.getEngineUrl() != null, SearchEngines::getEngineUrl, searchEngines.getEngineUrl())
                    .set(searchEngines.getName() != null, SearchEngines::getName, searchEngines.getName())
                    .set(searchEngines.getIconUrl() != null, SearchEngines::getIconUrl, searchEngines.getIconUrl())
                    .set(searchEngines.getIsQuickSearch() != null, SearchEngines::getIsQuickSearch, searchEngines.getIsQuickSearch());
        }
        return R.success(searchEnginesService.update(wrapper));
    }

    @DeleteMapping
    public R<Boolean> deleteSearchEngines(@RequestBody List<Integer> ids) {
        LambdaQueryWrapper<SearchEngines> wrapper = new LambdaQueryWrapper<>();
        wrapper .in(SearchEngines::getId, ids)
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId());

        return searchEnginesService.remove(wrapper)? R.success(true) : R.error("删除失败");
    }

}