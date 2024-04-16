//仰晨study 创建时间2023/12/5 1:49 星期二
package ikun.yc.ycpage.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.ControlAddItemTool;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.entity.dto.WechatDto;
import ikun.yc.ycpage.service.WechatService;
import ikun.yc.ycpage.utils.StrUtils;
import ikun.yc.ycpage.utils.VerificationCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {
    private static final Map<String, String> toDoItemMap = new HashMap<>();

    static {
        toDoItemMap.put("0 ", "普通");
        toDoItemMap.put("1 ", "循环");
        toDoItemMap.put("2 ", "长期");
        toDoItemMap.put("3 ", "紧急");
        toDoItemMap.put("4 ", "英语");
        toDoItemMap.put("5 ", "日记");
        toDoItemMap.put("6 ", "工作");
        toDoItemMap.put("7 ", "其他");
    }

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ControlAddItemTool controlAddItemTool;


    /**
     * 获取各种第三方api数据
     *
     * @param wechatDto 微信dto
     * @return 回复信息
     */
    @Override
    public String getMsg(WechatDto wechatDto) {
        setReplyType(wechatDto); // 设置回复类型

        // 处理消息
        switch (wechatDto.getReplyType()) {
            case "登录":
            case "登陆":
                return this.login(wechatDto.getFromUserName());

            case "添加待办":
                return this.addPending(wechatDto.getFromUserName(), wechatDto.getContent(), wechatDto.getReplyType());

            case "sm":
            case "说明":
                return getHelp();


            case "翻译":
                String srcText = wechatDto.getContent().substring(3).trim();
                String fyUrl = "https://findmyip.net/api/translate.php?text=" + srcText;
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

            case "fy":
            case "舔狗日记":
                try {
                    // 调用舔狗日记接口
                    return restTemplate.getForObject("https://api.likepoems.com/ana/lickdog/", String.class);
                } catch (RestClientException exception) {
                    log.error("舔狗日记接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }

            default:
                return getDefaultMsg();

            // 王者战力https://api.pearktrue.cn/api/hero/?hero=元歌&type=wx
            // 鸡汤一言https://api.lucksss.com/api/yiyan?code=json   不写code直接是字符串
            // 天气https://acid.jiuzige.com.cn/web/index/fcyWeather?city=东莞
            // 疯狂星期四https://api.pearktrue.cn/api/kfc/
            // 安慰文案https://v.api.aa1.cn/api/api-wenan-anwei/index.php?type=json
            // 爱情文案https://v.api.aa1.cn/api/api-wenan-aiqing/index.php?type=json
        }
    }


    /**
     * @param toUserName 用户名
     * @return 验证码模板
     */
    private String login(String toUserName) {
        String code;
        do {
            code = VerificationCodeUtil.generateCode();
        } while (Boolean.TRUE.equals(redisTemplate.hasKey(code)));   // 直接检查验证码是否已作为键存在

        redisTemplate.opsForValue().set(code, toUserName, 1, TimeUnit.MINUTES);          // 存储验证码和用户名的映射
        return "登录验证码为:" + code + "，一分钟内有效。\n失效后可重新发送登录获取验证码,网站地址:https://yc556.cn";
    }


    /**
     * 添加待办
     *
     * @param UserID       用户ID
     * @param content      待办内容
     * @param toDoItemType 待办类型
     * @return 成功返回id，失败返回失败原因
     * @author 仰晨
     */
    private String addPending(String UserID, String content, String toDoItemType) {
        // 检查用户是否被禁用
        if (controlAddItemTool.getOneMinuteAddItemById(UserID))
            return "添加待办过于频繁，您已被禁用添加备忘待办5分钟！";

        String[] parts = content.split("\\s", 2);                       // 使用正则表达式匹配第一个空格进行分割
        int itemType = Integer.parseInt(parts[0].trim());                         // 转换前缀得到待办类型
        ToDoItems items = new ToDoItems(UserID, parts[1], itemType);             // 待办内容和类型
        boolean save = items.insert();                                          // 保存
        if (!save) return "添加失败";
        return "添加" + toDoItemType + "待办成功 \n对该待办的增删改查请到<a href=\"https://yc556.cn\" >仰晨: https://yc556.cn</a>";
    }


    /**
     * 设置回复类型
     * 暂时除了添加待办、翻译 要修改，其他都是=内容
     * @param wechatDto 微信DTO
     */
    private void setReplyType(WechatDto wechatDto) {
        if (wechatDto.startsWithAny("翻译 ", "fy "))
            wechatDto.setReplyType("翻译");
        if (isTextInToDoItemMap(wechatDto.getContent()) != null)
            wechatDto.setReplyType("添加待办");
        wechatDto.setReplyType(wechatDto.getContent());
    }

    /**
     * 获取帮助
     *
     * @return 说明
     * @author 陈光龙
     * @since 2023/12/17
     */
    private String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("目前支持的功能有：\n");
        for (Map.Entry<String, String> entry : toDoItemMap.entrySet())
            sb.append(StrUtils.joins(entry.getKey(), "+空格+内容 => 添加", entry.getValue(), "待办\n"));

        sb.append(StrUtils.joins(msgMenu("登录"), "或", msgMenu("登陆"), " => 获取登录验证码\n",
                msgMenu("翻译 只因你太美", "翻译"), "或", msgMenu("fy hello", "fy"), "+空格+内容=>翻译内容\n",
                msgMenu("舔狗日记"), "或", msgMenu("tgrj"), " => 舔狗日记\n",
                msgMenu("说明"), "或", msgMenu("sm"), " =>显示可用功能\n",
                "仰晨主页:<a href=\"https://yc556.cn\"> https://yc556.cn</a>"
        ));

        return sb.toString();
    }

    /**
     * 获取默认消息
     *
     * @author ChenGuangLong
     * @since 2024/04/06 17:29:00
     */
    private String getDefaultMsg() {
        return StrUtils.joins("因为公众号对接了服务器，之前的回复和自定义菜单都失效了，非常抱歉" +
                        "\n如果你要登录Open备忘第一页(仰晨主页)请点击或回复", msgMenu("登录"),
                "\n如果想看现在支持的功能请输入或点击", msgMenu("说明"), " 或 ", msgMenu("sm"));
    }


    /**
     * 消息菜单
     */
    private String msgMenu(String content, String text) {
        return String.format("<a href=\"weixin://bizmsgmenu?msgmenucontent=%s&msgmenuid=0\">%s</a>", content, text);
    }

    /**
     * 消息菜单
     */
    private String msgMenu(String content) {
        return msgMenu(content, content);
    }

    /**
     * 判断用户输入是否在待办事项中
     *
     * @param text 用户信息
     * @return 开头在就返回值  不在就返回null
     * @author 仰晨
     * @since 2023-12-13
     */
    private String isTextInToDoItemMap(String text) {
        return toDoItemMap.entrySet()
                .stream()
                .filter(entry -> text.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
