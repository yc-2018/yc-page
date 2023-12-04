//仰晨study 创建时间2023/12/5 1:49 星期二
package ikun.yc.ycpage.service.impl;

import ikun.yc.ycpage.common.VerificationCodeUtil;
import ikun.yc.ycpage.service.WechatService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
@Service
public class WechatServiceImpl implements WechatService {
    @Resource
    public RedisTemplate<String, String> redisTemplate;

    /**
     * @param toUserName 用户名
     * @return 验证码模板
     */
    @Override
    public String login(String toUserName) {
        String code;
        do {
            code = VerificationCodeUtil.generateCode();
        } while (Boolean.TRUE.equals(redisTemplate.hasKey(code)));   // 直接检查验证码是否已作为键存在

        redisTemplate.opsForValue().set(code, toUserName,60, TimeUnit.MINUTES);          // 存储验证码和用户名的映射
        return "登录验证码为:" + code + ", 5分钟内有效。\n失效后可重新发送登录获取验证码,目前网站地址:https://yc556.gitee.io";
    }
}
