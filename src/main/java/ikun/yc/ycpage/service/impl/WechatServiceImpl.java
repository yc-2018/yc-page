//仰晨study 创建时间2023/12/5 1:49 星期二
package ikun.yc.ycpage.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.ControlAddItemTool;
import ikun.yc.ycpage.entity.ToDoItems;
import ikun.yc.ycpage.entity.dto.WechatDto;
import ikun.yc.ycpage.entity.enumeration.MemoType;
import ikun.yc.ycpage.service.WechatService;
import ikun.yc.ycpage.utils.StrUtils;
import ikun.yc.ycpage.utils.VerificationCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ControlAddItemTool controlAddItemTool;


    /**
     * 回复信息
     * @param wechatDto 微信dto
     */
    @Override
    public String getMsg(WechatDto wechatDto) {
        String replyType = setReplyType(wechatDto); // 回复类型
        log.info("回复类型: " + replyType);
        // 处理消息
        switch (replyType) {
            case "登录":
            case "登陆":
                return this.login(wechatDto.getFromUserName());

            case "添加待办":
                return this.addPending(wechatDto.getFromUserName(), wechatDto.getContent());

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

            case "tgrj":
            case "舔狗日记":
                try {
                    // 调用舔狗日记接口
                    return restTemplate.getForObject("https://api.likepoems.com/ana/lickdog/", String.class);
                } catch (RestClientException exception) {
                    log.error("舔狗日记接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }

            case "yy":
            case "一言":
                try {
                    // 鸡汤一言https://api.lucksss.com/api/yiyan?code=json   不写code直接是字符串
                    return restTemplate.getForObject("https://api.lucksss.com/api/yiyan", String.class);
                } catch (RestClientException exception) {
                    log.error("一言接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }
            case "kfc":
                // 疯狂星期四文案
                String urlKfc = "https://api.pearktrue.cn/api/kfc/";
                try {
                    // 调用翻译接口
                    String result = restTemplate.getForObject(urlKfc, String.class);
                    // 解析接口返回结果
                    JsonNode jsonNode = new ObjectMapper().readTree(result);
                    return jsonNode.get("text").asText();
                } catch (RestClientException | JsonProcessingException exception) {
                    log.error("疯狂星期四接口调用失败", exception);
                    return "\uD83D\uDE2D接口失效";
                }

            default:
                return StrUtils.joins("因为公众号对接了服务器，之前的回复和自定义菜单都失效了，非常抱歉" +
                        "\n如果你要登录Open备忘第一页(仰晨主页)请点击或回复", msgMenu("登录"),
                    "\n如果想看现在支持的功能请输入或点击", msgMenu("说明"), " 或 ", msgMenu("sm"));

            //接口介绍：根据英雄名获取其语音文件https://api.pearktrue.cn/api/game/wzyp.php?msg=孙悟空  太久了 新英雄一个都没有，而且太长了
            // 王者战力https://api.pearktrue.cn/api/hero/?hero=元歌&type=wx
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
     * @return 成功返回id，失败返回失败原因
     * @author 仰晨
     */
    private String addPending(String UserID, String content) {
        // 检查用户是否被禁用
        if (controlAddItemTool.getOneMinuteAddItemById(UserID))
            return "添加待办过于频繁，您已被禁用添加备忘待办5分钟！";

        String[] parts = content.split("\\s", 2);                       // 使用正则表达式匹配第一个空格进行分割
        int itemType = Integer.parseInt(parts[0].trim());                         // 转换前缀得到待办类型
        ToDoItems items = new ToDoItems(UserID, parts[1], itemType);             // 待办内容和类型
        boolean save = items.insert();                                          // 保存
        if (!save) return "添加失败";
        return "添加" + getMemoTypeByText(content) + "待办成功 \n对该待办的增删改查请到<a href=\"https://yc556.cn\" >仰晨: https://yc556.cn</a>";
    }


    /**
     * 设置回复类型
     * 暂时除了添加待办、翻译 要修改，其他都是=内容
     * @param wechatDto 微信DTO
     */
    private String setReplyType(WechatDto wechatDto) {
        if (wechatDto.startsWithAny("翻译 ", "fy ")) return "翻译";
        if (getMemoTypeByText(wechatDto.getContent()) != null) return "添加待办";
        return wechatDto.getContent();
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
        for (MemoType value : MemoType.values())
            sb.append(StrUtils.joins(value.getCode(), "+内容 => 添加", value.getName(), "待办\n"));

        sb.append(StrUtils.joins(menuOr("登录", "登陆"), " => 获取登录验证码\n",
            menuOr("翻译 只因你太美", "翻译", "fy hello", "fy"), "+内容=>翻译内容\n",
            msgMenu("kfc"), " => 疯狂星期四文案\n",
            menuOr("舔狗日记", "tgrj"), " => 舔狗日记\n",
            menuOr("一言", "yy"), " => 随机一言\n",
            menuOr("说明", "sm"), " =>显示可用功能\n",
            "仰晨主页:<a href=\"https://yc556.cn\"> https://yc556.cn</a>"
        ));

        return sb.toString();
    }

    /** 有2个菜单时使用*/
    private String menuOr(String msg1, String msg2) {
        return StrUtils.joins(msgMenu(msg1), "或", msgMenu(msg2));
    }

    /** 有2个菜单时使用消息和回复内容不一样时使用*/
    private String menuOr(String content1, String msg1, String content2, String msg2) {
        return StrUtils.joins(msgMenu(content1,msg1), "或", msgMenu(content2,msg2));
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
    private String getMemoTypeByText(String text) {
        for (MemoType value : MemoType.values())
            if (text.startsWith(value.getCode())) return value.getName();
        return null;
    }
}
