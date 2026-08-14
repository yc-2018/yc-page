package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
public class OperateLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String operateUser;

    private LocalDateTime operateTime;

    private String className;

    private String methodName;

    private String methodParams;

    private String returnValue;

    private Long costTime;

}
