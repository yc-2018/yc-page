//仰晨study 创建时间2023/12/5 1:48 星期二
package ikun.yc.ycpage.service;

public interface WechatService {
    String login(String toUserName);

//    String ordinaryPending(String toUserName, String content);
//
//    String forPending(String toUserName, String content);

    String addPending(String toUserName, String content, String prefix);
}
