package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 设备使用日志
 *
 * @author cgl
 * @since 2025/11/12 01:32:12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "device_usage_log")
public class DeviceUsageLog extends Model<DeviceUsageLog> {
    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 用户ID */
    private String userId;
    /** App名字 */
    private String name;
    /** App包名 */
    private String packageName;
    /** 电量 */
    private Integer battery;
    /** 是否在充电 */
    private Boolean charging;
    /** 设备名 */
    private String device;
    /** 创建时间 */
    private Instant time;
}