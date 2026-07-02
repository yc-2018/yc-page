package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.dto.MemoIncompleteCountDto;
import ikun.yc.ycpage.mapper.MemoMapper;
import ikun.yc.ycpage.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 备忘录服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoServiceImpl extends ServiceImpl<MemoMapper, Memo> implements MemoService {
    private final MemoMapper memoMapper;


    /**
     * 添加待办
     * @param memo 待办事项请全体
     * @return 成功或失败或被禁用。
     */
    @Override
    public R<Integer> addItem(Memo memo) {
        String userId = BaseContext.getCurrentId();

        memo.setCreateTime(null);
        memo.setUserId(userId);

        boolean save = this.save(memo);
        return save ? R.success(memo.getId()) : R.error("添加失败");
    }



    /** 查询需要预加载展示的未完成待办数量 */
    @Override
    public List<MemoIncompleteCountDto> getIncompleteCounts(Integer currentType) {
        return memoMapper.selectIncompleteCounts(new Memo(BaseContext.getCurrentId(), currentType));
    }


    /**
     * 更新备忘录item
     *
     * @param memo 要修改的item信息
     * @return 成功与否
     */
    @Transactional
    @Override
    public boolean updateItem(Memo memo) {
        if (Objects.equals(memo.getCompleted(), 1) && Objects.isNull(memo.getOkTime())) { // 完成没提供时间，选择当前时间
            memo.setOkTime(LocalDateTime.now());
        }
        return this.update(memo, Wrappers.<Memo>lambdaUpdate()
                .eq(Memo::getId, memo.getId())
                .eq(Memo::getUserId, memo.getUserId())
        );
    }

}
