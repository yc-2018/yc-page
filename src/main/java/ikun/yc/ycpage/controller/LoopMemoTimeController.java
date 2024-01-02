package ikun.yc.ycpage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.LoopMemoTime;
import ikun.yc.ycpage.service.LoopMemoTimeService;
import lombok.RequiredArgsConstructor;
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

    /**
     * 获取循环备忘录时间列表
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

    return R.success(loopMemoTimeService.page(new Page<>(page, pageSize),
            new LambdaQueryWrapper<LoopMemoTime>()
                    .eq(LoopMemoTime::getToDoItemId, itemId)
                    .orderByDesc(LoopMemoTime::getMemoDate)));
    }
}
