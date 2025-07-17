package ikun.yc.ycpage.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 页面参数 实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("page_parameters")
@EqualsAndHashCode(callSuper = false)
public class PageParameters extends Model<PageParameters> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页面参数ID */
    @TableId(type = IdType.AUTO)
	private Integer id;

    /** 用户ID，与users表关联 */
    @JsonIgnore
    @TableField(select = false)
    private String userId;

    /** 边宽 */
    private String borderWidth;

    /** 背景URL */
    private String backgroundUrl;

}