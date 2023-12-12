package ikun.yc.ycpage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  /**
   *  统一LocalDateTime的序列化方式
   *   不想全局也可使用@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   *                private LocalDateTime createTime;
   *  JavaTimeModule，这个模块是处理Java 8日期和时间API的专用模块。
   *  WRITE_DATES_AS_TIMESTAMPS设置为false是为了避免将日期时间序列化为时间戳（即数组格式"updateTime":[2023,12,6,16,51,8]）。
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }
}
