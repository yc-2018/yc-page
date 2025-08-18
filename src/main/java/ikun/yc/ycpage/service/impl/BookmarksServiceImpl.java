package ikun.yc.ycpage.service.impl;

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
import org.springframework.util.StringUtils;

import java.util.*;

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
            bookmarkGroup.setSort(StringUtils.hasText(bookmarkGroup.getSort()) ?
                    bookmarkGroup.getSort() + "/" + bookmarks.getId() : bookmarks.getId().toString()
            );
            this.updateById(bookmarkGroup);
            // 如果增加的是书签组
        }else if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP)){
            bookmarks.setSort(null).insert();   // 保存书签组 但是新的是不会有排序字段的

            this.lambdaUpdate()
                .eq(Bookmarks::getUserId, userId)
                .eq(Bookmarks::getType, BOOKMARK_ROOT)
                .setSql("sort = CASE WHEN sort IS NULL OR sort = '' THEN " + bookmarks.getId() + " ELSE CONCAT(sort, '/', " + bookmarks.getId() + ") END")
                .update();
        }
        return bookmarks.getId();
    }

    /**
     *
     *
     * @param bookmarks 书签
     * @return 删除结果
     * @author ChenGuangLong
     */
    @Transactional
    @Override
    public Boolean delBookmark(Bookmarks bookmarks) {
        if (Objects.equals(bookmarks.getType(), BOOKMARK)){
            // 如果是书签，判断书签组是否存在 是否是当前用户的
            Bookmarks bookmarkGroup = this.getById(Integer.parseInt(bookmarks.getSort()));
            if (Objects.isNull(bookmarkGroup)|| !bookmarkGroup.getUserId().equals(bookmarks.getUserId()))
                throw new ParamException("参数存在错误,或页面数据不是最新的！");
            // 书签组排序字段删除当前要删除的书签
            String newSort = reduceSortString(bookmarkGroup.getSort(), bookmarks.getId());
            this.lambdaUpdate()
                .eq(Bookmarks::getUserId, bookmarkGroup.getUserId())
                .eq(Bookmarks::getId, bookmarkGroup.getId())
                .set(Bookmarks::getSort, newSort)
                .update();
            return this.removeById(bookmarks.getId());

            // 如果删除的是书签组
        }else if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP)){
            Bookmarks bookmarkRoot = this.lambdaQuery()
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId())
                .eq(Bookmarks::getType, BOOKMARK_ROOT)
                .one();
            String newSort = reduceSortString(bookmarkRoot.getSort(), bookmarks.getId());
            // 更新根排序
            this.lambdaUpdate()
                .eq(Bookmarks::getId, bookmarkRoot.getId())
                .set(Bookmarks::getSort, newSort)
                .update();

            // 删除书签组和它的子书签
            return this.lambdaUpdate()
                .eq(Bookmarks::getUserId, bookmarkRoot.getUserId())
                .eq(Bookmarks::getType, BOOKMARK)
                .eq(Bookmarks::getSort, bookmarks.getId().toString())
                .or()
                .eq(Bookmarks::getId, bookmarks.getId())
                .remove();

        }
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
        Bookmarks sqlBookmark = bookmarks.selectById();
        // 判断书签是否存在 且是当前用户的
        if (Objects.isNull(sqlBookmark) || !sqlBookmark.getUserId().equals(bookmarks.getUserId()))
            throw new ParamException("书签组不存在");

        // 排序的数据只是位置不一样
        if (!new HashSet<>(Arrays.asList(bookmarks.getSort().split("/"))).equals(
             new HashSet<>(Arrays.asList(sqlBookmark.getSort().split("/")))))
            throw new ParamException("本地数据非最新,请刷新后重试。");

        return sqlBookmark.setSort(bookmarks.getSort()).updateById();
    }

    /**
     * 从排序字符串减去要删除的id
     *
     * @param sortString 排序字符串
     * @param idToRemove 要删除id
     * @author ChenGuangLong
     * @since 2024/03/05 00:32:39
     */
    private String reduceSortString(String sortString, int idToRemove) {
        // 将sortString按'/'分割
        List<String> parts = new ArrayList<>(Arrays.asList(sortString.split("/")));

        // 将idToRemove转换为字符串
        String idStr = String.valueOf(idToRemove);

        // 检查是否包含idToRemove
        if (!parts.contains(idStr)) throw new IllegalArgumentException("书签不存在");//排序字符串不包含指定的ID

        // 移除指定的ID
        parts.remove(idStr);

        // 根据剩余部分重组sortString
        if (parts.isEmpty()) return ""; // 或根据实际需求返回null或其他值
         else return String.join("/", parts);

    }
}