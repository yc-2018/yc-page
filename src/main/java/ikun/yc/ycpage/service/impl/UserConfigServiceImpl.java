package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.UserConfig;
import ikun.yc.ycpage.entity.enumeration.LinkType;
import ikun.yc.ycpage.mapper.UserConfigMapper;
import ikun.yc.ycpage.service.UserConfigService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
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
                .select(linkType.getFieldMapper(), UserConfig::getId)
                .eq(UserConfig::getUserId, BaseContext.getCurrentId())
                .one(); // 注册时,每人都初始化了一条，搜索加上id才不会为null

        // 获取排序字段 格式 id/id/id
        return config.getXxxSort(linkType);
    }


    /**
     * 【搜索引擎排序】从排序字符串中移除ID
     *
     * @param userId 用户ID
     * @param id 要移除的ID
     * @param linkType 链接类型枚菌
     */
    @Override
    public void removeIdFromSortString(String userId, Integer id, LinkType linkType) {
        // 1. 获取当前配置
        UserConfig config = this.lambdaQuery()
                .select(UserConfig::getId, UserConfig::getSearchSort, UserConfig::getLowSearchSort)
                .eq(UserConfig::getUserId, userId)
                .one();

        if (config == null) return;

        // 2. 获取排序字段并处理
        String sortField = config.getXxxSort(linkType) ;
        if (StringUtils.hasText(sortField)) {
            // 移除目标ID并重新拼接
            String newSortField = Arrays.stream(sortField.split("/"))
                    .filter(s -> !s.equals(id.toString()))
                    .collect(Collectors.joining("/"));

            // 3. 更新配置
            config.setXxxSort(linkType, newSortField);
            this.updateById(config);
        }
    }

    /**
     * 【搜索引擎排序】添加ID到排序字符串末尾
     *
     * @param userId 用户ID
     * @param id 要添加的ID
     * @param linkType 链接类型枚菌
     */
    @Override
    public void appendIdToSortString(String userId, Integer id, LinkType linkType) {
        // 1. 获取当前配置
        UserConfig config = this.lambdaQuery()
                .eq(UserConfig::getUserId, userId)
                .one();

        if (config == null) return;

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
        config.setXxxSort(linkType, newSortField);

        this.updateById(config);
    }
}




