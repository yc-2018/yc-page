package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.JwtUtils;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Users;
import ikun.yc.ycpage.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;

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
    public final RedisTemplate redisTemplate;

    @PostMapping
    public R<?> login(String key) {
        String user = (String) redisTemplate.opsForValue().get(key);
        if (user == null) {
            return R.error("验证码不存在");
        }
        log.info("用户：{},登录", user);
        Users loginUser = usersService.getById(user);
        if (loginUser == null) {
            log.info("是新用户");
            usersService.save(new Users().setId(user));
        }
        return R.success(JwtUtils.generateJwt(new HashMap<String, Object>(){{put("userId", user);}}));
    }


    @GetMapping("/get")
    public Object get() {
        return usersService.list();
    }
}