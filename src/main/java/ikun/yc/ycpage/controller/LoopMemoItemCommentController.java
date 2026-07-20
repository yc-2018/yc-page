package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.exception.FieldIsNullException;
import ikun.yc.ycpage.entity.LoopMemoItem;
import ikun.yc.ycpage.entity.LoopMemoItemComment;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.service.LoopMemoItemCommentService;
import ikun.yc.ycpage.service.LoopMemoItemService;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 循环备忘记录评论控制器
 *
 * @author codex
 * @since 2026-05-19
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/loopMemoItemComment")
public class LoopMemoItemCommentController {

    private final LoopMemoItemCommentService loopMemoItemCommentService;
    private final LoopMemoItemService loopMemoItemService;
    private final MemoService memoService;

    /**
     * 获取循环备忘记录评论列表
     *
     * @param page       第几页
     * @param pageSize   页面大小
     * @param loopItemId 循环记录id
     * @return 循环备忘记录评论列表
     */
    @GetMapping("/{loopItemId}")
    public R<Page<LoopMemoItemComment>> getLoopMemoItemCommentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @PathVariable Integer loopItemId) {
        LoopMemoItem loopMemoItem = getCurrentUserLoopMemoItem(loopItemId); // 当前用户的循环记录
        return R.success(loopMemoItemCommentService.lambdaQuery()
                .eq(LoopMemoItemComment::getMemoId, loopMemoItem.getMemoId())
                .eq(LoopMemoItemComment::getLoopItemId, loopItemId)
                .orderByDesc(LoopMemoItemComment::getCommentDate)
                .page(new Page<>(page, pageSize))
        );
    }

    /**
     * 添加循环备忘记录评论
     *
     * @param comment 循环备忘记录评论
     * @return 循环备忘记录评论
     */
    @PostMapping
    @Transactional
    public R<LoopMemoItemComment> addLoopMemoItemComment(@RequestBody LoopMemoItemComment comment) {
        if (comment.getLoopItemId() == null) throw new FieldIsNullException("循环记录不能为空");
        LoopMemoItem loopMemoItem = getCurrentUserLoopMemoItem(comment.getLoopItemId()); // 当前用户的循环记录
        comment.setMemoId(loopMemoItem.getMemoId());
        if (comment.getCommentDate() == null) comment.setCommentDate(LocalDateTime.now());
        loopMemoItemCommentService.save(comment);
        return R.success(comment);
    }

    /**
     * 更新循环备忘记录评论
     *
     * @param comment 循环备忘记录评论
     * @return 循环备忘记录评论
     */
    @PutMapping
    public R<LoopMemoItemComment> updateLoopMemoItemComment(@RequestBody LoopMemoItemComment comment) {
        if (comment.getId() == null) throw new FieldIsNullException("评论id不能为空");
        if (comment.getMemoId() == null) throw new FieldIsNullException("备忘录不能为空");
        if (comment.getLoopItemId() == null) throw new FieldIsNullException("循环记录不能为空");
        OptimisticLockUtils.requireVersion(comment.getVersion());
        getCurrentUserLoopMemoItem(comment.getLoopItemId(), comment.getMemoId());
        boolean b = loopMemoItemCommentService.update(comment, Wrappers.<LoopMemoItemComment>lambdaUpdate()
                .eq(LoopMemoItemComment::getId, comment.getId())
                .eq(LoopMemoItemComment::getMemoId, comment.getMemoId())
                .eq(LoopMemoItemComment::getLoopItemId, comment.getLoopItemId())
        );
        OptimisticLockUtils.requireUpdated(b);
        return R.success(comment);
    }

    /**
     * 删除循环备忘记录评论
     *
     * @param memoId     备忘录id
     * @param loopItemId 循环记录id
     * @param commentId  评论id
     * @return 删除结果
     */
    @DeleteMapping("/{memoId}/{loopItemId}/{commentId}")
    public R<Boolean> deleteLoopMemoItemComment(
            @PathVariable Integer memoId,
            @PathVariable Integer loopItemId,
            @PathVariable Integer commentId,
            @RequestParam Integer version) {
        OptimisticLockUtils.requireVersion(version);
        getCurrentUserLoopMemoItem(loopItemId, memoId);
        boolean b = loopMemoItemCommentService.remove(Wrappers.<LoopMemoItemComment>lambdaUpdate()
                .eq(LoopMemoItemComment::getId, commentId)
                .eq(LoopMemoItemComment::getMemoId, memoId)
                .eq(LoopMemoItemComment::getLoopItemId, loopItemId)
                .eq(LoopMemoItemComment::getVersion, version)
        );
        OptimisticLockUtils.requireUpdated(b);
        return R.success(true);
    }

    /**
     * 获取并校验当前用户可访问的循环记录
     *
     * @param loopItemId 循环记录id
     * @return 当前用户可访问的循环记录
     */
    private LoopMemoItem getCurrentUserLoopMemoItem(Integer loopItemId) {
        LoopMemoItem loopMemoItem = loopMemoItemService.getById(loopItemId); // 循环记录
        if (loopMemoItem == null) throw new FieldIsNullException("循环记录不存在");
        return getCurrentUserLoopMemoItem(loopItemId, loopMemoItem.getMemoId());
    }

    /**
     * 获取并校验当前用户可访问的循环记录
     *
     * @param loopItemId 循环记录id
     * @param memoId     备忘录id
     * @return 当前用户可访问的循环记录
     */
    private LoopMemoItem getCurrentUserLoopMemoItem(Integer loopItemId, Integer memoId) {
        LoopMemoItem loopMemoItem = loopMemoItemService.lambdaQuery()
                .eq(LoopMemoItem::getId, loopItemId)
                .eq(LoopMemoItem::getMemoId, memoId)
                .one();
        if (loopMemoItem == null) throw new FieldIsNullException("循环记录不存在");
        Memo memo = memoService.lambdaQuery()
                .select(Memo::getId)
                .eq(Memo::getId, memoId)
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .one();
        if (memo == null) throw new FieldIsNullException("循环记录不存在");
        return loopMemoItem;
    }
}
