package ikun.yc.ycpage.mapper;

import ikun.yc.ycpage.entity.ToDoItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * (to_do_items)数据Mapper
 *
 * @author yc
 * @since 2023-12-03 22:31:22
*/
@Mapper
public interface ToDoItemsMapper extends BaseMapper<Object> {

    // 分组统计加在标签上面 未完成的条数。
    @Select("SELECT item_type,count(*) FROM to_do_items WHERE user_id=#{userId} AND completed=0 AND item_type NOT IN (4, #{type}) group by item_type")
    List<Map> selectGroupToDoItemsCount(String userId, Integer type);
}
