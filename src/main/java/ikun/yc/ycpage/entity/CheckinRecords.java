package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ikun.yc.ycpage.common.BigDecimalScale6Deserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 微信小程序打卡记录表
 * TableName checkin_records
 */
@Data
@Accessors(chain = true)    //chain = true 生成setter方法返回this //https://blog.csdn.net/qs_xf/article/details/123876948
@TableName(value ="checkin_records")
@EqualsAndHashCode(callSuper = false)
public class CheckinRecords extends Model<CheckinRecords> {
    /** 记录ID  */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联用户openid  */
    @JsonIgnore
    private String userOpenid;

    /** 经度（-180.000000到180.000000）  */
    @JsonDeserialize(using = BigDecimalScale6Deserializer.class) // 绑定反序列化器
    @NotNull(message = "经度不能为空")
    @DecimalMin("-180.000000")
    @DecimalMax("180.000000")
    private BigDecimal longitude;

    /** 纬度（-90.000000到90.000000）  */
    @JsonDeserialize(using = BigDecimalScale6Deserializer.class) // 绑定反序列化器
    @NotNull(message = "纬度不能为空")
    @DecimalMin("-90.000000")
    @DecimalMax("90.000000")
    private BigDecimal latitude;

    /** 地点名称（如：北京故宫）  */
    private String name;

    /** 详细地址  */
    private String address;

    /** 备注（最长255个字符）  */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 打卡时间  */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    /** 更新时间  */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 删除标记 0-正常 1-删除  */
    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // 该字段只能用于接收前端数据，不能返回给前端
    private Integer isDeleted;

    /** 地点类型  */
    private String locationType;
}