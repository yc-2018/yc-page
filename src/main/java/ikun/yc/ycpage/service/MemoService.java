package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Memo;
import ikun.yc.ycpage.entity.dto.MemoIncompleteCountDto;

import java.util.List;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
public interface MemoService extends IService<Memo> {

    R<Integer> addItem(Memo memo);

    List<MemoIncompleteCountDto> getIncompleteCounts(Integer currentType);

    void fillMemoTags(List<Memo> memos);

    boolean updateItem(Memo memo);

    /** 按版本逻辑删除当前用户备忘录。 */
    boolean deleteItem(Integer id, Integer version);
}
