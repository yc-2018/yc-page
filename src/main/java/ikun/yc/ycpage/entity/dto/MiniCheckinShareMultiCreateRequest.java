package ikun.yc.ycpage.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 小程序多地打卡分享创建请求
 *
 * @author cgl
 * @since 2026/10/27
 */
@Data
public class MiniCheckinShareMultiCreateRequest {
    /** 参与分享的打卡记录ID，按展示顺序传入 */
    @NotEmpty(message = "打卡记录不能为空")
    private List<Integer> recordIds;

    /** 有效期类型 */
    @NotBlank(message = "分享时长不能为空")
    private String durationType;

    /** 多地分享名称，可不传，为空时读取侧展示默认名称 */
    @Size(max = 30, message = "分享名称不能超过30字")
    private String title;

    /** 是否允许评论，默认允许 */
    private Boolean commentsEnabled;

    /** 是否公开全部评论，默认不公开；仅在允许评论时生效 */
    private Boolean commentsPublic;

    /** 创建人openid，由登录态注入 */
    @JsonIgnore
    private String userOpenid;
}
