package ikun.yc.ycpage.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * (bookmarks)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 * @description 由 Mybatisplus Code Generator 创建
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("bookmarks")
public class Bookmarks extends Model<Bookmarks> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 书签ID
     */
    @TableId(type = IdType.AUTO)
	private Integer id;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 名称
     */
    private String bookmarkName;
    /**
     * URL
     */
    private String url;
    /**
     * 图标
     */
    private String icon;

}