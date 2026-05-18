package ikun.yc.ycpage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ikun.yc.ycpage.entity.LoopMemoItemComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 循环备忘记录评论 Mapper 接口
 *
 * @author codex
 * @since 2026-05-19
 */
public interface LoopMemoItemCommentMapper extends BaseMapper<LoopMemoItemComment> {

    /**
     * 查询每条循环记录前几条评论，并带出评论总数
     *
     * @param loopItemIds 循环记录id列表
     * @param limitSize 每条循环记录返回的评论数量
     * @return 循环记录评论预览
     */
    List<LoopMemoItemComment> selectPreviewByLoopItemIds(@Param("loopItemIds") List<Integer> loopItemIds,
                                                         @Param("limitSize") Long limitSize);
}
