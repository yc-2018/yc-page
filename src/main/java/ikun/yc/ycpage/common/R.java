package ikun.yc.ycpage.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
/**通用返回结果，服务端响应的数据最终都会封装成此对象Result*/
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    private Integer code; //编码：1成功，0和其它数字为失败

    private Boolean success; // 成功true 其他null 方便前端判断

    private String msg; //错误信息

    private T data; //数据

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> success(T object) {
        R<T> r = new R<>();
        r.success = true;
        r.data = object;
        r.code = 1;
        return r;
    }


    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.msg = msg;
        r.code = 0;
        return r;
    }

}
