package ikun.yc.ycpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {

    /** 注册RESTful服务客户端 */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 创建拦截器
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            // 在这里添加所需的Header
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.139 Safari/537.36");
            // 继续执行请求
            return execution.execute(request, body);
        };

        // 将拦截器添加到RestTemplate
        restTemplate.getInterceptors().add(interceptor);

        return restTemplate;
    }
}