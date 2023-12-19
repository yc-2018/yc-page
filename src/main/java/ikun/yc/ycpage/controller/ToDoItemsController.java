package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.exception.FieldIsNullException;
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
     *
     * @param toDoItems 待办对象
     * @return 成功与否
     * @author 仰晨
     * @since 2023-12-03 22:31:22
     */
    @PostMapping
    public R<Boolean> addItem(@RequestBody ToDoItems toDoItems) {
        if (toDoItems.getItemType() == null)throw new FieldIsNullException("待办类型不能为空");
        if (toDoItems.getContent() == null) throw new FieldIsNullException("待办内容不能为空");

        toDoItems.setCreateTime(null);                      // 不允许传递创建时间
        toDoItems.setUserId(BaseContext.getCurrentId());    // 设置为登录用户id，不然就可以被随便乱搞了

        return toDoItemsService.addItem(toDoItems);
    }

    /**
     * 获取待办列表
     *
     * @param page      第几页
     * @param pageSize  每页多少条
     * @param completed 想看的完成类型 0 未完成 1 已完成 -1 全部
     * @param type      待办类型
     * @return 待办列表
     */
    @GetMapping("/{type}")
    public R<Page<ToDoItems>> getItem(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(defaultValue = "0") Integer completed,
                                      @PathVariable Integer type) {

        LambdaQueryWrapper<ToDoItems> queryWrapper = new LambdaQueryWrapper<ToDoItems>()
                .eq(ToDoItems::getItemType, type)
                .eq(ToDoItems::getUserId, BaseContext.getCurrentId())               // 请求头的token 的id
                .eq(completed != -1, ToDoItems::getCompleted, completed)   // 0 未完成 1 已完成 -1 全部
                .lt(completed == -1, ToDoItems::getCompleted, 10)      // >=10 已删除
                .orderByDesc(ToDoItems::getUpdateTime);

        return R.success(toDoItemsService.page(new Page<>(page, pageSize), queryWrapper));
    }

    /**
     * 修改待办
     * @param toDoItem 待办对象
     */
    @PutMapping
    public R<Boolean> updateItem(@RequestBody ToDoItems toDoItem) {
        log.info("待办更新参数：{}", toDoItem);
        toDoItem.setCreateTime(null);   // 不允许更新创建时间
        LambdaUpdateWrapper<ToDoItems> updateWrapper = new LambdaUpdateWrapper<ToDoItems>()
                .eq(ToDoItems::getId, toDoItem.getId())
                .eq(ToDoItems::getUserId, BaseContext.getCurrentId());

        // 如果 NumberOfRecurrences 不为空，则在数据库层面增加 1
        if (toDoItem.getNumberOfRecurrences() != null)
            updateWrapper.setSql("number_of_recurrences = number_of_recurrences + 1");

        boolean updateSuccess = toDoItemsService.update(toDoItem,updateWrapper);

        return updateSuccess ? R.success(true) : R.error("修改失败");
    }

    /**
     * 逻辑删除待办
     * 获取的时候不要拿大于十的就好了。为了删除后还能区分是否完成。10就是未完成，11就是完成。
     * @param id 待办id
     */
    @DeleteMapping("/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        log.info("逻辑删除待办id：{}", id);
        LambdaUpdateWrapper<ToDoItems> updateWrapper = new LambdaUpdateWrapper<ToDoItems>()
                .eq(ToDoItems ::getId, id)
                .eq(ToDoItems ::getUserId, BaseContext.getCurrentId())
                .setSql("completed = completed + 10");

        boolean updateSuccess = toDoItemsService.update(new ToDoItems(), updateWrapper);

        return updateSuccess ? R.success(true) : R.error("删除失败");
    }
}
