//仰晨:微信接口 创建时间2023/11/28 1:30 星期二
package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wechat")
public class WechatController {
    @Value("${wechat.token}")
    private String token;

    @Value("${wechat.encodingAesKey}")
    private String encodingAesKey;

    @Resource
    public WechatService wechatService;

    /**
     * 常规待办事项
     */
    private static final String ORDINARY = "0 ";
    /**
     * 循环待办事项
     */
    private static final String CIRCULATE = "1 ";
    /**
     * 长期待办
     */
    private static final String LONG_TERM = "2 ";
    /**
     * 紧急待办
     */
    private static final String URGENT = "3 ";

    /**
     * 单词待办
     */
    private static final String WORD = "4 ";

    /**
     * 定义前缀字符串列表,每个前缀对应不同的待办事项类型
     */
    private static final List<String> PREFIXES = Arrays.asList(ORDINARY, CIRCULATE, LONG_TERM, URGENT, WORD);



    /**
     * 处理微信服务器发送的GET请求，用于验证服务器地址
     *
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return 返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败。
     */
    @GetMapping
    public String validate(@RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) throws Exception {
        log.info("验证微信服务器地址\nsignature:{},\ntimestamp:{},\nnonce:{},\nechostr:{}", signature, timestamp, nonce, echostr);

        String[] params = new String[]{token, timestamp, nonce};
        Arrays.sort(params);

        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            sb.append(param);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(sb.toString().getBytes());
        String encryptedStr = byteArrayToHexString(digest).toLowerCase();

        System.out.println("加密后的字符串：" + encryptedStr);
        if (encryptedStr.equals(signature)) {
            System.out.println("认证通过");
            return echostr;
        } else {
            return "validation failed";
        }
    }


    /**
     * byte数组转16进制字符串
     */
    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF).toUpperCase();
            if (hex.length() < 2) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


    /**
     * 处理微信服务器发送的POST请求，用于接收和回复消息
     *
     * @param payload 微信服务器发送的POST请求内容
     * @return 返回回复消息给微信服务器
     */
    @PostMapping(produces = "text/xml; charset=UTF-8")
    public String handleMessage(@RequestBody String payload) throws Exception {
        log.info("处理微信服务器发送的POST请求，用于接收和回复消息\npayload:{}", payload);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(payload)));

        Element root = doc.getDocumentElement();
        String msgType = root.getElementsByTagName("MsgType").item(0).getTextContent();
        String fromUserName = root.getElementsByTagName("FromUserName").item(0).getTextContent();
        String toUserName = root.getElementsByTagName("ToUserName").item(0).getTextContent();

        if ("image".equals(msgType)) return createTextMessage(fromUserName, toUserName, "暂不支持图片");
        if ("voice".equals(msgType)) return createTextMessage(fromUserName, toUserName, "暂不支持语音");

        String content = root.getElementsByTagName("Content").item(0).getTextContent();
        // 处理用户输入
        String trimmedContent = content.trim();
        if ("登录".equals(trimmedContent))
            return createTextMessage(fromUserName, toUserName, wechatService.login(toUserName));

        /*
         * 下面开始待办事项
         */
        for (String prefix : PREFIXES)
            if (trimmedContent.startsWith(prefix))
                return createTextMessage(fromUserName, toUserName, wechatService.addPending(fromUserName, content, prefix));

        return createTextMessage(fromUserName, toUserName, "小黑子:ikun正在努力中...");
    }

    /**
     * 创建回复消息
     */
    private String createTextMessage(String fromUserName, String toUserName, String content) {
        return "<xml>" +
                "<ToUserName><![CDATA[" + fromUserName + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + toUserName + "]]></FromUserName>" +
                "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>" +
                "<MsgType><![CDATA[text]]></MsgType>" +
                "<Content><![CDATA[" + content + "]]></Content>" +
                "</xml>";
    }

}
