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

    /**
     * 按用户id获取列表
     *
     * @return 搜索引擎列表
     * @author ChenGuangLong
     * @since 2023/12/23 16:53:55
     */
    @GetMapping("/list")
    public R<List<SearchEngines>> getListByUserId() {
        return R.success(searchEnginesService.listByMap(Collections.singletonMap("user_id", BaseContext.getCurrentId())));
    }


    /**
     * 添加搜索引擎
     *
     * @param searchEngines 搜索引擎
     * @return 成功与否
     * @author ChenGuangLong
     * @since 2023/12/23 16:53:03
     */
    @PostMapping
    public R<Boolean> addSearchEngines(@RequestBody SearchEngines searchEngines) {
        if (searchEngines.getEngineUrl()== null) return R.error("URL不允许为空");
        if (searchEngines.getName()== null) return R.error("名称不允许为空");

        searchEngines.setUserId(BaseContext.getCurrentId());
        return R.success(searchEnginesService.save(searchEngines));
    }


    /**
     * 批量循环更新搜索引擎
     *
     * @param searchEngineList 搜索引擎列表
     * @return 返回拼接的循环进去的true或false
     * @author ChenGuangLong
     * @since 2023/12/23
     */
    @PutMapping
    public R<?> updateSearchEngines(@RequestBody List<SearchEngines> searchEngineList) {
        if (searchEngineList == null || searchEngineList.size() == 0) return R.error("乱搞！🤺");

        StringBuilder sb = new StringBuilder();
        for (SearchEngines searchEngines : searchEngineList) {
            sb.append( searchEnginesService.update(new LambdaUpdateWrapper<SearchEngines>()
                    .eq(SearchEngines::getId, searchEngines.getId())
                    .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                    .set(searchEngines.getEngineUrl() != null, SearchEngines::getEngineUrl, searchEngines.getEngineUrl())
                    .set(searchEngines.getName() != null, SearchEngines::getName, searchEngines.getName())
                    .set(searchEngines.getIconUrl() != null, SearchEngines::getIconUrl, searchEngines.getIconUrl())
                    .set(searchEngines.getIsQuickSearch() != null, SearchEngines::getIsQuickSearch, searchEngines.getIsQuickSearch())
            ));
        }
        return R.success(sb.toString());
    }


    /**
     * 批量删除搜索引擎
     *
     * @param ids 引擎id
     * @return 成功返回true，失败返回提示信息
     * @author ChenGuangLong
     * @since 2023/12/23
     */
    @DeleteMapping
    public R<Boolean> deleteSearchEngines(@RequestBody List<Integer> ids) {
        LambdaQueryWrapper<SearchEngines> wrapper = new LambdaQueryWrapper<>();
        wrapper .in(SearchEngines::getId, ids)
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId());

        return searchEnginesService.remove(wrapper)? R.success(true) : R.error("删除失败");
    }

}