package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.UserConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.enumeration.LinkType;

/**
* @author cgl
* description 针对表【user_config(用户配置)】的数据库操作Service
* createDate 2025-08-04 21:18:24
*/
public interface UserConfigService extends IService<UserConfig> {

    String getSearchEngineSort(LinkType linkType);

    /** 获取搜索引擎排序版本号。 */
    Integer getSearchEngineSortVersion(LinkType linkType);

    /** 按版本更新搜索引擎排序。 */
    Integer updateSearchEngineSort(String userId, LinkType linkType, String sort, Integer version);

    /** 按版本从排序字符串移除搜索引擎，并返回新版本。 */
    Integer removeIdFromSortString(String userId, Integer id, LinkType linkType, Integer version);

    /** 按版本把搜索引擎追加到排序末尾，并返回新版本。 */
    Integer appendIdToSortString(String userId, Integer id, LinkType linkType, Integer version);

    /** 按版本把搜索引擎从一个分类移动到另一个分类，并返回新版本。 */
    Integer moveIdBetweenSortStrings(String userId, Integer id, LinkType sourceType, LinkType targetType, Integer version);
}
