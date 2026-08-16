package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import ikun.yc.ycpage.entity.LoopMemoItem;
import ikun.yc.ycpage.entity.LoopMemoItemComment;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferRequest;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferResponse;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.exception.FieldIsNullException;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.mapper.LoopMemoItemMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.service.LoopMemoItemCommentService;
import ikun.yc.ycpage.service.LoopMemoItemService;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 目前作为:循环代办的历史时间。 服务实现类
 * </p>
 *
 * @author chengguanglong
 * @since 2023-12-20
 */
@Service
@RequiredArgsConstructor
public class LoopMemoItemServiceImpl extends ServiceImpl<LoopMemoItemMapper, LoopMemoItem> implements LoopMemoItemService {

    private static final int LOOP_MEMO_TYPE = 1; // 循环备忘类型
    private static final int DELETED_COMPLETED_LIMIT = 10; // completed 大于等于该值表示已删除

    private final MemoService memoService;
    private final LoopMemoItemCommentService loopMemoItemCommentService;

    /**
     * 转移循环备忘记录到另一个循环备忘
     *
     * @param request 转移请求
     * @return 转移结果和两边最新循环次数
     */
    @Override
    @Transactional
    public LoopMemoItemTransferResponse transferLoopMemoItems(LoopMemoItemTransferRequest request) {
        validateTransferRequest(request);
        OptimisticLockUtils.requireVersion(request.getSourceMemoVersion());
        OptimisticLockUtils.requireVersion(request.getTargetMemoVersion());
        Memo sourceMemo = getCurrentUserLoopMemo(request.getSourceMemoId()); // 源循环备忘
        Memo targetMemo = getCurrentUserLoopMemo(request.getTargetMemoId()); // 目标循环备忘
        if (!request.getSourceMemoVersion().equals(sourceMemo.getVersion())
                || !request.getTargetMemoVersion().equals(targetMemo.getVersion())) {
            throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        }
        List<Integer> loopItemIds = request.getLoopItemIds(); // 要转移的循环记录主键列表
        Set<Integer> distinctLoopItemIds = new HashSet<>(loopItemIds); // 去重后的循环记录主键

        long sourceItemCount = this.lambdaQuery()
                .eq(LoopMemoItem::getMemoId, sourceMemo.getId())
                .in(LoopMemoItem::getId, distinctLoopItemIds)
                .count(); // 属于源循环备忘的记录数量
        if (sourceItemCount != distinctLoopItemIds.size()) throw new FieldIsNullException("循环记录不存在");

        LoopMemoItem itemUpdateEntity = new LoopMemoItem(); // 触发循环记录更新时间自动填充的实体
        boolean itemUpdate = this.update(itemUpdateEntity, Wrappers.<LoopMemoItem>lambdaUpdate()
                .in(LoopMemoItem::getId, distinctLoopItemIds)
                .eq(LoopMemoItem::getMemoId, sourceMemo.getId())
                .set(LoopMemoItem::getMemoId, targetMemo.getId())
        ); // 循环记录归属更新结果
        if (!itemUpdate) throw new ParamException("转移失败");

        LoopMemoItemComment commentUpdateEntity = new LoopMemoItemComment(); // 触发评论更新时间自动填充的实体
        loopMemoItemCommentService.update(commentUpdateEntity, Wrappers.<LoopMemoItemComment>lambdaUpdate()
                .in(LoopMemoItemComment::getLoopItemId, distinctLoopItemIds)
                .eq(LoopMemoItemComment::getMemoId, sourceMemo.getId())
                .set(LoopMemoItemComment::getMemoId, targetMemo.getId())
        );

        int sourceCount = refreshMemoLoopCount(sourceMemo, request.getSourceMemoVersion()); // 源循环备忘最新次数
        int targetCount = refreshMemoLoopCount(targetMemo, request.getTargetMemoVersion()); // 目标循环备忘最新次数

        LoopMemoItemTransferResponse response = new LoopMemoItemTransferResponse(); // 转移响应
        response.setSourceMemoId(sourceMemo.getId());
        response.setTargetMemoId(targetMemo.getId());
        response.setMovedCount(distinctLoopItemIds.size());
        response.setSourceNumberOfRecurrences(sourceCount);
        response.setTargetNumberOfRecurrences(targetCount);
        response.setSourceMemoVersion(sourceMemo.getVersion());
        response.setTargetMemoVersion(targetMemo.getVersion());
        return response;
    }

    /**
     * 校验循环记录转移请求基础参数
     *
     * @param request 转移请求
     */
    private void validateTransferRequest(LoopMemoItemTransferRequest request) {
        if (request == null) throw new FieldIsNullException("转移参数不能为空");
        if (request.getSourceMemoId() == null) throw new FieldIsNullException("源循环备忘不能为空");
        if (request.getTargetMemoId() == null) throw new FieldIsNullException("目标循环备忘不能为空");
        if (Objects.equals(request.getSourceMemoId(), request.getTargetMemoId())) throw new ParamException("不能转移到原循环备忘");
        if (request.getLoopItemIds() == null || request.getLoopItemIds().isEmpty()) throw new FieldIsNullException("请选择循环记录");
        if (request.getLoopItemIds().stream().anyMatch(Objects::isNull)) throw new FieldIsNullException("循环记录不能为空");
    }

    /**
     * 获取当前用户可访问的循环备忘
     *
     * @param memoId 循环备忘主键
     * @return 当前用户循环备忘
     */
    private Memo getCurrentUserLoopMemo(Integer memoId) {
        Memo memo = memoService.lambdaQuery()
                .select(Memo::getId, Memo::getItemType, Memo::getUserId, Memo::getVersion)
                .eq(Memo::getId, memoId)
                .eq(Memo::getUserId, BaseContext.getCurrentId())
                .lt(Memo::getCompleted, DELETED_COMPLETED_LIMIT)
                .one(); // 当前用户备忘
        if (memo == null || !Objects.equals(memo.getItemType(), LOOP_MEMO_TYPE)) throw new FieldIsNullException("循环备忘不存在");
        return memo;
    }

    /**
     * 刷新循环备忘冗余次数
     */
    private int refreshMemoLoopCount(Memo memo, Integer expectedVersion) {
        Integer memoId = memo.getId();
        int count = Math.toIntExact(this.lambdaQuery()
                .eq(LoopMemoItem::getMemoId, memoId)
                .count()); // 最新循环次数
        if (!expectedVersion.equals(memo.getVersion())) throw new ikun.yc.ycpage.common.exception.OptimisticLockException();
        memo.setNumberOfRecurrences(count);
        OptimisticLockUtils.requireUpdated(memoService.updateById(memo));
        return count;
    }
}
