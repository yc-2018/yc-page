package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序打卡评论响应
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentResponse {
    /** 评论ID */
    private Integer id;

    /** 打卡记录ID */
    private Integer recordId;

    /** 评论内容，已删除评论为空 */
    private String content;

    /** 顶级评论ID，顶级评论为0 */
    private Integer rootId;

    /** 直接回复的评论ID，顶级评论为0 */
    private Integer replyToCommentId;

    /** 直接回复对象昵称 */
    private String replyToNickname;

    /** 评论人昵称 */
    private String nickname;

    /** 评论人头像 */
    private String avatarUrl;

    /** 是否当前用户发表 */
    private Boolean isMine;

    /** 当前用户是否可以删除 */
    private Boolean canDelete;

    /** 是否已软删除 */
    private Boolean isDeleted;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 回复数量 */
    private Integer replyCount;

    /** 是否还有更多回复 */
    private Boolean hasMoreReplies;

    /** 预览回复列表 */
    private List<MiniCheckinCommentResponse> replies;
}
