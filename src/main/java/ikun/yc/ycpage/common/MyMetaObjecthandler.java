//仰晨study 创建时间2023/6/9 0:52 星期五
package ikun.yc.ycpage.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * 自定义元数据对象处理器
 */
//@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]");
        log.info(metaObject.toString());
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());
//        metaObject.setValue("ikun", "测试......");//类不存在的属性------会报错
//        log.info("ikun.text");
        /*
        * 如果您的类中新增了一个公共字段属性，需要在插入和更新时进行自动填充，可以将代码修改如下：
        * 首先，在insertFill()方法中添加对新字段的赋值，注意使用 if (metaObject.hasSetter("newField")) 判断是否存在该属性的 setter 方法，从而避免报错。
        * if (metaObject.hasSetter("newField")) {
        *     metaObject.setValue("newField", "默认值或其他逻辑");
        * }
        * */
//        metaObject.setValue("createUser", BaseContext.getCurrentId());
//        metaObject.setValue("updateUser", BaseContext.getCurrentId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]");
        log.info(metaObject.toString());
        metaObject.setValue("updateTime", LocalDateTime.now());
//        metaObject.setValue("updateUser", BaseContext.getCurrentId());
    }
}
