package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.CheckinRecords;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Mapper;

/**
* author 陈光龙
* description 针对表【checkin_records(打卡记录表)】的数据库操作Service
* createDate 2025-03-08 17:13:45
*/
@Mapper
public interface CheckinRecordsService extends IService<CheckinRecords> {

}
