package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.dto.MemoIncompleteCountDto;
import ikun.yc.ycpage.mapper.MemoMapper;
import ikun.yc.ycpage.service.MemoService;
import ikun.yc.ycpage.service.MemoTagRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 备忘录服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoServiceImpl extends ServiceImpl<MemoMapper, Memo> implements MemoService {
    private final MemoMapper memoMapper;
    private final MemoTagRelationService memoTagRelationService;


    /**
     * 添加待办
     * @param memo 待办事项请全体
     * @return 成功或失败或被禁用。
     */
    @Override
    public R<Integer> addItem(Memo memo) {
        String userId = BaseContext.getCurrentId();

        memo.validateImgArr(); // 校验备忘图片字段长度
        memo.setCreateTime(null);
        memo.setUserId(userId);
        memoTagRelationService.validateMemoTags(memo.getItemType(), memo.getTagIds());

        boolean save = this.save(memo);
        if (save && memo.getTagIds() != null) memoTagRelationService.saveMemoTags(memo.getId(), memo.getItemType(), memo.getTagIds());
        return save ? R.success(memo.getId()) : R.error("添加失败");
    }



    /** 查询需要预加载展示的未完成待办数量 */
    @Override
    public List<MemoIncompleteCountDto> getIncompleteCounts(Integer currentType) {
        return memoMapper.selectIncompleteCounts(new Memo(BaseContext.getCurrentId(), currentType));
    }

    /** 批量填充备忘标签 */
    @Override
    public void fillMemoTags(List<Memo> memos) {
        memoTagRelationService.fillMemoTags(memos);
    }


    /**
     * 更新备忘录item
     *
     * @param memo 要修改的item信息
     * @return 成功与否
     */
    @Transactional
    @Override
    public boolean updateItem(Memo memo) {
        memo.validateImgArr(); // 校验备忘图片字段长度
        if (Objects.equals(memo.getCompleted(), 1) && Objects.isNull(memo.getOkTime())) { // 完成没提供时间，选择当前时间
            memo.setOkTime(LocalDateTime.now());
        }
        Memo oldMemo = null; // 原备忘数据
        if (memo.getTagIds() != null || memo.getItemType() != null) {
            oldMemo = this.lambdaQuery()
                    .select(Memo::getId, Memo::getItemType)
                    .eq(Memo::getId, memo.getId())
                    .eq(Memo::getUserId, memo.getUserId())
                    .one();
            if (oldMemo == null) return false;
            Integer tagItemType = memo.getItemType() == null ? oldMemo.getItemType() : memo.getItemType(); // 标签对应的备忘类型
            memoTagRelationService.validateMemoTags(tagItemType, memo.getTagIds());
        }

        boolean update = this.update(memo, Wrappers.<Memo>lambdaUpdate()
                .eq(Memo::getId, memo.getId())
                .eq(Memo::getUserId, memo.getUserId())
        );
        if (!update) return false;
        if (memo.getTagIds() != null) {
            Integer tagItemType = memo.getItemType() == null ? oldMemo.getItemType() : memo.getItemType(); // 标签对应的备忘类型
            memoTagRelationService.saveMemoTags(memo.getId(), tagItemType, memo.getTagIds());
        } else if (memo.getItemType() != null) {
            memoTagRelationService.clearMemoTags(memo.getId());
        }
        return true;
    }

}
