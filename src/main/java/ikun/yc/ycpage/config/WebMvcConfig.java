//仰晨study 创建时间2023/12/4 0:06 星期一
package ikun.yc.ycpage.config;

import ikun.yc.ycpage.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 继承 WebMvcConfigurationSupport 时，Spring Boot的自动配置特性会被禁用，包括静态资源处理。
 * 这是为什么您发现 addResourceHandlers 方法在继承 WebMvcConfigurationSupport 时无效的原因。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)       //注册拦截器
                .addPathPatterns("/**")                     //拦截全部路径    /*是一级路径/**是全部路径
                .excludePathPatterns(
                        "/users/login",
                        "/wechat",
                        "/doc.html",                // 放行Swagger
                        "/docs.html",               // 放行Swagger
                        "/swagger-ui.html",         // 放行Swagger
                        "/index.html",              // 放行Swagger
                        "/v2/api-docs/**",          // 放行Swagger
                        "/swagger-resources/**",    // 放行Swagger
                        "/swagger-ui/**",           // 放行Swagger UI静态资源
                        "/v3/api-docs/**",          // 放行Swagger新API文档路径
                        "/webjars/**",              // 放行webjars路径下的资源
                        "/css/**",                  // 放行css静态资源
                        "/js/**",                   // 放行js静态资源
                        "/images/**"                // 放行images静态资源
                );  // 排除 (不拦截的路径)
    }




}
