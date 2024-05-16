//仰晨:微信接口 创建时间2023/11/28 1:30 星期二
package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.entity.dto.WechatDto;
import ikun.yc.ycpage.service.WechatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.Arrays;

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "接收微信接口")
@RequestMapping("/接收微信接口")
public class WechatController {
    @Value("${wechat.token}")
    private String token;

//    @Value("${wechat.encodingAesKey}")
//    private String encodingAesKey;

    private final WechatService wechatService;




    /**
     * 处理微信服务器发送的GET请求，用于验证服务器地址
     *
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return 返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败。
     */
    @ApiOperation(value = "处理微信服务器发送的GET请求", notes = "用于微信验证服务器地址", httpMethod = "GET")
    @GetMapping
    public String validate(@RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) throws Exception {
        log.info("验证微信服务器地址\n signature:{},\n timestamp:{},\n nonce:{},\n echostr:{}", signature, timestamp, nonce, echostr);

        String[] params = new String[]{token, timestamp, nonce};
        Arrays.sort(params);

        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            sb.append(param);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(sb.toString().getBytes());
        String encryptedStr = byteArrayToHexString(digest).toLowerCase();

      //System.out.println("加密后的字符串：" + encryptedStr);
        if (encryptedStr.equals(signature)) {
            System.out.println("认证通过");
            return echostr;
        } else {
            return "validation failed";
        }
    }



    /**
     * 处理微信服务器发送的POST请求，用于接收和回复消息
     *
     * @param payload 微信服务器发送的POST请求内容
     * @return 返回回复消息给微信服务器
     */
    @ApiOperation(value = "接收微信信息", notes = "接收及返回微信信息", httpMethod = "POST")
    @PostMapping(produces = "text/xml; charset=UTF-8")
    public String handleMessage(@RequestBody String payload) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(payload)));

        Element root = doc.getDocumentElement();
        String msgType = root.getElementsByTagName("MsgType").item(0).getTextContent();
        String fromUserName = root.getElementsByTagName("FromUserName").item(0).getTextContent();   // 用户微信ID
        String toUserName = root.getElementsByTagName("ToUserName").item(0).getTextContent();       // 公众号ID

        WechatDto wechatDto = new WechatDto(msgType, fromUserName, toUserName, null);

        if ("image".equals(msgType)) return wechatDto.msg("暂不支持图片");
        if ("voice".equals(msgType)) return wechatDto.msg("暂不支持语音");

        // 获取用户输入内容
        String content = root.getElementsByTagName("Content").item(0).getTextContent().trim();
        wechatDto.setContent(content);

        return wechatDto.msg(wechatService.getMsg(wechatDto));
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

}
