package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 打卡记录表
 * TableName checkin_records
 */
@Data
@Accessors(chain = true)    //chain = true 生成setter方法返回this //https://blog.csdn.net/qs_xf/article/details/123876948
@TableName(value ="checkin_records")
@EqualsAndHashCode(callSuper = false)
public class CheckinRecords implements Serializable {
    /** 记录ID  */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联用户openid  */
    @JsonIgnore
    private String userOpenid;

    /** 经度（-180.000000到180.000000）  */
    private BigDecimal longitude;

    /** 纬度（-90.000000到90.000000）  */
    private BigDecimal latitude;

    /** 地点名称（如：北京故宫）  */
    private String name;

    /** 详细地址  */
    private String address;

    /** 备注（最长255字符）  */
    private String remark;

    /** 打卡时间  */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    private Date checkinTime;

    /** 更新时间  */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 仅返回给前端，不接收前端传入的数据
    private Date updateTime;

    /** 删除标记 0-正常 1-删除  */
    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // 该字段只能用于接收前端数据，不能返回给前端
    private Integer isDeleted;

    /** 地点类型  */
    private String locationType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}