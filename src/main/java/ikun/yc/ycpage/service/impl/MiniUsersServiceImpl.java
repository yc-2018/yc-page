package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.entity.MiniUsers;
import ikun.yc.ycpage.entity.dto.MiniSessionDTO;
import ikun.yc.ycpage.service.MiniUsersService;
import ikun.yc.ycpage.mapper.MiniUsersMapper;
import ikun.yc.ycpage.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
* author 陈光龙
* description 针对表【mini_users(微信小程序用户表)】的数据库操作Service实现
* createDate 2025-03-08 14:22:41
*/
@Service
@RequiredArgsConstructor
public class MiniUsersServiceImpl extends ServiceImpl<MiniUsersMapper, MiniUsers> implements MiniUsersService{
  private final WechatMiniAuthService wechatMiniAuthService;


  @Override
  public String miniLogin(String code) throws JsonProcessingException {
    // 步骤1：通过code换取openid和session_key
    MiniSessionDTO sessionInfo = wechatMiniAuthService.getSessionInfo(code);

    // 步骤2：处理/保存用户信息
    MiniUsers user = processUserInfo(sessionInfo);

    // 步骤3：生成Jwt
    return JwtUtils.generateJwt(new HashMap<String, Object>() {{put("userId", user.getOpenid());}}, "yz");
  }

  private MiniUsers processUserInfo(MiniSessionDTO session) {
    // 查找已有用户
    MiniUsers user = this.lambdaQuery().eq(MiniUsers::getOpenid, session.getOpenid()).one();

    // 新用户注册逻辑
    if (user == null) {
      user = new MiniUsers();
      user.setOpenid(session.getOpenid());
      this.save(user);
      return user;
    }

    // 更新登录信息
    user.setLastLoginTime(LocalDateTime.now());
    user.setLoginCount(user.getLoginCount() + 1);
    this.updateById(user);

    return user;
  }

}




