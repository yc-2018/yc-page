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
 * (search_engines)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("search_engines")
@EqualsAndHashCode(callSuper = false)
public class SearchEngines extends Model<SearchEngines> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 搜索引擎ID  */
    @TableId(type = IdType.AUTO)
	private Integer id;

    /** 搜索引擎URL  */
    private String engineUrl;

    /** 是否快速搜索 1快 0普通  */
    private Integer isQuickSearch;

    /** 不常用 1是 0否  */
    private Integer lowUsage;

    /** 名称  */
    private String name;

    /** 图标URL  */
    private String iconUrl;

    /** 用户id  */
    @JsonIgnore
    @TableField(select = false)
    private String userId;

}