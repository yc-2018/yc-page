package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.LoopMemoTime;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.mapper.ToDoItemsMapper;
import ikun.yc.ycpage.service.LoopMemoTimeService;
import ikun.yc.ycpage.service.ToDoItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ToDoItemsServiceImpl extends ServiceImpl<ToDoItemsMapper, Memo> implements ToDoItemsService {
    private final ToDoItemsMapper toDoItemsMapper;
    private final LoopMemoTimeService loopMemoTimeService;


    /**
     * 添加待办
     * @param toDoItem 待办事项请全体
     * @return 成功或失败或被禁用。
     */
    @Override
    public R<Integer> addItem(Memo toDoItem) {
        String userId = BaseContext.getCurrentId();

        toDoItem.setCreateTime(null);
        toDoItem.setUserId(userId);

        boolean save = this.save(toDoItem);
        return save ? R.success(toDoItem.getId()) : R.error("添加失败");
    }



    /**
     * @return 分组统计加在标签上面 未完成的条数。但是不包括 2长期、1循环、4英语、5日记、和当前的
     */
    @Override
    public Map getGroupToDoItemsCount(Integer type) {
        // 假设这是从MyBatis查询返回的原始列表
        List<Map> originalList = toDoItemsMapper.selectGroupToDoItemsCount(new Memo(BaseContext.getCurrentId(), type));

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
     * @param toDoItem 要修改的item信息
     * @return 成功与否
     */
    @Transactional
    @Override
    public boolean updateItem(Memo toDoItem) {
        LambdaUpdateWrapper<Memo> updateWrapper = new LambdaUpdateWrapper<Memo>()
                .eq(Memo::getId, toDoItem.getId())
                .eq(Memo::getUserId, toDoItem.getUserId());

        // 完成或编辑(循环+1 以外的直接更新)
        if (toDoItem.getNumberOfRecurrences() == null) return this.update(toDoItem, updateWrapper);

        // 循环（如果 NumberOfRecurrences 不为空，则在数据库层面增加 1）
        toDoItem.setNumberOfRecurrences(null); // 避免更新时替换掉本来的值再加一
        return this.update(toDoItem, updateWrapper.setSql("number_of_recurrences = COALESCE(number_of_recurrences, 0) + 1"))
                && loopMemoTimeService.save(new LoopMemoTime(toDoItem));
    }
}