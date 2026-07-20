package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.DelCache;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.anno.RedisCache;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.entity.enumeration.LinkType;
import ikun.yc.ycpage.service.SearchEnginesService;
import ikun.yc.ycpage.service.UserConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
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
     * @param linkType 获取这个类型列表
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/06 20:00:06
     */
    @RedisCache
    @GetMapping
    public R<List<SearchEngines>> getList(@RequestParam(defaultValue = "0") LinkType linkType) {
        // 1. 查询基础数据
        List<SearchEngines> enginesList = searchEnginesService.lambdaQuery()
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                .eq(SearchEngines::getType, linkType.getCode())
                .list();

        Integer sortVersion = userConfigService.getSearchEngineSortVersion(linkType);
        enginesList.forEach(engine -> engine.setSortVersion(sortVersion));

        if (enginesList.isEmpty()) return R.success(enginesList);
        // 2. 获取排序数据
        // 获取排序字段 格式 id/id/id
        String sortField = userConfigService.getSearchEngineSort(linkType);

        if (!StringUtils.hasText(sortField)) return R.success(enginesList);

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

    /** 获取搜索引擎排序版本，空列表新增时也需要携带。 */
    @GetMapping("/sortVersion")
    public R<Integer> getSortVersion() {
        return R.success(userConfigService.getSearchEngineSortVersion(LinkType.SEARCH));
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
    @DelCache    // 清除缓存
    @PostMapping
    @Transactional
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<SearchEngines> addSearchEngines(@RequestBody @Valid SearchEngines searchEngines) {
        OptimisticLockUtils.requireVersion(searchEngines.getSortVersion());
        searchEngines.setUserId(BaseContext.getCurrentId());
        boolean saveSuccess = searchEngines.insert();   // 插入,实体类继承Model的正确用法

        if (!saveSuccess) return R.error("添加失败！");

        Integer nextSortVersion = userConfigService.appendIdToSortString(
                BaseContext.getCurrentId(), searchEngines.getId(), searchEngines.getType(), searchEngines.getSortVersion());
        searchEngines.setSortVersion(nextSortVersion);

        return R.success(searchEngines);
    }

    /**
     * 编辑搜索引擎
     *
     * @param updatedEngine 更新后的搜索引擎对象
     * @return 更新后的搜索引擎
     */
    @Log
    @DelCache    // 清除缓存
    @PutMapping
    @Transactional
    public R<SearchEngines> updateSearchEngines(@RequestBody @Valid SearchEngines updatedEngine) {
        // 1. 获取当前用户ID
        String userId = BaseContext.getCurrentId();

        // 2. 获取原始记录
        SearchEngines originalEngine = searchEnginesService.getById(updatedEngine.getId());
        if (originalEngine == null || !originalEngine.getUserId().equals(userId)) {
            return R.error("搜索引擎不存在或无权操作");
        }

        // 3. 检查分类是否改变
        boolean categoryChanged = !Objects.equals(originalEngine.getType(), updatedEngine.getType());

        // 4. 更新记录
        updatedEngine.setUserId(userId); // 确保用户ID不被修改
        OptimisticLockUtils.requireVersion(updatedEngine.getVersion());
        boolean updateSuccess = searchEnginesService.updateById(updatedEngine);
        OptimisticLockUtils.requireUpdated(updateSuccess);

        // 5. 处理分类转换的排序逻辑（常用/不常用转换）
        if (updatedEngine.getType() != null && categoryChanged) {
            Integer nextSortVersion = userConfigService.moveIdBetweenSortStrings(
                    userId, updatedEngine.getId(), originalEngine.getType(), updatedEngine.getType(),
                    updatedEngine.getSortVersion()); // 分类移动和排序字段修改使用同一次 CAS
            updatedEngine.setSortVersion(nextSortVersion);
        } else {
            updatedEngine.setSortVersion(userConfigService.getSearchEngineSortVersion(originalEngine.getType()));
        }
        return R.success(updatedEngine);
    }

    /**
     * 删除搜索引擎
     *
     * @param id 主键
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/07 00:23:07
     */
    @Log
    @DelCache    // 删除缓存
    @Transactional
    @DeleteMapping("/{id}")
    public R<Boolean> deleteSearchEngines(@PathVariable Integer id, @RequestParam Integer version,
                                          @RequestParam Integer sortVersion) {
        // 1. 查询要删除的搜索引擎
        SearchEngines engine = searchEnginesService.getById(id);
        if (engine == null || !engine.getUserId().equals(BaseContext.getCurrentId())) {
            return R.error("搜索引擎不存在或无权操作");
        }

        // 2. 删除记录
        OptimisticLockUtils.requireVersion(version);
        OptimisticLockUtils.requireVersion(sortVersion);
        if (!Objects.equals(engine.getVersion(), version)) throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        boolean deleteSuccess = searchEnginesService.remove(Wrappers.<SearchEngines>lambdaUpdate()
                .eq(SearchEngines::getId, id)
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                .eq(SearchEngines::getVersion, version));
        OptimisticLockUtils.requireUpdated(deleteSuccess);

        // 3. 从用户配置的排序字符串中移除该ID
        userConfigService.removeIdFromSortString(
                BaseContext.getCurrentId(), id, engine.getType(), sortVersion);

        return R.success(true);
    }

    /**
     * 排序搜索引擎
     *
     * @param sort      排序
     * @param linkType   类型
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/10 21:29:10
     */
    @Log
    @DelCache    // 删除缓存
    @PostMapping("/sort")
    public R<Boolean> sortSearchEngines(
            @Pattern(regexp = "(\\d+)(/\\d+)*", message = "排序参数格式有误") String sort,
            @RequestParam(defaultValue = "0") LinkType linkType,
            @RequestParam Integer sortVersion
    ) {
        List<String> searchIds = searchEnginesService.lambdaQuery()
                .select(SearchEngines::getId)
                .eq(SearchEngines::getUserId, BaseContext.getCurrentId())
                .eq(SearchEngines::getType, linkType)
                .list()
                .stream()
                .map(se -> se.getId().toString())
                .collect(Collectors.toList());

        // 如果 ID 集合完全一致且没有重复，才允许重写排序字段
        List<String> sortIds = Arrays.asList(sort.split("/"));
        OptimisticLockUtils.requireSameIds(searchIds, sortIds);

        userConfigService.updateSearchEngineSort(BaseContext.getCurrentId(), linkType, sort, sortVersion);
        return R.success(true);
    }
}
