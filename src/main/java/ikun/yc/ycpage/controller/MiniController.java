package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.entity.CheckinRecords;
import ikun.yc.ycpage.entity.dto.MiniCheckinDto;
import ikun.yc.ycpage.service.MiniUsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 小程序控制器
 *
 * @author yc
 * @since 2025-3-8
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/mini")
public class MiniController {
    private final MiniUsersService miniUsersService;

    @PostMapping("/login")
    public R<String> wechatLogin(String code) {
        return R.success(miniUsersService.miniLogin(code));
    }

    /**
     * 保存打卡记录
     *
     * @param checkinRecord 打卡记录
     * @return {@link R }<{@link String }>
     * @author ChenGuangLong
     * @since 2025/03/08 17:33:38
     */
    @UserId(fieldName = "userOpenid")
    @PostMapping("/checkin")
    public R<?> checkin(@RequestBody @Valid CheckinRecords checkinRecord) {
        // 先从数据库获取今天的这个用户这个经纬度是否已经打卡
        CheckinRecords sqlCheckin = checkinRecord.selectOne(Wrappers.<CheckinRecords>lambdaQuery()
                .select(CheckinRecords::getId)
                .eq(CheckinRecords::getUserOpenid, checkinRecord.getUserOpenid())
                .eq(CheckinRecords::getLongitude, checkinRecord.getLongitude())
                .eq(CheckinRecords::getLatitude, checkinRecord.getLatitude())
                .ge(CheckinRecords::getCheckinTime, LocalDate.now().atStartOfDay())
        );
        if (sqlCheckin != null) return R.error("此处今日已打卡");

        // 判断今天打卡是否超过100次
        if (checkinRecord.selectCount(Wrappers.<CheckinRecords>lambdaQuery()
                .eq(CheckinRecords::getUserOpenid, checkinRecord.getUserOpenid())
                .ge(CheckinRecords::getCheckinTime, LocalDate.now().atStartOfDay())
        ) > 100) return R.error("今日打卡超百次");

        return R.success(checkinRecord.insert());
    }

    /**
     * 获取打卡列表
     *
     * @param checkinDto 小程序打卡搜索列表请求参数
     * @param page       第几页
     * @author ChenGuangLong
     * @since 2025/03/09
     */
    @PostMapping("/checkinList")
    public R<Page<CheckinRecords>> checkinList(@RequestBody MiniCheckinDto checkinDto, @RequestParam(defaultValue = "1") Integer page) {
        Page<CheckinRecords> pages = new Page<>(page, 10);
        Page<CheckinRecords> recordsPage = new CheckinRecords().selectPage(pages, Wrappers.<CheckinRecords>lambdaQuery()
                .eq(CheckinRecords::getUserOpenid, BaseContext.getCurrentId())
                .between(checkinDto.getStartTime() != null && checkinDto.getEndTime() != null, CheckinRecords::getCheckinTime, checkinDto.getStartTime(), checkinDto.getEndTime())
                .eq(CheckinRecords::getIsDeleted, 0)
                .and(StringUtils.hasText(checkinDto.getAddress()), wrapper -> wrapper.like(CheckinRecords::getAddress, checkinDto.getAddress()).or().like(CheckinRecords::getName, checkinDto.getAddress()))
                .like(StringUtils.hasText(checkinDto.getRemark()), CheckinRecords::getRemark, checkinDto.getRemark())
                .eq(StringUtils.hasText(checkinDto.getLocationType()), CheckinRecords::getLocationType, checkinDto.getLocationType())
                .orderByDesc(CheckinRecords::getCheckinTime)
        );
        return R.success(recordsPage);
    }
}