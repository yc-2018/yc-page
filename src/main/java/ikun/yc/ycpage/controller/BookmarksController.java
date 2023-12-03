package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.service.BookmarksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 服务控制器
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 * @description 由 Mybatisplus Code Generator 创建
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/bookmarks")
public class BookmarksController {
    private final BookmarksService bookmarksService;

}