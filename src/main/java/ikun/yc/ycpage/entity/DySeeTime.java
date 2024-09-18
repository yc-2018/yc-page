package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.dto.DateDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import static ikun.yc.ycpage.entity.enumeration.SeeTimeType.*;


/**
 * dy看时间
 *
 * @author ChenGuangLong
 * @since 2024/05/31 17:24:18
 */
@EqualsAndHashCode(callSuper = false)
@TableName(value = "dy_see_time")
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 为null的字段不返回给前端
public class DySeeTime extends Model<DySeeTime> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    @TableField(select = false)
    private Integer id;

    /** 用户微信id */
    @JsonIgnore
    @TableField(select = false)
    private String userId;

    /** 日期 */
    @TableField(exist = false)      // 不是数据库字段，但是数据库能注入到这
    private String date;            // 聚合时的日期

    /** 开始时间戳 */
    private Instant startTime;

    /** 结束时间戳 */
    private Instant endTime;

    /** 这次看页面的时长（秒） */
    private Integer thisTime;

    /** 当天总时长（开始到结束中间可能有不算的时长）包括各个页面 */
    private Integer totalDuration;

    /** 描述 */
    private String remark;


    /**
     * 检查合法性 结束时间必须大于开始时间 总时长必须大于0 否则抛出异常  通过就设置id和userId
     *
     * @author ChenGuangLong
     * @since 2024/05/31 17:41:19
     */
    public DySeeTime checkLegal() {
        if (startTime == null || thisTime == null || thisTime < 0) throw new ParamException("入参异常");
        if (endTime != null && endTime.toEpochMilli() <= startTime.toEpochMilli()) throw new ParamException("传参异常");
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
        DySeeTime sqlSeeTime = this.selectOne(Wrappers.<DySeeTime>lambdaQuery()
                .eq(DySeeTime::getStartTime, startTime)                 // 加了索引
                .eq(DySeeTime::getUserId, BaseContext.getCurrentId())   // 条件顺序不能变
        );
        if (sqlSeeTime == null) return this.insert();
        // 结束时间不能小于已存在的结束时间
        if (sqlSeeTime.getEndTime().toEpochMilli() > this.getEndTime().toEpochMilli()) {
            throw new ParamException("参数异常");
        }
        this.id = sqlSeeTime.getId();
        return this.updateById();
    }

    /**
     * 按日期获取【看时间】
     *
     * @param dateDto 日期dto
     * @return {@code List<DySeeTime>}
     */
    public List<DySeeTime> getSeeTimeByDate(DateDto dateDto) {
        // 计算两个时间的时间差
        long durationInSeconds = dateDto.getEndDate().getEpochSecond() - dateDto.getStartDate().getEpochSecond();

        if (dateDto.getSeeRange() == DAY.getValue()) {
            if (durationInSeconds > 86400L) throw new IllegalArgumentException("开始时间和结束时间不能超过一天");
            return this.selectList(Wrappers.<DySeeTime>lambdaQuery()
                    .eq(DySeeTime::getUserId, BaseContext.getCurrentId())
                    .between(DySeeTime::getStartTime, dateDto.getStartDate(), dateDto.getEndDate())
            );
        }
        if (dateDto.getSeeRange() == WEEK.getValue()) {
            if (durationInSeconds > 604800L) throw new IllegalArgumentException("开始时间和结束时间不能超过一周");
            return this.selectList(Wrappers.<DySeeTime>query()
                    .select("DATE(start_time) AS date", "SUM(this_time) AS this_time")
                    .eq("user_id", BaseContext.getCurrentId())
                    .between("start_time", dateDto.getStartDate(), dateDto.getEndDate())
                    .groupBy("DATE(start_time)")
                    .orderByAsc("DATE(start_time)")
            );
        }
        if (dateDto.getSeeRange() == MONTH.getValue()) {
            if (durationInSeconds > 2678400L) throw new IllegalArgumentException("开始时间和结束时间不能超过一个月");
            return this.selectList(Wrappers.<DySeeTime>query()
                    .select("DATE(start_time) AS date", "SUM(this_time) AS this_time")
                    .eq("user_id", BaseContext.getCurrentId())
                    .between("start_time", dateDto.getStartDate(), dateDto.getEndDate())
                    .groupBy("DATE(start_time)")
                    .orderByAsc("DATE(start_time)")
            );
        }
        if (dateDto.getSeeRange() == YEAR.getValue()) {
            if (durationInSeconds > 31622400L) throw new IllegalArgumentException("开始时间和结束时间不能超过一年");
            return this.selectList(Wrappers.<DySeeTime>query()
                    .select("DATE_FORMAT(start_time, '%Y-%m') AS date", "SUM(this_time) AS this_time")
                    .eq("user_id", BaseContext.getCurrentId())
                    .between("start_time", dateDto.getStartDate(), dateDto.getEndDate())
                    .groupBy("DATE_FORMAT(start_time, '%Y-%m')")
                    .orderByAsc("DATE_FORMAT(start_time, '%Y-%m')")
            );
        }
        return null;
    }


}
