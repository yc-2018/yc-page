package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.entity.CheckinRecords;
import ikun.yc.ycpage.service.MiniUsersService;
import ikun.yc.ycpage.service.CheckinRecordsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
    private final CheckinRecordsService checkinRecordsService;

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
    public R<Boolean> checkin(@RequestBody @Valid CheckinRecords checkinRecord) {
        return R.success(checkinRecord.insert());
    }
}