package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

/** 用户配置 */
@Data
@Accessors(chain = true)
@TableName(value ="user_config")
@JsonInclude(JsonInclude.Include.NON_EMPTY)              // 如果为空字符串或 null，这个字段不会返回给前端
public class UserConfig {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 用户ID，与users表关联 */
    private String userId;
    /** 背景URL */
    private String backgroundUrl;
    /** 搜索引擎排序 id/id/id */
    private String searchSort;
    /** 不常用的搜索引擎排序 id/id/id */
    private String lowSearchSort;
    /** 首页大图标书签排序  id/id/id */
    private String homeBookmarkSort;
}