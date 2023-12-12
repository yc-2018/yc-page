//仰晨study 创建时间2023/4/24 1:45 星期一
package ikun.yc.ycpage.interceptor;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.JwtUtils;
import ikun.yc.ycpage.common.LoginException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override   //目标资源方法运行前运行，返回true: 放行，放回false，不放行
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        System.out.println("preHandle  目标资源方法运行前运行");

        String uri = req.getRequestURI();
        log.info("请求路径：{}", uri);
        // 对 /login 和 /wechat 之外的请求进行拦截(这里不写也行，一般来说注册机里面写了就好了
        if (uri.startsWith("/users/login") || uri.equals("/wechat")) return true;

        String jwt = req.getHeader("Authorization");

        // 检查 JWT 是否存在
        if (jwt == null || jwt.isEmpty()) throw new LoginException("未登录");

        /*{
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return false;
        }*/

        try {
            // 尝试解析 JWT
            Claims claims = JwtUtils.parseJWT(jwt);
            // 放到当前线程中，供后续使用
            BaseContext.setCurrentId(claims.get("userId", String.class));
//            // 可以将 claims 放入请求属性中以供后续使用
//            req.setAttribute("claims", claims);
        } catch (JwtException e) {
//            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "解析令牌失败");
//            return false;
            throw new LoginException("登录信息有误");
        }
        return true;
    }

    @Override   //目标资源方法运行后运行
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle  标资源方法运行后运行");
    }

    @Override   //视图渲染完毕后运行，最后运行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("afterCompletion  视图渲染完毕后运行，最后运行");
    }
}
