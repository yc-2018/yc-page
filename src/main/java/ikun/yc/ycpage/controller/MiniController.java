package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.PassToken;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniUser;
import ikun.yc.ycpage.entity.dto.MiniCheckinDto;
import ikun.yc.ycpage.service.MiniUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
    private final MiniUserService miniUserService;
    private static final List<Integer> NEARBY_RADIUS_OPTIONS = Arrays.asList(100, 500, 1000, 5000, 10000, 50000); // 附近搜索允许的半径
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180); // 最小合法经度
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180); // 最大合法经度
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90); // 最小合法纬度
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90); // 最大合法纬度
    private static final String NEARBY_DISTANCE_SQL = "6371000 * 2 * ASIN(SQRT("
            + "POWER(SIN((RADIANS(latitude) - RADIANS({1})) / 2), 2) "
            + "+ COS(RADIANS({1})) * COS(RADIANS(latitude)) "
            + "* POWER(SIN((RADIANS(longitude) - RADIANS({0})) / 2), 2)"
            + ")) <= {2}"; // Haversine 球面距离，单位米

    @PassToken
    @PostMapping("/login")
    public R<String> wechatLogin(String code) {
        return R.success(miniUserService.miniLogin(code));
    }

    /**
     * 保存打卡记录
     *
     * @param checkinRecord 打卡记录
     * @return {@link R }<{@link String }>
     * @author ChenGuangLong
     * @since 2025/03/08 17:33:38
     */
    @CountControl(operationType = CountControlAspect.ADD, msg = "频率过快 冷却一分钟")  // 一分钟请求超出5次，禁用1分钟
    @UserId(fieldName = "userOpenid")
    @PostMapping("/checkin")
    public R<?> checkin(@RequestBody @Valid MiniCheckinRecords checkinRecord) {
        // 先从数据库获取今天的这个用户这个经纬度是否已经打卡
        long count = checkinRecord.selectCount(Wrappers.<MiniCheckinRecords>lambdaQuery()
                .select(MiniCheckinRecords::getId)
                .eq(MiniCheckinRecords::getUserOpenid, checkinRecord.getUserOpenid())
                .eq(MiniCheckinRecords::getIsDeleted, 0)
                .eq(MiniCheckinRecords::getLongitude, checkinRecord.getLongitude())
                .eq(MiniCheckinRecords::getLatitude, checkinRecord.getLatitude())
                .ge(MiniCheckinRecords::getCheckinTime, LocalDate.now().atStartOfDay())
        );
        if (count != 0) return R.error("此处今日已打卡");

        // 判断今天打卡是否超过100次
        if (checkinRecord.selectCount(Wrappers.<MiniCheckinRecords>lambdaQuery()
                .eq(MiniCheckinRecords::getUserOpenid, checkinRecord.getUserOpenid())
                .ge(MiniCheckinRecords::getCreateTime, LocalDate.now().atStartOfDay())
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
    public R<Page<MiniCheckinRecords>> checkinList(@RequestBody MiniCheckinDto checkinDto, @PathVariable Integer page) {
        MiniCheckinDto queryDto = checkinDto == null ? new MiniCheckinDto() : checkinDto; // 实际使用的筛选参数
        String nearbySearchError = validateNearbySearch(queryDto); // 附近搜索参数错误信息
        if (nearbySearchError != null) return R.error(nearbySearchError);
        boolean hasNearbySearch = hasNearbySearch(queryDto); // 是否启用附近搜索
        Page<MiniCheckinRecords> pages = new Page<>(page, 10);
        Page<MiniCheckinRecords> recordsPage = new MiniCheckinRecords().selectPage(pages, Wrappers.<MiniCheckinRecords>lambdaQuery()
                .eq(MiniCheckinRecords::getUserOpenid, BaseContext.getCurrentId())
                .between(queryDto.getStartTime() != null && queryDto.getEndTime() != null, MiniCheckinRecords::getCheckinTime, queryDto.getStartTime(), queryDto.getEndTime())
                .eq(MiniCheckinRecords::getIsDeleted, 0)
                .and(StringUtils.hasText(queryDto.getAddress()), wrapper -> wrapper.like(MiniCheckinRecords::getAddress, queryDto.getAddress()).or().like(MiniCheckinRecords::getName, queryDto.getAddress()))
                .like(StringUtils.hasText(queryDto.getRemark()), MiniCheckinRecords::getRemark, queryDto.getRemark())
                .eq(StringUtils.hasText(queryDto.getLocationType()), MiniCheckinRecords::getLocationType, queryDto.getLocationType())
                .apply(hasNearbySearch, NEARBY_DISTANCE_SQL, queryDto.getNearbyLongitude(), queryDto.getNearbyLatitude(), queryDto.getNearbyRadius())
                .orderByDesc(MiniCheckinRecords::getCheckinTime)
        );
        return R.success(recordsPage);
    }

    /**
     * 判断请求是否携带附近搜索条件
     *
     * @param checkinDto 小程序打卡搜索列表请求参数
     * @return 是否启用附近搜索
     */
    private boolean hasNearbySearch(MiniCheckinDto checkinDto) {
        return checkinDto.getNearbyLongitude() != null
                || checkinDto.getNearbyLatitude() != null
                || checkinDto.getNearbyRadius() != null;
    }

    /**
     * 校验附近搜索参数，避免缺字段或半径超出预设范围
     *
     * @param checkinDto 小程序打卡搜索列表请求参数
     * @return 错误提示，返回 null 表示校验通过
     */
    private String validateNearbySearch(MiniCheckinDto checkinDto) {
        if (!hasNearbySearch(checkinDto)) return null;
        if (checkinDto.getNearbyLongitude() == null || checkinDto.getNearbyLatitude() == null || checkinDto.getNearbyRadius() == null) return "附近搜索参数不完整";
        if (!NEARBY_RADIUS_OPTIONS.contains(checkinDto.getNearbyRadius())) return "附近搜索范围有误";
        if (checkinDto.getNearbyLongitude().compareTo(MIN_LONGITUDE) < 0 || checkinDto.getNearbyLongitude().compareTo(MAX_LONGITUDE) > 0) return "附近搜索经度有误";
        if (checkinDto.getNearbyLatitude().compareTo(MIN_LATITUDE) < 0 || checkinDto.getNearbyLatitude().compareTo(MAX_LATITUDE) > 0) return "附近搜索纬度有误";
        return null;
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
        boolean updateOk = new MiniCheckinRecords().update(Wrappers.<MiniCheckinRecords>lambdaUpdate()
                .set(MiniCheckinRecords::getIsDeleted, 1)
                .eq(MiniCheckinRecords::getUserOpenid, BaseContext.getCurrentId())
                .eq(MiniCheckinRecords::getId, id)
        );
        return updateOk ? R.success(true) : R.error("删除失败");
    }

    /**
     * 修改打卡数据
     *
     * @param miniCheckinRecords 打卡记录
     * @author ChenGuangLong
     * @since 2025/03/11
     */
    @PostMapping("/updateCheckin")
    public R<?> updateCheckin(@RequestBody MiniCheckinRecords miniCheckinRecords) {
        if (miniCheckinRecords.getId() == null) return R.error("数据有误！");
        boolean updateOk = miniCheckinRecords.update(Wrappers.<MiniCheckinRecords>lambdaUpdate()
                .set(StringUtils.hasText(miniCheckinRecords.getName()), MiniCheckinRecords::getName, miniCheckinRecords.getName())
                .set(StringUtils.hasText(miniCheckinRecords.getAddress()), MiniCheckinRecords::getAddress, miniCheckinRecords.getAddress())
                .set(MiniCheckinRecords::getRemark, miniCheckinRecords.getRemark())     // 描述
                .set(MiniCheckinRecords::getImgs, miniCheckinRecords.getImgs())         // 图片
                .set(StringUtils.hasText(miniCheckinRecords.getLocationType()), MiniCheckinRecords::getLocationType, miniCheckinRecords.getLocationType())
                .set(miniCheckinRecords.getCheckinTime() != null, MiniCheckinRecords::getCheckinTime, miniCheckinRecords.getCheckinTime())
                .set( MiniCheckinRecords::getUpdateTime, LocalDate.now())
                .eq(MiniCheckinRecords::getId, miniCheckinRecords.getId())
                .eq(MiniCheckinRecords::getUserOpenid, BaseContext.getCurrentId())
        );
        return updateOk ? R.success(true) : R.error("修改失败");
    }

    /**
     * 获取用户信息(头像、名称)
     *
     * @author ChenGuangLong
     * @since 2025/09/02 01:29:02
     */
    @PostMapping("/getUserInfo")
    public R<MiniUser> getUserInfo() {
        return R.success(new MiniUser().selectOne(Wrappers.<MiniUser>lambdaQuery()
                .select(MiniUser::getNickname, MiniUser::getAvatarUrl)
                .eq(MiniUser::getOpenid, BaseContext.getCurrentId())
        ));
    }

    /**
     * 更新用户信息（头像、名称）
     *
     * @param miniUser 微信用户
     * @author ChenGuangLong
     * @since 2025/09/02 01:36:02
     */
    @PostMapping("/updateUserInfo")
    public R<?> updateUserInfo(@RequestBody MiniUser miniUser) {
        if (StringUtils.hasText(miniUser.getNickname()) && StringUtils.hasText(miniUser.getAvatarUrl())) return R.error("没数！");

        miniUser.update(Wrappers.<MiniUser>lambdaUpdate()
                .set(StringUtils.hasText(miniUser.getNickname()), MiniUser::getNickname, miniUser.getNickname())
                .set(StringUtils.hasText(miniUser.getAvatarUrl()), MiniUser::getAvatarUrl, miniUser.getAvatarUrl())
                .eq(MiniUser::getOpenid, BaseContext.getCurrentId())
        );
        return R.success(miniUser);
    }
}
