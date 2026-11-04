package ikun.yc.ycpage.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序一次性订阅消息 Service
 *
 * @author cgl
 * @since 2026/11/05
 */
public interface MiniSubscribeMessageService {
    /**
     * 记录用户已同意接收的订阅消息模板
     *
     * @param openid      当前用户openid
     * @param templateIds 用户已同意的模板ID
     */
    void recordAcceptedTemplates(String openid, List<String> templateIds);

    /**
     * 发送评论留言通知，提醒记录主人有新评论
     *
     * @param ownerOpenid   记录主人openid
     * @param projectName   评论项目名称
     * @param commenterName 留言人昵称
     * @param content       留言内容
     * @param time          留言时间
     * @param shareId       分享ID，可为空
     * @param shareType     分享类型，可为空
     */
    void notifyNewComment(String ownerOpenid, String projectName, String commenterName,
                          String content, LocalDateTime time, Integer shareId, String shareType);

    /**
     * 发送评论回复通知，提醒被回复人有新回复
     *
     * @param targetOpenid 被回复人openid
     * @param projectName  评论项目名称
     * @param replierName  回复用户昵称
     * @param content      回复内容
     * @param time         回复时间
     * @param shareId      分享ID，可为空
     * @param shareType    分享类型，可为空
     */
    void notifyReply(String targetOpenid, String projectName, String replierName,
                     String content, LocalDateTime time, Integer shareId, String shareType);
}
