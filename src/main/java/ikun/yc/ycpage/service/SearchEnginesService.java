package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.SearchEngines;

import java.util.List;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
public interface SearchEnginesService extends IService<SearchEngines> {

    Integer batchUpdate(List<SearchEngines> searchEngineList);
}
