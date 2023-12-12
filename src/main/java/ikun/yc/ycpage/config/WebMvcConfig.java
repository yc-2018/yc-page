//仰晨study 创建时间2023/12/4 0:06 星期一
package ikun.yc.ycpage.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import ikun.yc.ycpage.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 继承 WebMvcConfigurationSupport 时，Spring Boot的自动配置特性会被禁用，包括静态资源处理。
 * 这是为什么您发现 addResourceHandlers 方法在继承 WebMvcConfigurationSupport 时无效的原因。
 */
@Slf4j
@Configuration
@EnableSwagger2
@EnableKnife4j
@RequiredArgsConstructor
public class WebMvcConfig extends WebMvcConfigurationSupport/* implements WebMvcConfigurer*/ {
    private final LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)       //注册拦截器
                .addPathPatterns("/**")                     //拦截全部路径    /*是一级路径/**是全部路径
                .excludePathPatterns("/users/login", "/wechat");  // 排除 /login 和 /wx
    }


    /**
     * 设置静态资源映射
     * */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始静态资源映射:接口文档地址请访问http://localhost:8080/doc.html");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
    }

    @Bean
    public Docket createRestApi() {
        //文档类型
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
