package ikun.yc.ycpage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志
 *
 * @author ChenGuangLong
 * @since 2023/12/28 02:13:25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperateLog {
    private Integer id;                  //ID
    private String operateUser;         //操作人ID
    private LocalDateTime operateTime; //操作时间
    private String className;         //操作类名
    private String methodName;       //操作方法名
    private String methodParams;    //操作方法参数
    private String returnValue;    //操作方法返回值
    private Long costTime;        //操作耗时
}
