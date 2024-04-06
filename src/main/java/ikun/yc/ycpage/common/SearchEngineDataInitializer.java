package ikun.yc.ycpage.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.entity.SearchEngines;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchEngineDataInitializer {
  private final ResourceLoader resourceLoader;  // 资源加载器

  /**
   * 给新用户加上一堆初始搜索引擎
   *
   * @param userId 新用户id
   * @author ChenGuangLong
   * @since 2023/12/22
   */
  public List<SearchEngines> getInitialSearchEngines(String userId) throws Exception {
    Resource resource = resourceLoader.getResource("classpath:initial-engines.json");
    ObjectMapper mapper = new ObjectMapper();
    List<SearchEngines> engines = mapper.readValue(resource.getInputStream(), new TypeReference<List<SearchEngines>>() {});
    // 为每个搜索引擎设置 userId
    engines.forEach(engine -> engine.setUserId(userId));
    return engines;
  }
}
