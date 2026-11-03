package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序打卡评论创建请求
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
public class MiniCheckinCommentCreateRequest {
    /** 打卡记录ID */
    @NotNull(message = "打卡记录不能为空")
    private Integer recordId;

    /** 分享ID，从打卡记录列表进入时为空 */
    private Integer shareId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 200, message = "评论不能超过200字")
    private String content;

    /** 直接回复的评论ID，发表顶级评论时为空 */
    private Integer replyToCommentId;

    /** 评论人openid，由登录态注入 */
    @JsonIgnore
    private String userOpenid;
}
