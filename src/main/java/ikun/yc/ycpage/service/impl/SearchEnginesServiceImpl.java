package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.mapper.SearchEnginesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ikun.yc.ycpage.service.SearchEnginesService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SearchEnginesServiceImpl extends ServiceImpl<SearchEnginesMapper, SearchEngines> implements SearchEnginesService {
    private final SearchEnginesMapper searchEnginesMapper;

    @Override
    public void batchUpdate(List<SearchEngines> searchEngineList) {
        searchEngineList.forEach(searchEngines -> searchEngines.setUserId(BaseContext.getCurrentId()));
        searchEnginesMapper.batchUpdateSearchEngines(searchEngineList);
    }
}