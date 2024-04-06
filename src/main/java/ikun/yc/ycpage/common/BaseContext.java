//仰晨study 创建时间2023/6/9 1:19 星期五
package ikun.yc.ycpage.common;

//import lombok.extern.slf4j.Slf4j;

/**
 * 基于ThreadLocal封装工具类，用户保存和获取当前登录用户id---每一次请求都是一个不同的线程
 * 为什么要搞这个东东。因为公共字段自动填充里面不能拿到用户的id值，所以要在线程里面设置一个
 */
//@Slf4j
public class BaseContext {
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    /**
     * @param id 设置当前线程的id值
     */
    public static void setCurrentId(String id) {
//        log.info("设置的当前线程id为{}",Thread.currentThread().getId());
        threadLocal.set(id);
    }

    /**
     * @return 返回当前线程保存的id值
     */
    public static String  getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 清除当前线程保存的id值，避免了内存泄漏的风险。
     *
     * @author ChenGuangLong
     * @since 2024/04/06 16:14:55
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
