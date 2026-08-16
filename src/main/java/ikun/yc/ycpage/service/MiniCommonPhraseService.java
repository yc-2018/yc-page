package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.MiniCommonPhrase;

import java.util.List;

/**
 * 小程序打卡常用语服务
 *
 * @author cgl
 * @since 2026/08/16
 */
public interface MiniCommonPhraseService extends IService<MiniCommonPhrase> {

    /** 查询当前用户的全部常用语 */
    List<MiniCommonPhrase> listCurrentUserPhrases();

    /** 新增当前用户的常用语 */
    boolean addCurrentUserPhrase(MiniCommonPhrase phrase);

    /** 修改当前用户的常用语 */
    boolean updateCurrentUserPhrase(MiniCommonPhrase phrase);

    /** 将当前用户的指定常用语置顶 */
    boolean topCurrentUserPhrase(Integer id);

    /** 删除当前用户的指定常用语 */
    boolean deleteCurrentUserPhrase(Integer id);
}
