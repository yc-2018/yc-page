package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareManageItem;

/**
 * 小程序打卡分享 Service
 *
 * @author cgl
 * @since 2026/06/18
 */
public interface MiniCheckinShareService extends IService<MiniCheckinShare> {
    /**
     * 创建打卡分享
     *
     * @param request 创建请求
     * @return 分享ID和过期时间
     */
    MiniCheckinShareCreateResponse createShare(MiniCheckinShareCreateRequest request);

    /**
     * 获取分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    MiniCheckinShareDetailResponse getShareDetail(Integer id);

    /**
     * 获取带当前登录人身份的分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    MiniCheckinShareDetailResponse getOwnerShareDetail(Integer id);

    /**
     * 分页获取当前用户的分享列表
     *
     * @param page 第几页
     * @return 分享管理列表
     */
    Page<MiniCheckinShareManageItem> getManageList(Integer page);

    /**
     * 从当前时间重新计算分享过期时间
     *
     * @param id           分享ID
     * @param durationType 分享时长类型
     * @return 分享ID和新的过期时间
     */
    MiniCheckinShareCreateResponse extendShare(Integer id, String durationType);

    /**
     * 停用分享
     *
     * @param id 分享ID
     * @return 是否停用成功
     */
    Boolean disableShare(Integer id);

    /**
     * 删除分享记录
     *
     * @param id 分享ID
     * @return 是否删除成功
     */
    Boolean deleteShare(Integer id);
}
