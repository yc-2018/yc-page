package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.MemoTag;
import ikun.yc.ycpage.service.MemoTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 备忘类型标签
 *
 * @author Codex
 * @since 2026-07-03
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/memoTag")
public class MemoTagController {
    private final MemoTagService memoTagService;

    /** 获取当前用户指定备忘类型下的标签 */
    @GetMapping("/{itemType}")
    public R<List<MemoTag>> list(@PathVariable Integer itemType) {
        return R.success(memoTagService.listCurrentUserTags(itemType));
    }

    /** 新增当前用户指定备忘类型下的标签 */
    @Log
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD)
    public R<Integer> add(@RequestBody MemoTag memoTag) {
        return R.success(memoTagService.addCurrentUserTag(memoTag));
    }

    /** 修改当前用户的标签名称 */
    @Log
    @PutMapping
    @CountControl(operationType = CountControlAspect.UPDATE)
    public R<Boolean> update(@RequestBody MemoTag memoTag) {
        return memoTagService.updateCurrentUserTag(memoTag) ? R.success(true) : R.error("修改失败");
    }

    /** 删除当前用户的标签和标签关联 */
    @Log
    @DeleteMapping("/{id}")
    @CountControl(operationType = CountControlAspect.DELETE)
    public R<Boolean> delete(@PathVariable Integer id) {
        return memoTagService.deleteCurrentUserTag(id) ? R.success(true) : R.error("删除失败");
    }
}
