package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.OptimisticLockUtils;
import ikun.yc.ycpage.common.exception.OptimisticLockException;
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
        OptimisticLockUtils.requireVersion(bookmarks.getParentVersion());
        // 如果是书签，判断书签组是否存在 是否是当前用户的
        if (Objects.equals(bookmarks.getType(), BOOKMARK)){
            Bookmarks bookmarkGroup = this.getById(Integer.parseInt(bookmarks.getSort()));
            if (Objects.isNull(bookmarkGroup)|| !bookmarkGroup.getUserId().equals(userId)){
                throw new ParamException("书签组不存在");
            }
            if (!Objects.equals(bookmarkGroup.getVersion(), bookmarks.getParentVersion())) {
                throw new OptimisticLockException();
            }
            this.save(bookmarks);

            // 给书签组排序字段追加上当前书签的id
            bookmarkGroup.setSort(StringUtils.hasText(bookmarkGroup.getSort()) ?
                    bookmarkGroup.getSort() + "/" + bookmarks.getId() : bookmarks.getId().toString()
            );
            OptimisticLockUtils.requireUpdated(this.updateById(bookmarkGroup));
            // 如果增加的是书签组
        }else if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP)){
            bookmarks.setSort(null).insert();   // 保存书签组 但是新的是不会有排序字段的
            Bookmarks bookmarkRoot = this.lambdaQuery()
                    .eq(Bookmarks::getUserId, userId)
                    .eq(Bookmarks::getType, BOOKMARK_ROOT)
                    .one(); // 当前用户的书签根排序节点
            if (bookmarkRoot == null || !Objects.equals(bookmarkRoot.getVersion(), bookmarks.getParentVersion())) {
                throw new OptimisticLockException();
            }
            bookmarkRoot.setSort(StringUtils.hasText(bookmarkRoot.getSort())
                    ? bookmarkRoot.getSort() + "/" + bookmarks.getId()
                    : bookmarks.getId().toString());
            OptimisticLockUtils.requireUpdated(this.updateById(bookmarkRoot));
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
        OptimisticLockUtils.requireVersion(bookmarks.getVersion());
        OptimisticLockUtils.requireVersion(bookmarks.getParentVersion());
        Bookmarks currentBookmark = this.getById(bookmarks.getId());
        if (currentBookmark == null || !Objects.equals(currentBookmark.getUserId(), bookmarks.getUserId())
                || !Objects.equals(currentBookmark.getVersion(), bookmarks.getVersion())) {
            throw new OptimisticLockException();
        }
        if (Objects.equals(bookmarks.getType(), BOOKMARK)){
            // 如果是书签，判断书签组是否存在 是否是当前用户的
            Bookmarks bookmarkGroup = this.getById(Integer.parseInt(bookmarks.getSort()));
            if (Objects.isNull(bookmarkGroup)|| !bookmarkGroup.getUserId().equals(bookmarks.getUserId()))
                throw new ParamException("参数存在错误,或页面数据不是最新的！");
            if (!Objects.equals(bookmarkGroup.getVersion(), bookmarks.getParentVersion())) {
                throw new OptimisticLockException();
            }
            // 书签组排序字段删除当前要删除的书签
            String newSort = reduceSortString(bookmarkGroup.getSort(), bookmarks.getId());
            bookmarkGroup.setSort(newSort);
            OptimisticLockUtils.requireUpdated(this.updateById(bookmarkGroup));
            boolean removed = this.remove(new LambdaQueryWrapper<Bookmarks>()
                    .eq(Bookmarks::getId, bookmarks.getId())
                    .eq(Bookmarks::getUserId, bookmarks.getUserId())
                    .eq(Bookmarks::getVersion, bookmarks.getVersion()));
            OptimisticLockUtils.requireUpdated(removed);
            return true;

            // 如果删除的是书签组
        }else if (Objects.equals(bookmarks.getType(), BOOKMARK_GROUP)){
            Bookmarks bookmarkRoot = this.lambdaQuery()
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId())
                .eq(Bookmarks::getType, BOOKMARK_ROOT)
                .one();
            if (bookmarkRoot == null || !Objects.equals(bookmarkRoot.getVersion(), bookmarks.getParentVersion())) {
                throw new OptimisticLockException();
            }
            String newSort = reduceSortString(bookmarkRoot.getSort(), bookmarks.getId());
            // 更新根排序
            bookmarkRoot.setSort(newSort);
            OptimisticLockUtils.requireUpdated(this.updateById(bookmarkRoot));

            // 删除书签组的子书签
            this.lambdaUpdate()
                .eq(Bookmarks::getUserId, bookmarkRoot.getUserId())
                .eq(Bookmarks::getType, BOOKMARK)
                .eq(Bookmarks::getSort, bookmarks.getId().toString())
                .remove();

            boolean removed = this.remove(new LambdaQueryWrapper<Bookmarks>()
                    .eq(Bookmarks::getId, bookmarks.getId())
                    .eq(Bookmarks::getUserId, bookmarks.getUserId())
                    .eq(Bookmarks::getVersion, bookmarks.getVersion()));
            OptimisticLockUtils.requireUpdated(removed);
            return true;

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
        OptimisticLockUtils.requireVersion(bookmarks.getVersion());
        Bookmarks sqlBookmark = bookmarks.selectById();
        // 判断书签是否存在 且是当前用户的
        if (Objects.isNull(sqlBookmark) || !sqlBookmark.getUserId().equals(bookmarks.getUserId()))
            throw new ParamException("书签组不存在");
        if (!Objects.equals(sqlBookmark.getVersion(), bookmarks.getVersion()))
            throw new OptimisticLockException();

        // 排序的数据只是位置不一样
        List<String> submittedIds = Arrays.asList(bookmarks.getSort().split("/")); // 客户端排序ID
        List<String> currentIds = Arrays.asList(sqlBookmark.getSort().split("/")); // 云端排序ID
        OptimisticLockUtils.requireSameIds(currentIds, submittedIds);

        boolean updated = sqlBookmark.setSort(bookmarks.getSort()).updateById();
        OptimisticLockUtils.requireUpdated(updated);
        return true;
    }

    /** 按版本更新书签内容。 */
    @Override
    public Bookmarks updateBookmark(Bookmarks bookmarks) {
        OptimisticLockUtils.requireVersion(bookmarks.getVersion());
        Bookmarks current = this.lambdaQuery()
                .eq(Bookmarks::getId, bookmarks.getId())
                .eq(Bookmarks::getUserId, BaseContext.getCurrentId())
                .one();
        if (current == null || !Objects.equals(current.getVersion(), bookmarks.getVersion())) {
            throw new OptimisticLockException();
        }
        current.setName(bookmarks.getName())
                .setUrl(bookmarks.getUrl())
                .setIcon(bookmarks.getIcon());
        OptimisticLockUtils.requireUpdated(this.updateById(current));
        return current;
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
