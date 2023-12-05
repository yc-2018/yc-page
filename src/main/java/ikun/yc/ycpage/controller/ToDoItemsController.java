package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.service.ToDoItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 待办
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/toDoItems")
public class ToDoItemsController {
    private final ToDoItemsService toDoItemsService;

    /**
     * 添加待办
     * @param toDoItems 待办对象
     * @return 成功与否
     * @author 仰晨
     * @since 2023-12-03 22:31:22
     */
    @PostMapping
    public R<Boolean> addItem(@RequestBody ToDoItems toDoItems) {
        Integer itemType = toDoItems.getItemType();
        if (itemType == null) return R.error("待办类型不能为空");
        return R.success(toDoItemsService.save(toDoItems));
    }

    @GetMapping("/{type}")
    public R<List<ToDoItems>> getItem(@PathVariable Integer type) {
        LambdaQueryWrapper<ToDoItems> queryWrapper = new LambdaQueryWrapper<ToDoItems>()
                .eq(ToDoItems::getItemType, type);
        // 请求头的token
        return R.success(toDoItemsService.list(queryWrapper));
    }
}
