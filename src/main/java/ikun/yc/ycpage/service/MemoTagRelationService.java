package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.MemoTagRelation;

import java.util.List;

/**
 * 备忘标签关联服务接口
 *
 * @author Codex
 * @since 2026-07-03
 */
public interface MemoTagRelationService extends IService<MemoTagRelation> {

    /** 校验备忘标签是否都属于当前用户指定类型 */
    void validateMemoTags(Integer itemType, List<Integer> tagIds);

    /** 替换指定备忘的标签关联 */
    void saveMemoTags(Integer memoId, Integer itemType, List<Integer> tagIds);

    /** 清空指定备忘的标签关联 */
    void clearMemoTags(Integer memoId);

    /** 批量填充备忘标签 */
    void fillMemoTags(List<Memo> memos);
}
