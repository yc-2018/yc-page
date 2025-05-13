package ikun.yc.ycpage.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.LoopMemoTime;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.service.LoopMemoTimeService;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 循环备忘录时间控制器
 *
 * @author ChenGuangLong
 * @since 2024/01/02 19:26:24
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/loopMemoTime")
public class LoopMemoTimeController {

    private final LoopMemoTimeService loopMemoTimeService;
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
    public R<Page<LoopMemoTime>> getLoopMemoTimeList(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "20") Integer pageSize,
                                                     @PathVariable Integer itemId) {
        return R.success(loopMemoTimeService.lambdaQuery()
            .eq(LoopMemoTime::getMemoId, itemId)
            .orderByDesc(LoopMemoTime::getMemoDate)
            .page(new Page<>(page, pageSize))
        );
    }


    /**
     * 更新循环备忘录
     *
     * @param loopMemoTime 循环备忘录对象
     * @return {@code R<Boolean> }
     */
    @PutMapping
    public R<Boolean> updateLoopMemoTime(@RequestBody LoopMemoTime loopMemoTime) {
        boolean b = loopMemoTimeService.update(loopMemoTime, Wrappers.<LoopMemoTime>lambdaUpdate()
                .eq(LoopMemoTime::getId, loopMemoTime.getId())
                .eq(LoopMemoTime::getMemoId, loopMemoTime.getMemoId())
        );
        return R.success(b);
    }

    /**
     * 删除循环备忘录
     *
     * @param loopId 循环id
     * @param memoId 备忘录id
     * @return {@code R<Boolean> }
     */
    @Transactional
    @DeleteMapping("/{memoId}/{loopId}")
    public R<Boolean> deleteLoopMemoTime(@PathVariable Integer loopId, @PathVariable String memoId) {
        // 待办减一
        boolean b = memoService.lambdaUpdate()
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .eq(Memo::getId, memoId)
                .setSql("number_of_recurrences = number_of_recurrences - 1")
                .update();
        if (b) {
            // 删除循环备忘录
            b = loopMemoTimeService.remove(Wrappers.<LoopMemoTime>lambdaUpdate()
                    .eq(LoopMemoTime::getId, loopId)
                    .eq(LoopMemoTime::getMemoId, memoId)
            );
        }
        return R.success(b);
    }

}
