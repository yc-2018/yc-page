package ikun.yc.ycpage.entity.dto;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.entity.Bookmarks;
import lombok.Getter;
import lombok.Setter;

/**
 * 书签 Dto
 *
 * @author yc
 */
@Setter
@Getter
public class BookmarksDto{

    /** 书签ID */
    private Integer id;

    /** 名称 */
    private String name;

    /** URL */
    private String url;

    /** 排序  书签组：所属书签排序，如：id/id/id    书签：书签组id */
    private String sort;

    /** 类型 状态0:正常书签(默认)  1:排序书签组(一个账号一个)  2:快捷图标书签 */
    private Integer type;

    /** 图标 */
    private String icon;

    public Bookmarks toBookmarks(){
        return new Bookmarks()
                .setId(id)
                .setName(name)
                .setUrl(url)
                .setSort(sort)
                .setType(type)
                .setIcon(icon)
                .setUserId(BaseContext.getCurrentId());
    }

}
