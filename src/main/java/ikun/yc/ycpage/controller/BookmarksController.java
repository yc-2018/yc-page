package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.*;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.Bookmarks;
import ikun.yc.ycpage.entity.dto.BookmarksDto;
import ikun.yc.ycpage.service.BookmarksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 书签控制层
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/bookmarks")
public class BookmarksController {
    public static final Integer LARGE_BOOKMARK = 3;   // 大（图标）书签
    public static final Integer BOOKMARK = 2;         // 书签
    public static final Integer BOOKMARK_GROUP = 1;   // 书签组
    public static final Integer BOOKMARK_ROOT = 0;   // 书签组的排序书签

    private  final BookmarksService bookmarksService;

    /**
     * 新增书签|组
     *
     * @param bookmarks 书签
     * @return 新增的书签id
     * @author ChenGuangLong
     * @since 2024/02/29 15:08:48
     */
    @Log
    @DelCache   // 删除缓存
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)  // 一分钟请求超出10次，禁用1分钟
    @PostMapping
    public R<Integer> addBookmarks(@RequestBody Bookmarks bookmarks) {
        if (!dataVerification(bookmarks)) throw new ParamException("参数有误");

        return R.success(bookmarksService.saveBookmarks(bookmarks.setId(null)));
    }


    /**
     * 获取书签
     *
     * @return 当前用户的所有书签
     * @author ChenGuangLong
     * @since 2024/02/29 19:44
     */
    @GetMapping
    @RedisCache // 缓存
    public R<List<Bookmarks>> getBookmarks() {
        return R.success(
            bookmarksService.lambdaQuery().eq(Bookmarks::getUserId, BaseContext.getCurrentId()).list()
        );
    }

    /**
     * 拖动排序
     * @param bookmarksDto 书签dto
     * @return {@link R }<{@link Boolean }>
     * @author ChenGuangLong
     * @since 2024/03/02 02:38:14
     */
    @DelCache   // 删除缓存
    @PutMapping("/dragSort")
    public R<Boolean> dragSort(@RequestBody BookmarksDto bookmarksDto) {
        if (!(Objects.equals(bookmarksDto.getType(), BOOKMARK_GROUP) ||
              Objects.equals(bookmarksDto.getType(), BOOKMARK_ROOT)))
            throw new ParamException("参数有误");

        return R.success(bookmarksService.dragSort(bookmarksDto.toBookmarks()));
    }

    /**
     * 修改书签
     *
     * @param bookmarksDto 要修改的书签
     * @author ChenGuangLong
     * @since 2024/03/05 18:16:15
     */
    @Log
    @DelCache   // 删除缓存
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)  // 一分钟请求超出10次，禁用1分钟
    @PutMapping
    public R<Boolean> updateBookmarks(@RequestBody BookmarksDto bookmarksDto) {
        if (!(Objects.equals(bookmarksDto.getType(), BOOKMARK_GROUP) ||
              Objects.equals(bookmarksDto.getType(), BOOKMARK)))
            throw new ParamException("参数有误");

        return R.success(
            bookmarksService.lambdaUpdate()
                .eq(Bookmarks::getId, bookmarksDto.getId())
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId())
                .set(Bookmarks::getName, bookmarksDto.getName())
                .set(Bookmarks::getUrl, bookmarksDto.getUrl())
                .set(Bookmarks::getIcon, bookmarksDto.getIcon())
                .update()
        );
    }


    /**
     * 删除书签
     *
     * @param bookmarks 书签
     * @return 删除结果
     * @author ChenGuangLong
     */
    @Log
    @UserId
    @DelCache   // 删除缓存
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 30)  // 一分钟请求超出30次，禁用1分钟
    @DeleteMapping
    public R<Boolean> deleteBookmarks(@RequestBody Bookmarks bookmarks) {
        if (!dataVerification(bookmarks)) throw new ParamException("参数有误");

        return R.success(bookmarksService.delBookmark(bookmarks));
    }




    /*****************
     * 判断是否是书签类型
     * 排序字符串要符合规则
     *
     * @author ChenGuangLong
     * @since 2024/02/29 19:35:09
     */
    private boolean dataVerification(Bookmarks bookmarks) {
        if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP) ||      // 判断类型
            Objects.equals(bookmarks.getType(), BOOKMARK) ||
            Objects.equals(bookmarks.getType(), LARGE_BOOKMARK)){
            if (Objects.equals(bookmarks.getType(), BOOKMARK))          // 判断书签排序字段
                return bookmarks.getSort().matches("^\\d+$");
            return matchesPattern(bookmarks.getSort());                 // 书签组||大书签 字段
        }
        return false;
    }

    /*****************
     * 排序字符串要符合规则
     * 5个数字 或 5个数字/5个数字 或 5个数字/5个数字/5个数字...
     *
     * @param sortStr 排序str
     * @return boolean
     * @author ChenGuangLong
     * @since 2024/03/01 17:19:53
     */
    private boolean matchesPattern(String sortStr) {
        if (sortStr == null || sortStr.isEmpty()) return true;
        String regex = "^(\\d{1,5})(/\\d{1,5})*$";
        return sortStr.matches(regex);
    }
}