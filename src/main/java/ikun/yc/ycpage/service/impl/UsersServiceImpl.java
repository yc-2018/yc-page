package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.entity.Users;
import ikun.yc.ycpage.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ikun.yc.ycpage.service.UsersService;
import org.springframework.stereotype.Service;

/**
 * 服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 * @description 由 Mybatisplus Code Generator 创建
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {
    private final UsersMapper usersMapper;

}