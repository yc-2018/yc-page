package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.entity.Bookmarks;
import ikun.yc.ycpage.mapper.BookmarksMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ikun.yc.ycpage.service.BookmarksService;
import org.springframework.stereotype.Service;

/**
 * 服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 * @description 由 Mybatisplus Code Generator 创建
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BookmarksServiceImpl extends ServiceImpl<BookmarksMapper, Bookmarks> implements BookmarksService {
    private final BookmarksMapper bookmarksMapper;

}