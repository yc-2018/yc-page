package ikun.yc.ycpage.service;


import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.Bookmarks;

/**
 * 服务接口
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
public interface BookmarksService extends IService<Bookmarks> {
    /**
     * 保存书签
     *
     * @param bookmarks 书签
     * @return 新增的书签id
     * @author ChenGuangLong
     * @since 2024/02/29 15:08:48
     */
    Integer saveBookmarks(Bookmarks bookmarks);

    Integer delBookmark(Bookmarks bookmarks);

    Boolean dragSort(Bookmarks bookmarks);
}
