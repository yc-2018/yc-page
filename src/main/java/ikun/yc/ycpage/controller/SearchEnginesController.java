package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CacheEvict;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.anno.RedisCache;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.entity.UserConfig;
import ikun.yc.ycpage.service.SearchEnginesService;
import ikun.yc.ycpage.service.UserConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final UserConfigService userConfigService;

    /**
     * 获取列表
     *
     * @param isLowUsage 是否获取的是不常用列表
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/06 20:00:06
     */
    @GetMapping
    @RedisCache("searchEngines")
    public R<List<SearchEngines>> getList(@RequestParam(defaultValue = "false") Boolean isLowUsage) {
        // 1. 查询基础数据
        List<SearchEngines> enginesList = searchEnginesService.lambdaQuery()
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                .eq(SearchEngines::getLowUsage, isLowUsage ? 1 : 0)
                .list();

        if (enginesList.isEmpty()) return R.success(enginesList);
        // 2. 获取排序数据
        // 获取排序字段 格式 id/id/id
        String sortField = userConfigService.getSearchEngineSort(isLowUsage);

        if (!StringUtils.hasText(sortField))
            return R.success(enginesList);
//        String[] sortIdArr = sortField.split("/");
//        List<SearchEngines> sortEnginesList = new ArrayList<>(sortIdArr.length);
//        for (String sortId : sortIdArr) {
//            Optional<SearchEngines> first = enginesList.stream()
//                    .filter(engine -> engine.getId().equals(Integer.parseInt(sortId)))
//                    .findFirst();
//            if (first.isPresent()) {
//                sortEnginesList.add(first.get());
//                enginesList.remove(first.get());
//            }
//        }
//        enginesList.addAll(sortEnginesList);

        // 3. 使用Map优化查找性能 O(1)
        Map<Integer, SearchEngines> engineMap = enginesList.stream()
                .collect(Collectors.toMap(SearchEngines::getId, Function.identity()));

        // 4. 按排序规则构建新列表
        List<SearchEngines> sortedList = new ArrayList<>(enginesList.size());
        Arrays.stream(sortField.split("/"))
                .map(Integer::parseInt)
                .filter(engineMap::containsKey)     // 过滤不存在的ID
                .forEach(id -> {
                    sortedList.add(engineMap.get(id));
                    engineMap.remove(id);        // 移除以避免重复添加
                });

        // 5. 添加未排序的剩余项（保持原始顺序）
        engineMap.forEach((id, engine) -> sortedList.add(engine));

        return R.success(sortedList);
    }

//    /**
//     * 按用户id获取列表
//     *
//     * @param type 类型（可选）
//     * @return 搜索引擎列表
//     * @author ChenGuangLong
//     * @since 2023/12/23 16:53:55
//     */
//    @GetMapping("/list")
//    @RedisCache("searchEngines") // 缓存 默认一天
//    public R<List<SearchEngines>> getListByUserId(@RequestParam(required = false) Integer type) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("user_id", BaseContext.getCurrentId());
//        if (type!= null) map.put("is_quick_search", type);
//
//        return R.success(searchEnginesService.listByMap(map));
//    }


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
    @CacheEvict("searchEngines")    // 删除缓存
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<SearchEngines> addSearchEngines(@RequestBody @Valid SearchEngines searchEngines) {
        searchEngines.setUserId(BaseContext.getCurrentId());
        boolean saveSuccess = searchEngines.insert();   // 插入,实体类继承Model的正确用法

        if (!saveSuccess) return R.error("添加失败！");

        // 2. 获取排序数据
        boolean isLowUsage = !Objects.equals(searchEngines.getLowUsage(), 0);
        String sortField = userConfigService.getSearchEngineSort(isLowUsage);
        // 检查排序字段和全部id对应
        List<SearchEngines> sqlEngines = searchEnginesService.lambdaQuery()
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                .eq(SearchEngines::getLowUsage, isLowUsage ? 1 : 0)
                .list();
        if (!sqlEngines.isEmpty()) {
            List<Integer> allIds = sqlEngines.stream().map(SearchEngines::getId).collect(Collectors.toList());
            if (StringUtils.hasText(sortField)) {
                List<Integer> sortIds = Arrays.stream(sortField.split("/"))
                        .map(Integer::parseInt)
                        .filter(v-> !allIds.contains(v))
                        .collect(Collectors.toList());
                if (!sortIds.isEmpty()) {
                    // 在sortField后面加上/ids
                    String noSort = sortIds.stream().map(Object::toString).collect(Collectors.joining("/"));
                    sortField = sortField + "/" + noSort;
                }
            } else sortField = allIds.stream().map(Object::toString).collect(Collectors.joining("/"));
        }

        if (StringUtils.hasText(sortField)) { // 用/分割  在最后增加id 并用 / 链接
            sortField = sortField + "/" + searchEngines.getId();
        }else {
            sortField = searchEngines.getId().toString();
        }
        userConfigService.lambdaUpdate()
                .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                .set(!isLowUsage, UserConfig::getSearchSort, sortField)
                .set(isLowUsage, UserConfig::getLowSearchSort, sortField)
                .update();

        return R.success(searchEngines);
    }


//    /**
//     * 批量更新搜索引擎
//     *
//     * @param searchEngineList 搜索引擎列表
//     * @return 返回拼接的循环进去的true或false
//     * @author ChenGuangLong
//     * @since 2023/12/23
//     */
//    @Log
//    @PutMapping
//    @CacheEvict("searchEngines")    // 删除缓存
//    @CountControl(operationType = CountControlAspect.UPDATE)
//    public R<?> updateSearchEngines(@RequestBody List<SearchEngines> searchEngineList) {
//        if (searchEngineList == null || searchEngineList.isEmpty()) return R.error("乱搞！🤺");
//
//        Integer i = searchEnginesService.batchUpdate(searchEngineList);
//        if (i == 0) return R.error("更新失败！");
//        return R.success(true);
//    }

    /**
     * 编辑搜索引擎
     *
     * @param updatedEngine 更新后的搜索引擎对象
     * @return 更新后的搜索引擎
     */
    @Log
    @PutMapping
    @CacheEvict("searchEngines")    // 删除缓存
    public R<SearchEngines> updateSearchEngines(@RequestBody @Valid SearchEngines updatedEngine) {
        // 1. 获取当前用户ID
        String userId = BaseContext.getCurrentId();

        // 2. 获取原始记录
        SearchEngines originalEngine = searchEnginesService.getById(updatedEngine.getId());
        if (originalEngine == null || !originalEngine.getUserId().equals(userId)) {
            return R.error("搜索引擎不存在或无权操作");
        }

        // 3. 检查分类是否改变
        boolean categoryChanged = !Objects.equals(originalEngine.getLowUsage(), updatedEngine.getLowUsage());

        // 4. 更新记录
        updatedEngine.setUserId(userId); // 确保用户ID不被修改
        boolean updateSuccess = searchEnginesService.updateById(updatedEngine);
        if (!updateSuccess) return R.error("更新失败！");

        // 5. 处理分类转换的排序逻辑（常用/不常用转换）
        if (updatedEngine.getLowUsage() != null && categoryChanged) {
            // 从原分类排序中移除
            userConfigService.removeIdFromSortString(userId, updatedEngine.getId(), originalEngine.getLowUsage() == 1);

            // 添加到新分类排序末尾
            userConfigService.appendIdToSortString(userId, updatedEngine.getId(), updatedEngine.getLowUsage() == 1);
        }
        return R.success(updatedEngine);
    }



//
//
//    /**
//     * 批量删除搜索引擎
//     *
//     * @param ids 引擎id
//     * @return 成功返回true，失败返回提示信息
//     * @author ChenGuangLong
//     * @since 2023/12/23
//     */
//    @Log
//    @DeleteMapping
//    @CacheEvict("searchEngines")    // 删除缓存
//    public R<Boolean> deleteSearchEngines(@RequestBody List<Integer> ids) {
//        return searchEnginesService.lambdaUpdate()
//            .in(SearchEngines::getId, ids)
//            .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
//            .remove()? R.success(true) : R.error("删除失败");
//    }


    /**
     * 删除搜索引擎
     *
     * @param id 主键
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/07 00:23:07
     */
    @Log
    @DeleteMapping("/{id}")
    @CacheEvict("searchEngines")    // 删除缓存
    public R<Boolean> deleteSearchEngines(@PathVariable Integer id) {
        // 1. 查询要删除的搜索引擎
        SearchEngines engine = searchEnginesService.getById(id);
        if (engine == null || !engine.getUserId().equals(BaseContext.getCurrentId())) {
            return R.error("搜索引擎不存在或无权操作");
        }

        // 2. 删除记录
        boolean deleteSuccess = searchEnginesService.removeById(id);
        if (!deleteSuccess) return R.error("删除失败！");

        // 3. 从用户配置的排序字符串中移除该ID
        boolean isLowUsage = engine.getLowUsage() != 0;
        String sortField = userConfigService.getSearchEngineSort(isLowUsage);
        if (StringUtils.hasText(sortField)) {
            // 移除目标ID并重新拼接
            String newSortField = Arrays.stream(sortField.split("/"))
                    .filter(s -> !s.equals(id.toString()))
                    .collect(Collectors.joining("/"));

            // 3. 更新配置
            userConfigService.lambdaUpdate()
                    .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                    .set(!isLowUsage, UserConfig::getSearchSort, newSortField)
                    .set(isLowUsage, UserConfig::getLowSearchSort, newSortField)
                    .update();
        }

        return R.success(true);
    }
}