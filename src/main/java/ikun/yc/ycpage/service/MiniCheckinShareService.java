package ikun.yc.ycpage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;

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
}
