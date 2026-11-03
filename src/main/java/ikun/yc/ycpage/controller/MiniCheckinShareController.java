package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.PassToken;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareExtendRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareManageItem;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareMultiCreateRequest;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
     * 创建多地打卡分享
     *
     * @param request 多地分享创建请求
     * @return 分享ID和过期时间
     */
    @PostMapping("/multi")
    @UserId(fieldName = "userOpenid")
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<MiniCheckinShareCreateResponse> createMultiShare(@RequestBody @Valid MiniCheckinShareMultiCreateRequest request) {
        return R.success(miniCheckinShareService.createMultiShare(request));
    }

    /**
     * 获取当前用户的分享管理列表
     *
     * @param page      第几页
     * @param shareType 分享类型筛选，可为空；支持 dynamic/static/multi
     * @return 分享管理列表
     */
    @PostMapping("/manage/list/{page}")
    public R<Page<MiniCheckinShareManageItem>> getManageList(@PathVariable Integer page,
                                                             @RequestParam(required = false) String shareType) {
        return R.success(miniCheckinShareService.getManageList(page, shareType));
    }

    /**
     * 修改分享时间
     *
     * @param id      分享ID
     * @param request 续期请求
     * @return 分享ID和新的过期时间
     */
    @PostMapping("/manage/extend/{id}")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<MiniCheckinShareCreateResponse> extendShare(@PathVariable Integer id,
                                                         @RequestBody @Valid MiniCheckinShareExtendRequest request) {
        return R.success(miniCheckinShareService.extendShare(id, request.getDurationType()));
    }

    /**
     * 停用分享
     *
     * @param id 分享ID
     * @return 是否停用成功
     */
    @PostMapping("/manage/disable/{id}")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> disableShare(@PathVariable Integer id) {
        return R.success(miniCheckinShareService.disableShare(id));
    }

    /**
     * 删除分享记录
     *
     * @param id 分享ID
     * @return 是否删除成功
     */
    @PostMapping("/manage/delete/{id}")
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 10)
    public R<Boolean> deleteShare(@PathVariable Integer id) {
        return R.success(miniCheckinShareService.deleteShare(id));
    }

    /**
     * 一键清除当前用户已停用或已过期的分享
     *
     * @return 清除的分享数量
     */
    @PostMapping("/manage/deleteExpired")
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 10)
    public R<Integer> deleteExpiredShares() {
        return R.success(miniCheckinShareService.deleteExpiredShares());
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

    /**
     * 获取带当前登录人身份的打卡分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    @GetMapping("/owner/{id}")
    public R<MiniCheckinShareDetailResponse> getOwnerShareDetail(@PathVariable Integer id) {
        return R.success(miniCheckinShareService.getOwnerShareDetail(id));
    }
}
