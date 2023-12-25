//仰晨study 创建时间2023/12/5 1:49 星期二
package ikun.yc.ycpage.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.ControlAddItemUtil;
import ikun.yc.ycpage.common.VerificationCodeUtil;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.service.ToDoItemsService;
import ikun.yc.ycpage.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ToDoItemsService toDoItemsService;
    private final RestTemplate restTemplate;
    private final ControlAddItemUtil controlAddItemUtil;

    /**
     * @param toUserName 用户名
     * @return 验证码模板
     */
    @Override
    public String login(String toUserName) {
        String code;
        do {
            code = VerificationCodeUtil.generateCode();
        } while (Boolean.TRUE.equals(redisTemplate.hasKey(code)));   // 直接检查验证码是否已作为键存在

        redisTemplate.opsForValue().set(code, toUserName,60, TimeUnit.MINUTES);          // 存储验证码和用户名的映射
        return "登录验证码为:" + code + "，五分钟内有效。\n失效后可重新发送登录获取验证码,目前网站地址:https://yc556.gitee.io";
    }



    /**
     * 添加待办
     * @author 仰晨
     * @param UserID 用户ID
     * @param content 待办内容
     * @param toDoItemType 待办类型
     * @return 成功返回id，失败返回失败原因
     */
    @Override
    public String addPending(String UserID, String content, String toDoItemType) {
        // 检查用户是否被禁用
        if (controlAddItemUtil.getOneMinuteAddItemById(UserID))
            return "添加待办过于频繁，您已被禁用添加备忘待办5分钟！";

        String[] parts = content.split("\\s", 2);                       // 使用正则表达式匹配第一个空格进行分割
        int itemType = Integer.parseInt(parts[0].trim());                         // 转换前缀得到待办类型
        ToDoItems items = new ToDoItems(UserID, parts[1], itemType);             // 待办内容和类型
        boolean save = toDoItemsService.save(items);                            // 保存
        if (!save) return "添加失败";
        return "添加" + toDoItemType + "待办成功,id：" + items.getId()+"\n对该待办的增删改查请到<a href=\"http://8.134.201.95\" >仰晨主页: https://yc556.gitee.io</a>";
    }

    /**
     * 获取各种第三方api数据
     * @param type 类型
     * @param s 内容
     * @return 返回处理后的api结果
     */
    @Override
    public String getApiData(String type, String s) {
        switch (type){
            case "翻译":
                String srcText = s.substring(3).trim();
                String fyUrl = "https://findmyip.net/api/translate.php?text="+srcText;
                try {
                    // 调用翻译接口
                    String result = restTemplate.getForObject(fyUrl, String.class);
                    // 解析接口返回结果
                    JsonNode jsonNode = new ObjectMapper().readTree(result);
                    return jsonNode.get("data").get("translate_result").asText();
                } catch (RestClientException | JsonProcessingException exception) {
                    log.error("翻译接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }
            case "舔狗日记":
                try {
                    // 调用舔狗日记接口
                    return restTemplate.getForObject("https://api.likepoems.com/ana/lickdog/", String.class);
                } catch (RestClientException exception) {
                    log.error("舔狗日记接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }

            default:
                return null;
        }
    }

    /**
     * 获取帮助
     * @param toDoItemMap 待办内容
     * @return 说明
     * @author 陈光龙
     * @since  2023/12/17
     */
    @Override
    public String getHelp(Map<String, String> toDoItemMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("目前支持的功能有：\n");
        for (Map.Entry<String, String> entry : toDoItemMap.entrySet())
            sb.append(entry.getKey()).append("+空格+内容").append(" => ").append("添加").append(entry.getValue()).append("待办").append("\n");

        sb.append("登录 或 登陆 => 获取登录验证码\n")
          .append("翻译或fy+空格+内容=>翻译内容\n")
          .append("舔狗日记 => 舔狗日记\n")
          .append("说明或sm=>给出目前支持的功能\n")
          .append("仰晨主页:<a href=\"http://8.134.201.95\"> https://yc556.gitee.io</a>");

        return sb.toString();
    }
}
