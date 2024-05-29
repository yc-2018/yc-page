package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.common.exception.FieldIsNullException;
import ikun.yc.ycpage.entity.Memo;
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
     * 一分钟最多请求十次。超过十次禁用5分钟
     * @param memo 待办对象
     * @return 成功与否
     * @author 仰晨
     * @since 2023-12-03 22:31:22
     */
    @Log
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10, banMin = 5)
    public R<Integer> addItem(@RequestBody Memo memo) {
        if (memo.getItemType() == null)throw new FieldIsNullException("待办类型不能为空");
        if (memo.getContent() == null) throw new FieldIsNullException("待办内容不能为空");

        memo.toReviseInfo();   // 不允许更新的字段就不允许更新

        return toDoItemsService.addItem(memo);
    }

    /**
     * 获取待办列表
     *
     * @param page      第几页
     * @param pageSize  每页多少条
     * @param completed 想看的完成类型 0 未完成 1 已完成 -1 全部
     * @param type      待办类型
     * @param orderBy  排序方式 1：更新时间↓ 2：更新时间↑ 3：创建时间↓ 4：创建时间↑ 5：A↓ 6：Z↓
     * @param firstLetter 从哪个字母开始查询
     * @param keyword  搜索关键词
     //* @param dateRange 日期范围
     * @return 待办列表
     */
    @GetMapping("/{type}")
    public R<Page<Memo>> getItem(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(defaultValue = "0") Integer completed,      // 0 未完成 1 已完成 -1 全部
                                 @RequestParam(defaultValue = "1") Integer orderBy,
                                 @RequestParam(required = false) String firstLetter,
                                 @RequestParam(required = false) String keyword,
                                 //@RequestParam(required = false) List<Date> dateRange,
                                 @PathVariable Integer type) {


        R<Page<Memo>> pageR = R.success(
            toDoItemsService.lambdaQuery()
                .eq(Memo::getItemType, type)
                .eq(Memo::getUserId, BaseContext.getCurrentId())        // 请求头的token 的id
                .eq(completed != -1, Memo::getCompleted, completed)     // 0 未完成 1 已完成 -1 全部
                .lt(completed == -1, Memo::getCompleted, 10)            // >=10 已删除
                .orderByDesc(orderBy == 1, Memo::getUpdateTime)
                .orderByDesc(orderBy == 3, Memo::getCreateTime)
                .orderByDesc(orderBy == 6, Memo::getContent)
                .orderByAsc(orderBy == 2, Memo::getUpdateTime)
                .orderByAsc(orderBy == 4, Memo::getCreateTime)
                .orderByAsc(orderBy == 5, Memo::getContent)
                .likeRight(firstLetter != null, Memo::getContent, firstLetter)
                .like(keyword != null, Memo::getContent, keyword)
                .page(new Page<>(page, pageSize))
        );


        // 如果是查询未完成的 而且不是英语的，统计数量
        if (completed == 0 && type != 4)
            pageR.add("groupToDoItemsCounts", toDoItemsService.getGroupToDoItemsCount(type));

        return pageR;
    }

    /**
     * 修改待办
     * @param toDoItem 待办对象
     */
    @Log
    @PutMapping
    @CountControl(operationType = CountControlAspect.UPDATE)  // 一分钟请求超出5次，禁用1分钟
    public R<Boolean> updateItem(@RequestBody Memo toDoItem) {

        toDoItem.toReviseInfo();   // 不允许更新的字段就不允许更新

        boolean updateSuccess = toDoItemsService.updateItem(toDoItem);

        return updateSuccess ? R.success(true) : R.error("修改失败");
    }

    /**
     * 逻辑删除待办
     * 获取的时候不要拿大于十的就好了。为了删除后还能区分是否完成。10就是未完成，11就是完成。
     * @param id 待办id
     */
    @Log
    @CountControl(operationType = CountControlAspect.DELETE)
    @DeleteMapping("/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        return toDoItemsService.lambdaUpdate()
            .eq(Memo::getId, id)
            .eq(Memo::getUserId, BaseContext.getCurrentId())
            .setSql("completed = completed + 10")
            .update() ? R.success(true) : R.error("删除失败");
    }
}
