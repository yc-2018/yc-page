package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.MemoTag;
import ikun.yc.ycpage.entity.MemoTagRelation;
import ikun.yc.ycpage.mapper.MemoTagMapper;
import ikun.yc.ycpage.mapper.MemoTagRelationMapper;
import ikun.yc.ycpage.service.MemoTagRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 备忘标签关联服务实现
 *
 * @author Codex
 * @since 2026-07-03
 */
@Service
@RequiredArgsConstructor
public class MemoTagRelationServiceImpl extends ServiceImpl<MemoTagRelationMapper, MemoTagRelation> implements MemoTagRelationService {
    private final MemoTagMapper memoTagMapper;

    /** 校验备忘标签是否都属于当前用户指定类型 */
    @Override
    public void validateMemoTags(Integer itemType, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        Set<Integer> distinctTagIds = new LinkedHashSet<>(tagIds); // 去重后的标签ID
        List<MemoTag> tags = selectCurrentUserTags(itemType, distinctTagIds);
        if (tags.size() != distinctTagIds.size()) throw new ParamException("标签有误");
    }

    /** 替换指定备忘的标签关联 */
    @Transactional
    @Override
    public void saveMemoTags(Integer memoId, Integer itemType, List<Integer> tagIds) {
        if (memoId == null) throw new ParamException("备忘不存在");
        clearMemoTags(memoId);
        if (tagIds == null || tagIds.isEmpty()) return;
        Set<Integer> distinctTagIds = new LinkedHashSet<>(tagIds); // 去重后的标签ID
        List<MemoTag> tags = selectCurrentUserTags(itemType, distinctTagIds);
        if (tags.size() != distinctTagIds.size()) throw new ParamException("标签有误");

        List<MemoTagRelation> relations = tags.stream()
                .map(tag -> new MemoTagRelation().setMemoId(memoId).setTagId(tag.getId()))
                .collect(Collectors.toList());
        this.saveBatch(relations);
    }

    /** 清空指定备忘的标签关联 */
    @Override
    public void clearMemoTags(Integer memoId) {
        this.lambdaUpdate()
                .eq(MemoTagRelation::getMemoId, memoId)
                .remove();
    }

    /** 批量填充备忘标签 */
    @Override
    public void fillMemoTags(List<Memo> memos) {
        if (memos == null || memos.isEmpty()) return;
        List<Integer> memoIds = memos.stream()
                .map(Memo::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (memoIds.isEmpty()) return;

        List<MemoTagRelation> relations = this.lambdaQuery()
                .in(MemoTagRelation::getMemoId, memoIds)
                .list();
        if (relations.isEmpty()) {
            memos.forEach(memo -> {
                memo.setTags(Collections.emptyList());
                memo.setTagIds(Collections.emptyList());
            });
            return;
        }

        List<Integer> tagIds = relations.stream()
                .map(MemoTagRelation::getTagId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, MemoTag> tagMap = memoTagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(MemoTag::getId, Function.identity()));
        Map<Integer, List<MemoTagRelation>> relationMap = relations.stream()
                .collect(Collectors.groupingBy(MemoTagRelation::getMemoId));

        memos.forEach(memo -> {
            List<MemoTagRelation> memoRelations = relationMap.getOrDefault(memo.getId(), Collections.emptyList());
            List<MemoTag> tags = new ArrayList<>();
            List<Integer> memoTagIds = new ArrayList<>();
            memoRelations.forEach(relation -> {
                MemoTag tag = tagMap.get(relation.getTagId());
                if (tag != null) {
                    tags.add(tag);
                    memoTagIds.add(tag.getId());
                }
            });
            memo.setTags(tags);
            memo.setTagIds(memoTagIds);
        });
    }

    /** 查询当前用户指定类型下的标签 */
    private List<MemoTag> selectCurrentUserTags(Integer itemType, Set<Integer> tagIds) {
        if (itemType == null) throw new ParamException("备忘类型不能为空");
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptyList();
        return com.baomidou.mybatisplus.extension.toolkit.Db.lambdaQuery(MemoTag.class)
                .eq(MemoTag::getUserId, BaseContext.getCurrentId())
                .eq(MemoTag::getItemType, itemType)
                .in(MemoTag::getId, tagIds)
                .list();
    }
}
