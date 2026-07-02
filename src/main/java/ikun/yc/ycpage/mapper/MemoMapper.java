package ikun.yc.ycpage.mapper;

import ikun.yc.ycpage.entity.Memo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ikun.yc.ycpage.entity.dto.MemoIncompleteCountDto;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 备忘录Mapper
 *
 * @author yc
 * @since 2023-12-03 22:31:22
*/
public interface MemoMapper extends BaseMapper<Memo> {

    // 分组统计加在标签上面 未完成的条数。但是不包括 1循环、2长期、4英语、5日记、7其他、和当前的
    @Select("SELECT item_type AS itemType,count(*) AS count " +
            "FROM memo " +
            "WHERE user_id=#{userId} AND completed=0 AND item_type NOT IN (1, 2, 4, 5, 7, #{itemType}) " +
            "group by item_type")
    List<MemoIncompleteCountDto> selectIncompleteCounts(Memo memo);
}
