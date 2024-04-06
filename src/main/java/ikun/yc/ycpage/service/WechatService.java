//仰晨study 创建时间2023/12/5 1:48 星期二
package ikun.yc.ycpage.service;

import java.util.Map;

public interface WechatService {
    String login(String toUserName);

//    String ordinaryPending(String toUserName, String content);
//
//    String forPending(String toUserName, String content);

    String addPending(String UserID, String content, String toDoItemType);

    String getApiData(String type, String s);

    String getHelp(Map<String, String> toDoItemMap);

    String getDefaultMsg();
}
