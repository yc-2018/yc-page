package ikun.yc.ycpage.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * <p>
 * MybatisPlusConfig
 * </p>
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 **/

@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 创建一个mybatisPlus拦截器，在里面添加分页功能----并交给了IOC容器管理
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        log.info("开始启动分页插件");
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); //按MYSQL语法分页
        return interceptor;


    }
}
