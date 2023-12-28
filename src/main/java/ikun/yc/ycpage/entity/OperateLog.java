package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * 操作日志
 *
 * @author ChenGuangLong
 * @since 2023/12/29 00:46:22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("operate_log")
@ApiModel(value="操作日志表", description="操作日志表")
public class OperateLog {

    @ApiModelProperty(value = "ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "操作人ID")
    private String operateUser;

    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operateTime;

    @ApiModelProperty(value = "操作类名")
    private String className;

    @ApiModelProperty(value = "操作方法名")
    private String methodName;

    @ApiModelProperty(value = "操作方法参数")
    private String methodParams;

    @ApiModelProperty(value = "操作方法返回值")
    private String returnValue;

    @ApiModelProperty(value = "操作耗时")
    private Long costTime;

}
