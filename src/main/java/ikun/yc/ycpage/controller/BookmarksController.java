package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.Bookmarks;
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
    @CountControl(operationType = CountControlAspect.UPDATE,controlFrequency = 10)  // 一分钟请求超出10次，禁用1分钟
    @PostMapping
    public R<Integer> addBookmarks(@RequestBody Bookmarks bookmarks) {
        if (!isaBookmarkType(bookmarks.getType())) throw new ParamException("参数有误");

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
    public R<List<Bookmarks>> getBookmarks() {
        return R.success(bookmarksService.list(new LambdaQueryWrapper<Bookmarks>()
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId()))
        );
    }


    /**
     * 更新书签
     *
     * @param bookmarks 书签
     * @return 更新结果
     * @author ChenGuangLong
     */


    /**
     * 删除书签
     *
     * @param bookmarks 书签
     * @return 删除结果
     * @author ChenGuangLong
     */




    /*****************
     * 判断是否是书签类型
     *
     * @author ChenGuangLong
     * @since 2024/02/29 19:35:09
     */
    private static boolean isaBookmarkType(Integer type) {
        return  Objects.equals(type, BOOKMARK_GROUP) ||
                Objects.equals(type, BOOKMARK) ||
                Objects.equals(type, LARGE_BOOKMARK);
    }
}