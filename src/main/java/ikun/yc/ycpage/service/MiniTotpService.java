package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.MiniTotp;

import java.util.List;

/**
 * 小程序2FA验证码服务
 *
 * @author cgl
 * @since 2026/10/06
 */
public interface MiniTotpService extends IService<MiniTotp> {

    /** 查询当前用户的全部2FA条目 */
    List<MiniTotp> listCurrentUserTotps();

    /** 新增当前用户的2FA条目 */
    boolean addCurrentUserTotp(MiniTotp totp);

    /** 修改当前用户的2FA条目，名称、账号和密钥都可以改 */
    boolean updateCurrentUserTotp(MiniTotp totp);

    /**
     * 置顶或取消置顶当前用户的指定2FA条目
     *
     * @param id  2FA条目ID
     * @param top true 表示置顶，写入当前时间戳；false 表示取消置顶，重置为 0
     */
    boolean setCurrentUserTotpTop(Integer id, boolean top);

    /** 删除当前用户的指定2FA条目 */
    boolean deleteCurrentUserTotp(Integer id);
}
