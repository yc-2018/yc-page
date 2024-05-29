package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.entity.Memo;

import java.util.Map;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
public interface ToDoItemsService extends IService<Memo> {

    R<Integer> addItem(Memo memo);

    Map getGroupToDoItemsCount(Integer type);

    boolean updateItem(Memo toDoItem);
}
