package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 服务控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 * @description 由 Mybatisplus Code Generator 创建
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
        redisTemplate.opsForValue().get(key);
        return null;
    }


    @GetMapping("/get")
    public Object get() {
        return usersService.list();
    }
}