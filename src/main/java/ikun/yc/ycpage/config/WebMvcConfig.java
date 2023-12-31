//仰晨study 创建时间2023/12/4 0:06 星期一
package ikun.yc.ycpage.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import ikun.yc.ycpage.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 继承 WebMvcConfigurationSupport 时，Spring Boot的自动配置特性会被禁用，包括静态资源处理。
 * 这是为什么您发现 addResourceHandlers 方法在继承 WebMvcConfigurationSupport 时无效的原因。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)       //注册拦截器
                .addPathPatterns("/**")                     //拦截全部路径    /*是一级路径/**是全部路径
                .excludePathPatterns(
                        "/users/login",
                        "/ikun说不能让人猜到微信接口",
                        "/favicon.ico",
                        "/doc.html",                // 放行Swagger
                        "/docs.html",               // 放行Swagger
                        "/swagger-ui.html",         // 放行Swagger
                        "/index.html",              // 放行Swagger
                        "/v2/api-docs/**",          // 放行Swagger
                        "/swagger-resources/**",    // 放行Swagger
                        "/noAuth/**",               // 放行Swagger
                        "/swagger-ui/**",           // 放行Swagger UI静态资源
                        "/v3/**",                   // 放行Swagger新API文档路径
                        "/webjars/**",              // 放行webjars路径下的资源
                        "/css/**",                  // 放行css静态资源
                        "/js/**",                   // 放行js静态资源
                        "/images/**"                // 放行images静态资源
                );  // 排除 (不拦截的路径)
    }


    /**
     * 定义全局默认时间序列化
     * 配置完后，格式如 "createTime": "2023-12-05T20:43:20",
     * 没配置时，格式如 "createTime": [2023,12,11,9,25,12]
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .indentOutput(true)
                .dateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .simpleDateFormat(DATE_TIME_FORMAT)
                .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                .serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)))
                .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                .deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)))
                .modulesToInstall(new ParameterNamesModule());
        converters.add(new MappingJackson2HttpMessageConverter(builder.build()));
    }


}
