package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.SearchEngines;

import java.util.List;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 * @description 由 Mybatisplus Code Generator 创建
 */
public interface SearchEnginesService extends IService<SearchEngines> {

    void batchUpdate(List<SearchEngines> searchEngineList);
}
