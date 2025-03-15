package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.MiniUsers;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* author 陈光龙
* description 针对表【mini_users(微信小程序用户表)】的数据库操作Service
* createDate 2025-03-08 14:22:41
*/
public interface MiniUsersService extends IService<MiniUsers> {

  String miniLogin(String code);
}
