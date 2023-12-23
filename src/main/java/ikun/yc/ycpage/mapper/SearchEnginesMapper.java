package ikun.yc.ycpage.mapper;

import ikun.yc.ycpage.entity.SearchEngines;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * (search_engines)数据Mapper
 *
 * @author yc
 * @since 2023-12-03 22:31:22
*/
@Mapper
public interface SearchEnginesMapper extends BaseMapper<SearchEngines> {
    void batchUpdateSearchEngines(List<SearchEngines> searchEngineList);

}
