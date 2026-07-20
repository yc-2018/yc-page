package ikun.yc.ycpage.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.exception.OptimisticLockException;
import ikun.yc.ycpage.common.exception.SqlUpdateException;
import ikun.yc.ycpage.entity.LoopMemoItem;
import ikun.yc.ycpage.entity.LoopMemoItemComment;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferRequest;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferResponse;
import ikun.yc.ycpage.mapper.LoopMemoItemCommentMapper;
import ikun.yc.ycpage.service.LoopMemoItemCommentService;
import ikun.yc.ycpage.service.LoopMemoItemService;
import ikun.yc.ycpage.service.MemoService;
import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final long DEFAULT_COMMENT_PAGE_SIZE = 5L; // 每条循环记录默认返回的评论数量

    private final LoopMemoItemService loopMemoItemService;
    private final LoopMemoItemCommentService loopMemoItemCommentService;
    private final LoopMemoItemCommentMapper loopMemoItemCommentMapper;
    private final MemoService memoService;

    /**
     * 批量转移循环备忘记录
     *
     * @param request 转移请求
     * @return 转移结果
     */
    @PutMapping("/transfer")
    public R<LoopMemoItemTransferResponse> transferLoopMemoItems(@RequestBody LoopMemoItemTransferRequest request) {
        return R.success(loopMemoItemService.transferLoopMemoItems(request));
    }

    /**
     * 获取循环备忘记录列表
     *
     * @param q        搜索关键字
     * @param page     第几页
     * @param pageSize 页面大小
     * @param itemId   待办id
     * @return 待办时间列表
     * @author ChenGuangLong
     * @since 2024/01/02 19:45:50
     */
    @GetMapping("/{itemId}")
    public R<Page<LoopMemoItem>> getLoopMemoItemList(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @PathVariable Integer itemId) {
        Page<LoopMemoItem> result = loopMemoItemService.lambdaQuery()
                .eq(LoopMemoItem::getMemoId, itemId)
                .like(Strings.hasText(q), LoopMemoItem::getLoopText, q)
                .orderByDesc(LoopMemoItem::getMemoDate)
                .page(new Page<>(page, pageSize))
        ;
        fillCommentPreview(result.getRecords());
        return R.success(result);
    }

    /**
     * 给循环记录补充前5条评论，避免前端首次展示时逐条请求
     *
     * @param records 循环记录列表
     */
    private void fillCommentPreview(List<LoopMemoItem> records) {
        if (records == null || records.isEmpty()) return;
        List<Integer> loopItemIds = records.stream()
                .map(LoopMemoItem::getId)
                .collect(Collectors.toList()); // 本页循环记录id
        List<LoopMemoItemComment> comments = loopMemoItemCommentMapper.selectPreviewByLoopItemIds(loopItemIds, DEFAULT_COMMENT_PAGE_SIZE);
        Map<Integer, List<LoopMemoItemComment>> commentMap = comments.stream()
                .collect(Collectors.groupingBy(LoopMemoItemComment::getLoopItemId)); // 按循环记录分组的评论
        records.forEach(record -> {
            List<LoopMemoItemComment> itemComments = commentMap.getOrDefault(record.getId(), java.util.Collections.emptyList());
            long total = itemComments.stream()
                    .findFirst()
                    .map(LoopMemoItemComment::getCommentTotal)
                    .orElse(0L); // 当前循环记录评论总数
            itemComments.forEach(comment -> comment.setCommentTotal(null));
            record.setComments(itemComments);
            record.setCommentTotal(total);
            record.setCommentHasMore(total > DEFAULT_COMMENT_PAGE_SIZE);
        });
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
        OptimisticLockUtils.requireVersion(loopMemoItem.getMemoVersion());
        Memo memo = getCurrentMemo(loopMemoItem.getMemoId());
        if (!memo.getVersion().equals(loopMemoItem.getMemoVersion())) {
            throw new OptimisticLockException();
        }
        loopMemoItemService.save(loopMemoItem);
        memo.setNumberOfRecurrences(Math.toIntExact(loopMemoItemService.lambdaQuery()
                .eq(LoopMemoItem::getMemoId, loopMemoItem.getMemoId()).count()));
        OptimisticLockUtils.requireUpdated(memoService.updateById(memo));
        loopMemoItem.setMemoVersion(memo.getVersion());
        return R.success(loopMemoItem);
    }


    /** 更新循环备忘录 */
    @PutMapping
    public R<LoopMemoItem> updateLoopMemoItem(@RequestBody LoopMemoItem loopMemoItem) {
        OptimisticLockUtils.requireVersion(loopMemoItem.getVersion());
        getCurrentMemo(loopMemoItem.getMemoId());
        boolean b = loopMemoItemService.update(loopMemoItem, Wrappers.<LoopMemoItem>lambdaUpdate()
                .eq(LoopMemoItem::getId, loopMemoItem.getId())
                .eq(LoopMemoItem::getMemoId, loopMemoItem.getMemoId())
        );
        OptimisticLockUtils.requireUpdated(b);
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
    public R<Boolean> deleteLoopMemoItem(@PathVariable Integer memoId, @PathVariable Integer loopId,
                                         @RequestParam Integer version, @RequestParam Integer memoVersion) {
        OptimisticLockUtils.requireVersion(version);
        OptimisticLockUtils.requireVersion(memoVersion);
        Memo memo = getCurrentMemo(memoId);
        if (!memo.getVersion().equals(memoVersion)) {
            throw new OptimisticLockException();
        }
        // 删除循环备忘项
        boolean removed = loopMemoItemService.remove(Wrappers.<LoopMemoItem>lambdaUpdate()
                .eq(LoopMemoItem::getId, loopId)
                .eq(LoopMemoItem::getMemoId, memoId)
                .eq(LoopMemoItem::getVersion, version)
        );
        OptimisticLockUtils.requireUpdated(removed);

        // 删除循环备忘项下的第三层评论
        loopMemoItemCommentService.remove(Wrappers.<LoopMemoItemComment>lambdaUpdate()
                .eq(LoopMemoItemComment::getLoopItemId, loopId)
                .eq(LoopMemoItemComment::getMemoId, memoId)
        );

        // 待办减一
        memo.setNumberOfRecurrences(Math.toIntExact(loopMemoItemService.lambdaQuery()
                .eq(LoopMemoItem::getMemoId, memoId).count()));
        boolean b = memoService.updateById(memo);
        OptimisticLockUtils.requireUpdated(b);

        return R.success(b);
    }

    /** 查询并校验当前用户的备忘录。 */
    private Memo getCurrentMemo(Integer memoId) {
        Memo memo = memoService.lambdaQuery()
                .select(Memo::getId, Memo::getUserId, Memo::getVersion, Memo::getNumberOfRecurrences)
                .eq(Memo::getId, memoId)
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .one();
        if (memo == null) throw new SqlUpdateException("备忘录不存在");
        return memo;
    }

}
