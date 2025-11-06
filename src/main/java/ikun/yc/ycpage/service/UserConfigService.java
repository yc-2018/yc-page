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

    void removeIdFromSortString(String userId, Integer id, LinkType linkType);

    void appendIdToSortString(String userId, Integer id, LinkType linkType);
}
