//仰晨study 创建时间2023/12/4 1:47 星期一
package ikun.yc.ycpage.utils;

import java.util.Random;

/**
 * 生成六位随机数字验证码
 */
public class VerificationCodeUtil {
    /**
     * 生成六位随机数字验证码
     * @return 返回六位验证码
     */
    public static String generateCode() {
        Random random = new Random();
        int num = random.nextInt(999999); // 生成一个随机数
        // 确保它是一个六位数
        return String.format("%06d", num);
    }
}
