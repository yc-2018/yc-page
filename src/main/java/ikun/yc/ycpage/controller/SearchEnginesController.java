package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.service.SearchEnginesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param type 类型（可选）
     * @return 搜索引擎列表
     * @author ChenGuangLong
     * @since 2023/12/23 16:53:55
     */
    @GetMapping("/list")
    public R<List<SearchEngines>> getListByUserId(@RequestParam(required = false) Integer type) {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", BaseContext.getCurrentId());
        if (type!= null) map.put("is_quick_search", type);

        return R.success(searchEnginesService.listByMap(map));
    }


    /**
     * 添加搜索引擎
     *
     * @param searchEngines 搜索引擎
     * @return 成功与否
     * @author ChenGuangLong
     * @since 2023/12/23 16:53:03
     */
    @Log
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, controlFrequency = 10)
    public R<?> addSearchEngines(@RequestBody SearchEngines searchEngines) {
        if (searchEngines.getEngineUrl()== null) return R.error("URL不允许为空");
        if (searchEngines.getName()== null) return R.error("名称不允许为空");
        if (searchEngines.getIsQuickSearch()== null) return R.error("引擎类型不允许为空");

        searchEngines.setUserId(BaseContext.getCurrentId());
        boolean saveSuccess = searchEnginesService.save(searchEngines);
        return R.success(saveSuccess?searchEngines.getId():null);
    }


    /**
     * 批量更新搜索引擎
     *
     * @param searchEngineList 搜索引擎列表
     * @return 返回拼接的循环进去的true或false
     * @author ChenGuangLong
     * @since 2023/12/23
     */
    @Log
    @PutMapping
    @CountControl(operationType = CountControlAspect.UPDATE)
    public R<?> updateSearchEngines(@RequestBody List<SearchEngines> searchEngineList) {
        if (searchEngineList == null || searchEngineList.isEmpty()) return R.error("乱搞！🤺");

        searchEnginesService.batchUpdate(searchEngineList);
        return R.success(true);
    }


    /**
     * 批量删除搜索引擎
     *
     * @param ids 引擎id
     * @return 成功返回true，失败返回提示信息
     * @author ChenGuangLong
     * @since 2023/12/23
     */
    @Log
    @DeleteMapping
    public R<Boolean> deleteSearchEngines(@RequestBody List<Integer> ids) {
        LambdaQueryWrapper<SearchEngines> wrapper = new LambdaQueryWrapper<>();
        wrapper .in(SearchEngines::getId, ids)
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId());

        return searchEnginesService.remove(wrapper)? R.success(true) : R.error("删除失败");
    }

}