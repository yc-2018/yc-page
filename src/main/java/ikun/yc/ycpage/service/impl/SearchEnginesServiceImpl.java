package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.entity.SearchEngines;
import ikun.yc.ycpage.mapper.SearchEnginesMapper;
import ikun.yc.ycpage.service.SearchEnginesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 服务接口实现
 *
 * @author yc
 * @since 2023-12-03 22:57:40
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SearchEnginesServiceImpl extends ServiceImpl<SearchEnginesMapper, SearchEngines> implements SearchEnginesService {
}