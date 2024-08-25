package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private String desc;


    /**
     * 检查合法性 结束时间必须大于开始时间 总时长必须大于0 否则抛出异常  通过就设置id和userId
     *
     * @author ChenGuangLong
     * @since 2024/05/31 17:41:19
     */
    public DySeeTime checkLegal() {
        if (endTime.getTime() <= startTime.getTime() || duration < 0) throw new ParamException("传参异常");
        this.id = null;
        this.userId = BaseContext.getCurrentId();
        return this;
    }

}