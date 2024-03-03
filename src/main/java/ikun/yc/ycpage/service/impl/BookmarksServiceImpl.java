package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.Bookmarks;
import ikun.yc.ycpage.mapper.BookmarksMapper;
import ikun.yc.ycpage.service.BookmarksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import static ikun.yc.ycpage.controller.BookmarksController.*;

/**
 * 书签服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BookmarksServiceImpl extends ServiceImpl<BookmarksMapper, Bookmarks> implements BookmarksService {

    /**
     * 保存书签
     *
     * @param bookmarks 书签
     * @return 新增的书签id
     * @author ChenGuangLong
     * @since 2024/02/29 15:08:48
     */
    @Transactional
    @Override
    public Integer saveBookmarks(Bookmarks bookmarks) {
        String userId = BaseContext.getCurrentId();
        bookmarks.setUserId(userId);
        // 如果是书签，判断书签组是否存在 是否是当前用户的
        if (Objects.equals(bookmarks.getType(), BOOKMARK)){
            Bookmarks bookmarkGroup = this.getById(Integer.parseInt(bookmarks.getSort()));
            if (Objects.isNull(bookmarkGroup)|| !bookmarkGroup.getUserId().equals(userId)){
                throw new ParamException("书签组不存在");
            }
            this.save(bookmarks);

            // 给书签组排序字段追加上当前书签的id
            bookmarkGroup.setSort(bookmarkGroup.getSort() == null || bookmarkGroup.getSort().isEmpty() ?
                    bookmarks.getId().toString() : bookmarkGroup.getSort() + "/" + bookmarks.getId()
            );
            this.updateById(bookmarkGroup);
            // 如果增加的是书签组
        }else if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP)){
            this.save(bookmarks.setSort(null)); // 保存书签组 但是新的是不会有排序字段的
            this.update(new LambdaUpdateWrapper<Bookmarks>()
                    .eq(Bookmarks::getUserId, userId)
                    .eq(Bookmarks::getType, BOOKMARK_ROOT)
                    .setSql("sort = CASE WHEN sort IS NULL OR sort = '' THEN " + bookmarks.getId() + " ELSE CONCAT(sort, '/', " + bookmarks.getId() + ") END")
            );
        }
        return bookmarks.getId();
    }

    /**
     * 删除书签
     *
     * @param bookmarks 书签
     * @return 删除结果
     * @author ChenGuangLong
     */
    @Override
    public Integer delBookmark(Bookmarks bookmarks) {
        return null;
    }

    /**
     * 拖动排序
     *
     * @param bookmarks 书签
     * @return {@link Boolean }
     * @author ChenGuangLong
     * @since 2024/03/03 20:53:29
     */
    @Override
    public Boolean dragSort(Bookmarks bookmarks) {
        Bookmarks sqlBookmark = this.getById(bookmarks.getId());
        // 判断书签是否存在 且是当前用户的
        if (Objects.isNull(sqlBookmark) || !sqlBookmark.getUserId().equals(bookmarks.getUserId()))
            throw new ParamException("书签组不存在");

        // 排序的数据只是位置不一样
        if (!new HashSet<>(Arrays.asList(bookmarks.getSort().split("/"))).equals(
             new HashSet<>(Arrays.asList(sqlBookmark.getSort().split("/")))))
            throw new ParamException("本地数据非最新,请刷新后重试。");

        return this.updateById(sqlBookmark.setSort(bookmarks.getSort()));
    }
}