package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.ControlAddItemUtil;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.mapper.ToDoItemsMapper;
import ikun.yc.ycpage.service.ToDoItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ToDoItemsServiceImpl extends ServiceImpl<ToDoItemsMapper, ToDoItems> implements ToDoItemsService {
    private final ControlAddItemUtil controlAddItemUtil;


    /**
     * 一个UserId一分钟最多请求十次。超过十次禁用5分钟
     * @param toDoItem 待办事项请全体
     * @return 成功或失败或被禁用。
     */
    @Override
    public R<Boolean> addItem(ToDoItems toDoItem) {
        String userId = BaseContext.getCurrentId();

        // 检查用户是否被禁用
        if (controlAddItemUtil.getOneMinuteAddItemById(userId))
            return R.error("请求过于频繁，您已被禁用添加备忘待办5分钟");

        // -----------------处理添加待办事项的业务逻辑---------------
        toDoItem.setCreateTime(null);
        toDoItem.setUserId(userId);

        boolean save = this.save(toDoItem);
        return save ? R.success(true) : R.error("添加失败");
    }
}