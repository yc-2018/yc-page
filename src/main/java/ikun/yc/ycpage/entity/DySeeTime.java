package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * dy看时间
 *
 * @author ChenGuangLong
 * @since 2024/05/31 17:24:18
 */
@EqualsAndHashCode(callSuper = false)
@TableName(value ="dy_see_time")
@NoArgsConstructor
@Data
public class DySeeTime extends Model<DySeeTime> implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户微信id */
    private String userId;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 当天总时长（开始到结束中间可能有不算的时长）包括各个页面 */
    private Integer duration;

    /** 描述 */
    private String remark;


    /**
     * 检查合法性 结束时间必须大于开始时间 总时长必须大于0 否则抛出异常  通过就设置id和userId
     *
     * @author ChenGuangLong
     * @since 2024/05/31 17:41:19
     */
    public DySeeTime checkLegal() {
        if (startTime == null || duration == null || duration < 0) throw new ParamException("入参异常");
        if (endTime != null && endTime.getTime() <= startTime.getTime()) throw new ParamException("传参异常");
        this.id = null;
        this.userId = BaseContext.getCurrentId();
        return this;
    }

    /**
     * 插入或更新seeTime数据 并返回结果
     * 先根据ID和时间判断是否存在，存在则更新，不存在则插入
     *
     * @return boolean
     * @author ChenGuangLong
     * @since 2024/08/28 23:02:02
     */
    public boolean updateOrInsert() {
        // 忽略毫秒（MySQL 中的时间戳则是以秒为单位的）
        startTime = new Date(startTime.getTime() / 1000 * 1000);

        DySeeTime sqlSeeTime = this.selectOne(Wrappers.<DySeeTime>lambdaQuery()
                .eq(DySeeTime::getUserId, BaseContext.getCurrentId())
                .eq(DySeeTime::getStartTime, startTime)
        );
        if (sqlSeeTime == null) {
            return this.insert();
        }
        return this.updateById();
    }

}