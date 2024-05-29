package ikun.yc.ycpage.mapper;

import ikun.yc.ycpage.entity.ToDoItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 备忘录Mapper
 *
 * @author yc
 * @since 2023-12-03 22:31:22
*/
@Mapper
public interface ToDoItemsMapper extends BaseMapper<ToDoItems> {

    // 分组统计加在标签上面 未完成的条数。但是不包括 2长期、1循环、4英语、5日记、7其他、和当前的
    @Select("SELECT item_type,count(*) " +
            "FROM to_do_items " +
            "WHERE user_id=#{userId} AND completed=0 AND item_type NOT IN (1, 2, 4, 5, 7, #{itemType}) " +
            "group by item_type")
    List<Map> selectGroupToDoItemsCount(ToDoItems toDoItems);
}
