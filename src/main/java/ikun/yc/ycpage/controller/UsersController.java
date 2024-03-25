package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Users;
import ikun.yc.ycpage.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UsersController {
    private final UsersService usersService;

    /**
     * 登录（同一个 ip一分钟内请求超过3次，封禁一个小时。）
     * @param request 用来获取用户的ip
     * @param key 验证码
     * @param expireTime 登录超时时间
     * @return jwt
     */
    @PostMapping("login")
    public R<?> login(HttpServletRequest request, String key, @RequestParam(defaultValue = "bt") String expireTime ) {
        return usersService.login(request, key,expireTime);
    }

    /**
     * 更新用户名或头像
     *
     * @param users 用户信息
     * @return {@code R<?>}
     */
    @PutMapping("update")
    public R<?> updateNameOrAvatar(Users users) {
        return R.success(usersService.updateById(users.getNameAndAvatar()));
    }

}