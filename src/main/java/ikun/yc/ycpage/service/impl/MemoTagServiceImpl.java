package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.FieldIsNullException;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MemoTag;
import ikun.yc.ycpage.entity.MemoTagRelation;
import ikun.yc.ycpage.mapper.MemoTagMapper;
import ikun.yc.ycpage.service.MemoTagRelationService;
import ikun.yc.ycpage.service.MemoTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 备忘标签服务实现
 *
 * @author Codex
 * @since 2026-07-03
 */
@Service
@RequiredArgsConstructor
public class MemoTagServiceImpl extends ServiceImpl<MemoTagMapper, MemoTag> implements MemoTagService {
    private final MemoTagRelationService memoTagRelationService;

    /** 查询当前用户指定类型下的标签 */
    @Override
    public List<MemoTag> listCurrentUserTags(Integer itemType) {
        if (itemType == null) throw new FieldIsNullException("备忘类型不能为空");
        return this.lambdaQuery()
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .eq(MemoTag::getItemType, itemType)
                .orderByAsc(MemoTag::getId)
                .list();
    }

    /** 新增当前用户指定类型下的标签 */
    @Override
    public Integer addCurrentUserTag(MemoTag memoTag) {
        validateTag(memoTag);
        memoTag.setId(null);
        memoTag.setName(memoTag.getName().trim());
        memoTag.setUserId(BaseContext.getCurrentId());
        if (existsTagName(memoTag.getItemType(), memoTag.getName(), null)) throw new ParamException("标签已存在");
        boolean save = this.save(memoTag);
        if (!save) throw new ParamException("新增标签失败");
        return memoTag.getId();
    }

    /** 修改当前用户的标签名称 */
    @Override
    public boolean updateCurrentUserTag(MemoTag memoTag) {
        if (memoTag == null || memoTag.getId() == null) throw new FieldIsNullException("标签不能为空");
        if (memoTag.getName() == null || memoTag.getName().trim().isEmpty()) throw new FieldIsNullException("标签名称不能为空");
        if (memoTag.getName().trim().length() > 32) throw new ParamException("标签名称不能超过32个字");
        String tagName = memoTag.getName().trim(); // 标签名称
        MemoTag oldTag = getCurrentUserTag(memoTag.getId());
        if (existsTagName(oldTag.getItemType(), tagName, oldTag.getId())) throw new ParamException("标签已存在");
        return this.lambdaUpdate()
                .eq(MemoTag::getId, oldTag.getId())
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .set(MemoTag::getName, tagName)
                .update();
    }

    /** 删除当前用户的标签和标签关联 */
    @Transactional
    @Override
    public boolean deleteCurrentUserTag(Integer id) {
        MemoTag memoTag = getCurrentUserTag(id);
        memoTagRelationService.remove(this.lambdaUpdateRelation(memoTag.getId()));
        return this.removeById(memoTag.getId());
    }

    /** 检查标签是否属于当前用户和指定类型 */
    @Override
    public boolean existsCurrentUserTag(Integer tagId, Integer itemType) {
        if (tagId == null || itemType == null) return false;
        return this.lambdaQuery()
                .eq(MemoTag::getId, tagId)
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .eq(MemoTag::getItemType, itemType)
                .count() > 0;
    }

    /** 查询当前用户标签 */
    private MemoTag getCurrentUserTag(Integer id) {
        if (id == null) throw new FieldIsNullException("标签不能为空");
        MemoTag memoTag = this.lambdaQuery()
                .eq(MemoTag::getId, id)
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .one();
        if (memoTag == null) throw new ParamException("标签不存在");
        return memoTag;
    }

    /** 校验标签参数 */
    private void validateTag(MemoTag memoTag) {
        if (memoTag == null) throw new FieldIsNullException("标签不能为空");
        if (memoTag.getItemType() == null) throw new FieldIsNullException("备忘类型不能为空");
        if (memoTag.getName() == null || memoTag.getName().trim().isEmpty()) throw new FieldIsNullException("标签名称不能为空");
        if (memoTag.getName().trim().length() > 32) throw new ParamException("标签名称不能超过32个字");
    }

    /** 检查当前用户同类型下是否已有同名标签 */
    private boolean existsTagName(Integer itemType, String tagName, Integer excludeId) {
        return this.lambdaQuery()
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .eq(MemoTag::getItemType, itemType)
                .eq(MemoTag::getName, tagName)
                .ne(excludeId != null, MemoTag::getId, excludeId)
                .count() > 0;
    }

    /** 创建删除标签关联的条件 */
    private com.baomidou.mybatisplus.core.conditions.Wrapper<MemoTagRelation> lambdaUpdateRelation(Integer tagId) {
        return com.baomidou.mybatisplus.core.toolkit.Wrappers.<MemoTagRelation>lambdaQuery()
                .eq(MemoTagRelation::getTagId, tagId);
    }
}
