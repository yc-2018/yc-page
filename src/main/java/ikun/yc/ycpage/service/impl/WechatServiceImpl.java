//仰晨study 创建时间2023/12/5 1:49 星期二
package ikun.yc.ycpage.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ToDoItemsService toDoItemsService;
    private final RestTemplate restTemplate;

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
     * @param fromUserName 用户名
     * @param content 待办内容
     * @param prefix 为了选择加上的字符串前缀的长度（要去掉前缀的长度）
     * @return 成功返回id，失败返回失败原因
     */
    @Override
    public String addPending(String fromUserName, String content, String prefix) {
        // 待办类型映射关系列表
        List<String> pendingNames = Arrays.asList("普通", "循环", "长期","紧急","英语","日记待办","工作待办");
        content=content.trim().substring(prefix.length());                       // 去掉空格和为了选择加上的字符串前缀
        int itemType = Integer.parseInt(prefix.trim());                         // 转换前缀得到待办类型
        ToDoItems items = new ToDoItems(fromUserName, content, itemType);      // 待办内容和类型
        boolean save = toDoItemsService.save(items);                          // 保存
        if (!save) return "添加失败";
        return "添加"+ pendingNames.get(itemType)+"待办成功,id：" + items.getId();
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
                String url = "https://findmyip.net/api/translate.php?text="+srcText;
                try {
                    // 调用翻译接口
                    String result = restTemplate.getForObject(url, String.class);
                    // 解析接口返回结果
                    JsonNode jsonNode = new ObjectMapper().readTree(result);
                    return jsonNode.get("data").get("translate_result").asText();
                } catch (RestClientException | JsonProcessingException exception) {
                    log.error("翻译接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }
            case "舔狗日记":
                return "舔狗。";
            default:
                return null;
        }
    }
}
