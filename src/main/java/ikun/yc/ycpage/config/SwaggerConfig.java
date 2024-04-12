package ikun.yc.ycpage.config;

import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * swagger3默认首页变成 <a href="http://localhost:8080/swagger-ui/index.html">swagger3</a>
 */
//@Configuration
public class SwaggerConfig {
  
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.OAS_30)
      .apiInfo(apiInfo())
      .select()
      .apis(RequestHandlerSelectors.basePackage("ikun.yc.ycpage.controller"))
      .paths(PathSelectors.any())
      .build()//;
      .securitySchemes(securitySchemes())
      .securityContexts(Collections.singletonList(securityContext()));
  }


  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
            .title("仰晨主页")
            .version("1.0")
            .description("仰晨主页接口文档")
            .build();
  }


  private List<SecurityScheme> securitySchemes() {
    List<SecurityScheme> apiKeyList = new ArrayList<>();
    //注意，这里应对应登录token鉴权对应的k-v
    apiKeyList.add(new ApiKey("Authorization", "Authorization", "header"));
    return apiKeyList;
  }

  /**
   * 这里设置 swagger2 认证的安全上下文
   */
  private SecurityContext securityContext() {
    return SecurityContext.builder()
            .securityReferences(Collections.singletonList(new SecurityReference("Authorization", scopes())))
            .build();
  }

  /**
   * 这里是写允许认证的scope
   */
  private AuthorizationScope[] scopes() {
    return new AuthorizationScope[]{
            new AuthorizationScope("web", "All scope is trusted!")
    };
  }


}
