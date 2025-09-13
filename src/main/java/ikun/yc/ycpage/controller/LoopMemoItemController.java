package ikun.yc.ycpage.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.exception.SqlSaveException;
import ikun.yc.ycpage.common.exception.SqlUpdateException;
import ikun.yc.ycpage.entity.LoopMemoItem;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.service.LoopMemoItemService;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 循环备忘录时间控制器
 *
 * @author ChenGuangLong
 * @since 2024/01/02 19:26:24
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/loopMemoItem")
public class LoopMemoItemController {

    private final LoopMemoItemService loopMemoItemService;
    private final MemoService memoService;

    /**
     * 获取循环备忘记录列表
     *
     * @param page     第几页
     * @param pageSize 页面大小
     * @param itemId   待办id
     * @return 待办时间列表
     * @author ChenGuangLong
     * @since 2024/01/02 19:45:50
     */
    @GetMapping("/{itemId}")
    public R<Page<LoopMemoItem>> getLoopMemoItemList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @PathVariable Integer itemId) {
        return R.success(loopMemoItemService.lambdaQuery()
            .eq(LoopMemoItem::getMemoId, itemId)
            .orderByDesc(LoopMemoItem::getMemoDate)
            .page(new Page<>(page, pageSize))
        );
    }

    /**
     * 添加循环备忘录项
     *
     * @param loopMemoItem 循环备注明细项
     * @return {@code LoopMemoItem }
     * @author ChenGuangLong
     * @since 2025/07/16 20:59
     */
    @PostMapping
    @Transactional
    public R<LoopMemoItem> addLoopMemoItem(@RequestBody LoopMemoItem loopMemoItem) {
        loopMemoItemService.save(loopMemoItem);
        memoService.update(Wrappers.<Memo>lambdaUpdate()
                .eq(Memo::getId, loopMemoItem.getMemoId())
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .set(Memo::getUpdateTime, LocalDateTime.now())  // 更新时间
                .setSql("number_of_recurrences = (select count(*) from loop_memo_item where memo_id = " + loopMemoItem.getMemoId() + ")")
        );
        return R.success(loopMemoItem);
    }


    /** 更新循环备忘录 */
    @PutMapping
    public R<LoopMemoItem> updateLoopMemoItem(@RequestBody LoopMemoItem loopMemoItem) {
        boolean b = loopMemoItemService.update(loopMemoItem, Wrappers.<LoopMemoItem>lambdaUpdate()
                .eq(LoopMemoItem::getId, loopMemoItem.getId())
                .eq(LoopMemoItem::getMemoId, loopMemoItem.getMemoId())
        );
        if (!b) throw new SqlUpdateException("修改失败");
        return R.success(loopMemoItem);
    }

    /**
     * 删除循环备忘录
     *
     * @param memoId 备忘录id
     * @param loopId 循环id
     * @return {@code R<Boolean> }
     */
    @Transactional
    @DeleteMapping("/{memoId}/{loopId}")
    public R<Boolean> deleteLoopMemoItem(@PathVariable String memoId, @PathVariable Integer loopId) {
        // 删除循环备忘项
        loopMemoItemService.remove(Wrappers.<LoopMemoItem>lambdaUpdate()
                .eq(LoopMemoItem::getId, loopId)
                .eq(LoopMemoItem::getMemoId, memoId)
        );

        // 待办减一
        boolean b = memoService.lambdaUpdate()
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .eq(Memo::getId, memoId)
                .setSql("number_of_recurrences = (select count(*) from loop_memo_item where memo_id = " + memoId + ")")
                .update();

        return R.success(b);
    }

}
