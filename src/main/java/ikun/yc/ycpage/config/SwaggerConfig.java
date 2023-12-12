package ikun.yc.ycpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * swagger3默认首页变成 <a href="http://localhost:8080/swagger-ui/index.html">swagger3</a>
 */
@Configuration
public class SwaggerConfig {
  
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
      .apiInfo(apiInfo())
      .select()
      .apis(RequestHandlerSelectors.basePackage("ikun.yc.ycpage.controller"))
      .paths(PathSelectors.any())
      .build();
  }


  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
            .title("仰晨主页")
            .version("1.0")
            .description("仰晨主页接口文档")
            .build();
  }

}
