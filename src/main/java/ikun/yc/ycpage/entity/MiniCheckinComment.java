package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小程序打卡评论表实体，评论跟随打卡记录而不是分享
 *
 * @author cgl
 * @since 2026/11/03
 */
@Data
@TableName("mini_checkin_comment")
public class MiniCheckinComment {
    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 打卡记录ID */
    private Integer recordId;

    /** 评论人openid */
    @JsonIgnore
    private String userOpenid;

    /** 评论内容 */
    private String content;

    /** 顶级评论ID，顶级评论为0 */
    private Integer rootId;

    /** 直接回复的评论ID，顶级评论为0 */
    private Integer replyToCommentId;

    /** 直接回复对象openid，顶级评论为空字符串 */
    private String replyToOpenid;

    /** 软删除标记 0-正常 1-已删除 */
    @JsonIgnore
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
