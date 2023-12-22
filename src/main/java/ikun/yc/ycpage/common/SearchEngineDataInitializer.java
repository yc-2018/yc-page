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
  private final ResourceLoader resourceLoader;

  public List<SearchEngines> getInitialSearchEngines(String userId) throws Exception {
    Resource resource = resourceLoader.getResource("classpath:initial-engines.json");
    ObjectMapper mapper = new ObjectMapper();
    List<SearchEngines> engines = mapper.readValue(resource.getInputStream(), new TypeReference<List<SearchEngines>>() {});
    // 为每个搜索引擎设置 userId
    engines.forEach(engine -> engine.setUserId(userId));
    return engines;
  }
}
