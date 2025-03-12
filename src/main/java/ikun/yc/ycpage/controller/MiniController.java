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
 * 小程序【在这打卡】控制器
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
        long count = checkinRecord.selectCount(Wrappers.<CheckinRecords>lambdaQuery()
                .select(CheckinRecords::getId)
                .eq(CheckinRecords::getUserOpenid, checkinRecord.getUserOpenid())
                .eq(CheckinRecords::getIsDeleted, 0)
                .eq(CheckinRecords::getLongitude, checkinRecord.getLongitude())
                .eq(CheckinRecords::getLatitude, checkinRecord.getLatitude())
                .ge(CheckinRecords::getCheckinTime, LocalDate.now().atStartOfDay())
        );
        if (count != 0) return R.error("此处今日已打卡");

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
    @PostMapping("/checkinList/{page}")
    public R<Page<CheckinRecords>> checkinList(@RequestBody MiniCheckinDto checkinDto, @PathVariable Integer page) {
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

    /**
     * 删除打卡记录
     *
     * @param id 打卡记录 id
     * @author ChenGuangLong
     * @since 2025/03/11
     */
    @PostMapping("/deleteCheckin/{id}")
    public R<?> deleteCheckin(@PathVariable Integer id) {
        boolean updateOk = new CheckinRecords().update(Wrappers.<CheckinRecords>lambdaUpdate()
                .set(CheckinRecords::getIsDeleted, 1)
                .eq(CheckinRecords::getUserOpenid, BaseContext.getCurrentId())
                .eq(CheckinRecords::getId, id)
        );
        return updateOk ? R.success(true) : R.error("删除失败");
    }

    /**
     * 修改打卡数据
     *
     * @param checkinRecords 打卡记录
     * @author ChenGuangLong
     * @since 2025/03/11
     */
    @PostMapping("/updateCheckin")
    public R<?> updateCheckin(@RequestBody CheckinRecords checkinRecords) {
        if (checkinRecords.getId() == null) return R.error("数据有误！");
        boolean updateOk = checkinRecords.update(Wrappers.<CheckinRecords>lambdaUpdate()
                .set(StringUtils.hasText(checkinRecords.getName()), CheckinRecords::getName, checkinRecords.getName())
                .set(StringUtils.hasText(checkinRecords.getAddress()), CheckinRecords::getAddress, checkinRecords.getAddress())
                .set(StringUtils.hasText(checkinRecords.getRemark()), CheckinRecords::getRemark, checkinRecords.getRemark())
                .set(StringUtils.hasText(checkinRecords.getLocationType()), CheckinRecords::getLocationType, checkinRecords.getLocationType())
                .set( CheckinRecords::getUpdateTime, LocalDate.now())
                .eq(CheckinRecords::getId, checkinRecords.getId())
                .eq(CheckinRecords::getUserOpenid, BaseContext.getCurrentId())
        );
        return updateOk ? R.success(true) : R.error("修改失败");
    }
}