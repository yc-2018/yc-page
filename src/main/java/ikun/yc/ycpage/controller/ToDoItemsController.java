package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.service.ToDoItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
        log.info("待办添加参数：{},当前线程id为{}", toDoItems, BaseContext.getCurrentId());
        toDoItems.setUserId(BaseContext.getCurrentId());    // 设置为登录用户id，不然就可以被随便乱搞了
        return toDoItems.getItemType() == null?
                R.error("待办类型不能为空"):
                R.success(toDoItemsService.save(toDoItems));
    }

    @GetMapping("/{type}")
    public R<Page<ToDoItems>> getItem(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @PathVariable Integer type) {

        LambdaQueryWrapper<ToDoItems> queryWrapper = new LambdaQueryWrapper<ToDoItems>()
                .eq(ToDoItems::getItemType, type)
                .eq(ToDoItems::getUserId, BaseContext.getCurrentId())  // 请求头的token 的id
                .orderByDesc(ToDoItems::getUpdateTime);

        return R.success(toDoItemsService.page(new Page<>(page, pageSize),queryWrapper));
    }
}
