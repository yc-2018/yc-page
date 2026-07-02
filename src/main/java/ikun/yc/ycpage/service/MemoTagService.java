package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.MemoTag;

import java.util.List;

/**
 * 备忘标签服务接口
 *
 * @author Codex
 * @since 2026-07-03
 */
public interface MemoTagService extends IService<MemoTag> {

    /** 查询当前用户指定类型下的标签 */
    List<MemoTag> listCurrentUserTags(Integer itemType);

    /** 新增当前用户指定类型下的标签 */
    Integer addCurrentUserTag(MemoTag memoTag);

    /** 修改当前用户的标签名称 */
    boolean updateCurrentUserTag(MemoTag memoTag);

    /** 删除当前用户的标签和标签关联 */
    boolean deleteCurrentUserTag(Integer id);

    /** 检查标签是否属于当前用户和指定类型 */
    boolean existsCurrentUserTag(Integer tagId, Integer itemType);
}
