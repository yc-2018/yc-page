package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.annotation.JsonInclude;
import ikun.yc.ycpage.entity.enumeration.LinkType;
import lombok.Data;
import lombok.experimental.Accessors;

/** 用户配置 */
@Data
@Accessors(chain = true)
@TableName(value = "user_config")
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
    /** 搜索引擎排序版本号 */
    private Integer sortVersion;

    /**
     * 获取xxx排序字段数据
     *
     * @param linkType 排序类型
     * @author 𝑐𝒉𝒆𝒏GuangLong
     * @since 2025/11/05 23:20:05
     */
    public String getXxxSort(LinkType linkType) {
        switch (linkType) {
            case SEARCH:
                return this.getSearchSort();
            case LOW_SEARCH:
                return this.getLowSearchSort();
            case HOME_LINK:
                return this.getHomeBookmarkSort();
            default:
                return null;
        }
    }

    /**
     * 设置xxx排序
     *
     * @param linkType  url类型
     * @param sortField 排序字段
     * @author 𝑐𝒉𝒆𝒏GuangLong
     * @since 2025/11/06 00:07:06
     */
    public void setXxxSort(LinkType linkType, String sortField) {
        switch (linkType) {
            case SEARCH:
                this.setSearchSort(sortField);
                break;
            case LOW_SEARCH:
                this.setLowSearchSort(sortField);
                break;
            case HOME_LINK:
                this.setHomeBookmarkSort(sortField);
                break;
        }
    }

    public SFunction<UserConfig, String> getSortField(int type) {
        switch (type) {
            case 0:
                return UserConfig::getSearchSort;
            case 1:
                return UserConfig::getLowSearchSort;
            case 2:
            default:
                return UserConfig::getHomeBookmarkSort;
        }
    }
}
