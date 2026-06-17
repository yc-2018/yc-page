package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.PassToken;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 小程序打卡分享控制器
 *
 * @author cgl
 * @since 2026/06/18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/checkinShare")
public class MiniCheckinShareController {
    private final MiniCheckinShareService miniCheckinShareService;

    /**
     * 创建打卡分享
     *
     * @param request 创建请求
     * @return 分享ID和过期时间
     */
    @PostMapping
    @UserId(fieldName = "userOpenid")
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<MiniCheckinShareCreateResponse> createShare(@RequestBody @Valid MiniCheckinShareCreateRequest request) {
        return R.success(miniCheckinShareService.createShare(request));
    }

    /**
     * 获取打卡分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    @PassToken
    @GetMapping("/{id}")
    public R<MiniCheckinShareDetailResponse> getShareDetail(@PathVariable Integer id) {
        return R.success(miniCheckinShareService.getShareDetail(id));
    }
}
