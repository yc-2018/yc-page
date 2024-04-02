package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Users;
import ikun.yc.ycpage.service.UsersService;
import ikun.yc.ycpage.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

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
    @PutMapping
    public R<?> updateNameOrAvatar(@RequestBody Users users, @RequestHeader("Authorization") String authHeader) {
        boolean ok = usersService.updateById(users.getNameAndAvatar());     // 更新用户名或头像
        if (!ok) return R.error("更新失败");

        Claims claims = JwtUtils.parseJWT(authHeader);                      // 解析 JWT
        Date expiration = claims.getExpiration();                           // 获取过期时间
        long expire = expiration.getTime() - System.currentTimeMillis();    // 计算剩余过期时间
        claims.put("username", users.getUsername());
        claims.put("avatar", users.getAvatar());
        String newJwt = JwtUtils.generateJwt(claims, expire);               // 生成新的 JWT
        return R.success(newJwt);
    }

}