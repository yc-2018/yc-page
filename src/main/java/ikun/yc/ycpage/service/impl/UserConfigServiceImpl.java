package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.UserConfig;
import ikun.yc.ycpage.entity.enumeration.LinkType;
import ikun.yc.ycpage.mapper.UserConfigMapper;
import ikun.yc.ycpage.service.UserConfigService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author cgl
* description 针对表【user_config(用户配置)】的数据库操作Service实现
* createDate 2025-08-04 21:18:24
*/
@Service
public class UserConfigServiceImpl extends ServiceImpl<UserConfigMapper, UserConfig> implements UserConfigService {

    /**
     * 获取搜索引擎排序
     *
     * @param linkType 链接类型枚菌
     * @author 𝑐𝒽𝑒𝑛𝐺𝑢𝑎𝑛𝑔𝐿𝑜𝑛𝑔
     * @since 2025/08/06 21:53:06
     */
    @Override
    public String getSearchEngineSort(@Nullable LinkType linkType) {
        if (linkType == null) throw new ParamException("参数错误");
        UserConfig config = this.lambdaQuery()
                .select(linkType.getFieldMapper(), UserConfig::getId, UserConfig::getSortVersion)
                .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                .one(); // 注册时,每人都初始化了一条，搜索加上id才不会为null

        // 获取排序字段 格式 id/id/id
        return config.getXxxSort(linkType);
    }

    /** 获取搜索引擎排序版本号。 */
    @Override
    public Integer getSearchEngineSortVersion(@Nullable LinkType linkType) {
        if (linkType == null) throw new ParamException("参数错误");
        UserConfig config = this.lambdaQuery()
                .select(UserConfig::getSortVersion)
                .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                .one();
        return config == null || config.getSortVersion() == null ? 0 : config.getSortVersion();
    }

    /** 按版本更新搜索引擎排序。 */
    @Override
    public Integer updateSearchEngineSort(String userId, LinkType linkType, String sort, Integer version) {
        OptimisticLockUtils.requireVersion(version);
        boolean updated = this.lambdaUpdate()
                .eq(UserConfig::getUserId, userId)
                .eq(UserConfig::getSortVersion, version)
                .set(linkType.getFieldMapper(), sort)
                .setSql("sort_version = sort_version + 1")
                .update();
        OptimisticLockUtils.requireUpdated(updated);
        return version + 1;
    }


    /**
     * 【搜索引擎排序】从排序字符串中移除ID
     *
     * @param userId 用户ID
     * @param id 要移除的ID
     * @param linkType 链接类型枚菌
     */
    @Override
    public Integer removeIdFromSortString(String userId, Integer id, LinkType linkType, Integer version) {
        OptimisticLockUtils.requireVersion(version);
        // 1. 获取当前配置
        UserConfig config = this.lambdaQuery()
                .select(UserConfig::getId, UserConfig::getSearchSort, UserConfig::getLowSearchSort,
                        UserConfig::getHomeBookmarkSort, UserConfig::getSortVersion)
                .eq(UserConfig::getUserId, userId)
                .one();

        if (config == null) throw new ParamException("用户配置不存在");
        if (!Objects.equals(config.getSortVersion(), version)) {
            throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        }

        // 2. 获取排序字段并处理
        String sortField = config.getXxxSort(linkType) ;
        if (!StringUtils.hasText(sortField) || Arrays.stream(sortField.split("/")).noneMatch(id.toString()::equals)) {
            throw new ParamException("排序数据有误，请刷新后重试");
        }
        String newSortField = Arrays.stream(sortField.split("/"))
                .filter(s -> !s.equals(id.toString()))
                .collect(Collectors.joining("/")); // 移除目标ID后的排序
        return updateSearchEngineSort(userId, linkType, newSortField, version);
    }

    /**
     * 【搜索引擎排序】添加ID到排序字符串末尾
     *
     * @param userId 用户ID
     * @param id 要添加的ID
     * @param linkType 链接类型枚菌
     */
    @Override
    public Integer appendIdToSortString(String userId, Integer id, LinkType linkType, Integer version) {
        OptimisticLockUtils.requireVersion(version);
        // 1. 获取当前配置
        UserConfig config = this.lambdaQuery()
                .eq(UserConfig::getUserId, userId)
                .one();

        if (config == null) throw new ParamException("用户配置不存在");
        if (!Objects.equals(config.getSortVersion(), version)) {
            throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        }

        // 2. 获取当前排序字段
        String sortField = config.getXxxSort(linkType);
        String newSortField;

        if (StringUtils.hasText(sortField)) {
            // 追加到末尾
            newSortField = sortField + "/" + id;
        } else {
            // 创建新的排序字符串
            newSortField = id.toString();
        }

        // 3. 更新配置
        return updateSearchEngineSort(userId, linkType, newSortField, version);
    }

    /** 按版本移动搜索引擎分类，只递增一次全局排序版本。 */
    @Override
    public Integer moveIdBetweenSortStrings(String userId, Integer id, LinkType sourceType,
                                            LinkType targetType, Integer version) {
        OptimisticLockUtils.requireVersion(version);
        UserConfig config = this.lambdaQuery()
                .select(UserConfig::getSearchSort, UserConfig::getLowSearchSort,
                        UserConfig::getHomeBookmarkSort, UserConfig::getSortVersion)
                .eq(UserConfig::getUserId, userId)
                .one(); // 当前排序配置快照
        if (config == null) throw new ParamException("用户配置不存在");
        if (!Objects.equals(config.getSortVersion(), version)) {
            throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        }

        String sourceSort = config.getXxxSort(sourceType); // 原分类排序
        if (!StringUtils.hasText(sourceSort) || Arrays.stream(sourceSort.split("/")).noneMatch(id.toString()::equals)) {
            throw new ParamException("排序数据有误，请刷新后重试");
        }
        String newSourceSort = Arrays.stream(sourceSort.split("/"))
                .filter(value -> !value.equals(id.toString()))
                .collect(Collectors.joining("/")); // 移除后的原分类排序
        String targetSort = config.getXxxSort(targetType); // 目标分类排序
        String newTargetSort = StringUtils.hasText(targetSort) ? targetSort + "/" + id : id.toString();

        boolean updated = this.lambdaUpdate()
                .eq(UserConfig::getUserId, userId)
                .eq(UserConfig::getSortVersion, version)
                .set(sourceType.getFieldMapper(), newSourceSort)
                .set(targetType.getFieldMapper(), newTargetSort)
                .setSql("sort_version = sort_version + 1")
                .update(); // 两个分类排序作为一个 CAS 更新
        OptimisticLockUtils.requireUpdated(updated);
        return version + 1;
    }
}




