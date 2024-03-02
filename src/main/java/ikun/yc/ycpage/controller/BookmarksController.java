package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
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
    @CountControl(operationType = CountControlAspect.ADD,controlFrequency = 10)  // 一分钟请求超出10次，禁用1分钟
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
    public R<List<Bookmarks>> getBookmarks() {
        return R.success(bookmarksService.list(new LambdaQueryWrapper<Bookmarks>()
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId()))
        );
    }

    /**
     * 拖动排序
     * @param bookmarksDto 书签dto
     * @return {@link R }<{@link Boolean }>
     * @author ChenGuangLong
     * @since 2024/03/02 02:38:14
     */

    @PutMapping("/dragSort")
    public R<Boolean> dragSort(@RequestBody BookmarksDto bookmarksDto) {
        if (!(Objects.equals(bookmarksDto.getType(), BOOKMARK_GROUP) ||
              Objects.equals(bookmarksDto.getType(), BOOKMARK_ROOT)))
            throw new ParamException("参数有误");

        return R.success(bookmarksService.update(new LambdaUpdateWrapper<Bookmarks>()
                .set(Bookmarks::getSort, bookmarksDto.getSort())
                .eq(Bookmarks::getId, bookmarksDto.getId())
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
    @Log
    @CountControl(operationType = CountControlAspect.DELETE,controlFrequency = 30)  // 一分钟请求超出30次，禁用1分钟
    @DeleteMapping
    public R<Integer> deleteBookmarks(@RequestBody Bookmarks bookmarks) {
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