package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
public interface UserService extends IService<User> {

    R<?> login(HttpServletRequest request, String key, String expireTime);
}
