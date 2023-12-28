package ikun.yc.ycpage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ikun.yc.ycpage.entity.OperateLog;
import org.apache.ibatis.annotations.Mapper;


/**
 * 操作日志映射器
 *
 * @author ChenGuangLong
 * @since 2023/12/29 00:48:05
 */
@Mapper
public interface OperateLogMapper extends BaseMapper<OperateLog> {}
