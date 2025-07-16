package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.LoopMemoItem;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.mapper.MemoMapper;
import ikun.yc.ycpage.service.LoopMemoItemService;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final LoopMemoItemService loopMemoItemService;


    /**
     * 添加待办
     * @param memo 待办事项请全体
     * @return 成功或失败或被禁用。
     */
    @Override
    public R<Integer> addItem(Memo memo) {
        String userId = BaseContext.getCurrentId();

        memo.setCreateTime(null);
        memo.setUserId(userId);

        boolean save = this.save(memo);
        return save ? R.success(memo.getId()) : R.error("添加失败");
    }



    /**
     * @return 分组统计加在标签上面 未完成的条数。但是不包括 2长期、1循环、4英语、5日记、和当前的
     */
    @Override
    public Map getGroupMemoCount(Integer type) {
        // 假设这是从MyBatis查询返回的原始列表
        List<Map> originalList = memoMapper.selectGroupMemoCount(new Memo(BaseContext.getCurrentId(), type));

        // 转换列表为期望的格式
        return originalList.stream()
                .collect(Collectors.toMap(
                        map -> map.get("item_type"), // 键：item_type
                        map -> map.get("count(*)")  // 值：count
                ));
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
        LambdaUpdateWrapper<Memo> updateWrapper = new LambdaUpdateWrapper<Memo>()
                .eq(Memo::getId, memo.getId())
                .eq(Memo::getUserId, memo.getUserId());

        // 完成或编辑(循环+1 以外的直接更新)
        if (memo.getNumberOfRecurrences() == null) {
            if (memo.getCompleted() != null && memo.getCompleted() == 1 && memo.getOkTime() == null) { // 完成没提供时间，选择当前时间
                memo.setOkTime(LocalDateTime.now());
            }
            return this.update(memo, updateWrapper);
        }

        return this.update(updateWrapper
                .setSql("number_of_recurrences = COALESCE(number_of_recurrences, 0) + 1")
                .set(Memo::getUpdateTime, LocalDateTime.now())  // 更新时间( 没有备忘录对象作为第一个参数 MyBatisPlus不会自动更新时间
        ) && loopMemoItemService.save(new LoopMemoItem(memo.setOkText(memo.getOkText())));
    }
}