package ikun.yc.ycpage.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class WechatDto {
    private String msgType;      //消息类型
    private String fromUserName; // 来源用户
    private String toUserName;   // 接收用户
    private String content;      // 内容


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
        if (this.content == null || strings == null) return false;
        for (String s : strings) return this.content.equals(s);
        return false;
    }

    /**
    检查给定字符串是否以可变参数中的任何一个字符串开头。
     * @param prefixes 可变参数，字符串数组
     * @return 如果给定字符串以可变参数中的任何一个字符串开头，则返回true，否则返回false
     */
    public boolean startsWithAny( String... prefixes) {
        if (this.content == null || prefixes == null) return false;
        for (String prefix : prefixes)
            if (this.content.startsWith(prefix)) return true;
        return false;
    }


}
