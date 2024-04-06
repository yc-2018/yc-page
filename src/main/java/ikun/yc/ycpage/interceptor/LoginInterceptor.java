//仰晨study 创建时间2023/4/24 1:45 星期一
package ikun.yc.ycpage.interceptor;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.utils.JwtUtils;
import ikun.yc.ycpage.common.exception.LoginException;
import ikun.yc.ycpage.common.exception.PathException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override   //目标资源方法运行前运行，返回true: 放行，放回false，不放行
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        System.out.println("preHandle  目标资源方法运行前运行");

        // 检查是否是OPTIONS请求(跨域)
//        if (req.getMethod().equals(HttpMethod.OPTIONS.name())) {
//            return true; // 允许OPTIONS请求通过
//        }

        String uri = req.getRequestURI();
        log.info("请求路径：{}", uri);
        // error路径处理
        if (uri.equals("/error")) throw new PathException("请求失败");
        // 对 /login 和 /wechat 之外的请求进行拦截(这里不写也行，一般来说注册机里面写了就好了
        if (uri.startsWith("/users/login") || uri.equals("/ikun说不能让人猜到微信接口")) return true;

        String jwt = req.getHeader("Authorization");

        // 检查 JWT 是否存在
        if (jwt == null || jwt.isEmpty()) throw new LoginException("未登录");

        try {
            // 尝试解析 JWT
            Claims claims = JwtUtils.parseJWT(jwt);
            // 放到当前线程中，供后续使用
            BaseContext.setCurrentId(claims.get("userId", String.class));
//            // 可以将 claims 放入请求属性中以供后续使用
//            req.setAttribute("claims", claims);
        } catch (JwtException e) {
            throw new LoginException("登录信息有误");
        }
        return true;
    }

    @Override   //目标资源方法运行后运行
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
//        log.info("postHandle  标资源方法运行后运行");

    }

    @Override   //视图渲染完毕后运行，最后运行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();  // 清除当前线程中的用户ID，避免了内存泄漏的风险。
    }
}
