package ikun.yc.ycpage.entity.dto;

import ikun.yc.ycpage.utils.StrUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信订阅号Dto
 *
 * @author cgl
 * @since  2024/04/17
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class WechatDto {
    private String msgType;      //消息类型
    private String fromUserName; // 来源用户
    private String toUserName;   // 接收用户(订阅号)
    private String content;      // 内容
    private String replyType;    // 回复类型(按这个类型给用户回复)


    /**
     * 创建文字回复消息
     */
    public String msg(String content) {
        return "<xml>" +
                "<ToUserName><![CDATA[" + this.fromUserName + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + this.toUserName + "]]></FromUserName>" +
                "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>" +
                "<MsgType><![CDATA[text]]></MsgType>" +
                "<Content><![CDATA[" + content + "]]></Content>" +
                "</xml>";
    }

    /**
     * 检查给定字符串是否等于可变参数中的任何一个字符串。
     *
     * @param strings 可变参数，包含需要比较的字符串。
     * @return 如果主字符串等于数组中的任何一个字符串，则返回true；否则返回false。
     * 如果主字符串为null或数组为空，同样返回false。
     */
    public boolean eqAny(String... strings) {
        return StrUtils.eqOr(this.content, strings);
    }

    /**
    检查给定字符串是否以可变参数中的任何一个字符串开头。
     * @param prefixes 可变参数，字符串数组
     * @return 如果给定字符串以可变参数中的任何一个字符串开头，则返回true，否则返回false
     */
    public boolean startsWithAny( String... prefixes) {
        return StrUtils.startWithOr(this.content, prefixes);
    }


}
