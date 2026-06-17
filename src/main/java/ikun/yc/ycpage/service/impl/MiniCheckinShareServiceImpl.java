package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.common.exception.SqlSaveException;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.enumeration.MiniCheckinShareDurationType;
import ikun.yc.ycpage.mapper.MiniCheckinShareMapper;
import ikun.yc.ycpage.service.MiniCheckinRecordsService;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 小程序打卡分享 Service 实现
 *
 * @author cgl
 * @since 2026/06/18
 */
@Service
@RequiredArgsConstructor
public class MiniCheckinShareServiceImpl extends ServiceImpl<MiniCheckinShareMapper, MiniCheckinShare> implements MiniCheckinShareService {
    private static final String SHARE_TYPE_DYNAMIC = "dynamic"; // 动态分享类型
    private static final String SHARE_TYPE_STATIC = "static"; // 静态分享类型
    private static final String SHARE_NOT_FOUND_MSG = "分享已过期或不存在"; // 分享不可访问的统一提示

    private final MiniCheckinRecordsService miniCheckinRecordsService;
    private final ObjectMapper objectMapper;

    /**
     * 创建打卡分享
     *
     * @param request 创建请求
     * @return 分享ID和过期时间
     */
    @Override
    public MiniCheckinShareCreateResponse createShare(MiniCheckinShareCreateRequest request) {
        if (request == null) {
            throw new ParamException("参数不能为空");
        }
        String userOpenid = request.getUserOpenid(); // 当前创建人openid
        if (!StringUtils.hasText(userOpenid)) {
            throw new ParamException("登录信息有误");
        }

        MiniCheckinRecords record = miniCheckinRecordsService.getOne(Wrappers.<MiniCheckinRecords>lambdaQuery()
                .eq(MiniCheckinRecords::getId, request.getRecordId())
                .eq(MiniCheckinRecords::getUserOpenid, userOpenid)
                .eq(MiniCheckinRecords::getIsDeleted, 0)
                .last("limit 1"));
        if (record == null) {
            throw new ParamException("打卡记录不存在");
        }

        boolean includeRemark = request.getIncludeRemark() == null || request.getIncludeRemark(); // 是否分享备注
        boolean includeImgs = request.getIncludeImgs() == null || request.getIncludeImgs(); // 是否分享图片
        String shareType = normalizeShareType(request.getShareType()); // 标准化分享类型
        LocalDateTime expireTime = MiniCheckinShareDurationType.fromCode(request.getDurationType())
                .resolveExpireTime(LocalDateTime.now()); // 有效截止时间
        String content = buildShareContent(record, shareType, includeRemark, includeImgs); // 分享内容字段

        MiniCheckinShare share = new MiniCheckinShare();
        share.setUserOpenid(userOpenid);
        share.setRecordId(record.getId());
        share.setExpireTime(expireTime);
        share.setShareType(shareType);
        share.setIncludeRemark(includeRemark ? 1 : 0);
        share.setIncludeImgs(includeImgs ? 1 : 0);
        share.setContent(content);

        if (!save(share)) {
            throw new SqlSaveException("创建分享失败");
        }
        return new MiniCheckinShareCreateResponse(share.getId(), expireTime);
    }

    /**
     * 获取分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    @Override
    public MiniCheckinShareDetailResponse getShareDetail(Integer id) {
        if (id == null) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        MiniCheckinShare share = getById(id);
        if (share == null || share.getExpireTime() == null || !share.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }

        MiniCheckinShareDetailResponse detail = SHARE_TYPE_STATIC.equals(share.getShareType())
                ? readStaticShareDetail(share)
                : readDynamicShareDetail(share);
        detail.setId(share.getId());
        detail.setRecordId(share.getRecordId());
        detail.setExpireTime(share.getExpireTime());
        detail.setShareType(share.getShareType());
        applyIncludeOptions(detail, share);
        return detail;
    }

    /**
     * 构建分享内容字段
     *
     * @param record        原打卡记录
     * @param shareType     分享类型
     * @param includeRemark 是否包含备注
     * @param includeImgs   是否包含图片
     * @return 内容字段
     */
    private String buildShareContent(MiniCheckinRecords record, String shareType, boolean includeRemark, boolean includeImgs) {
        if (SHARE_TYPE_DYNAMIC.equals(shareType)) {
            return String.valueOf(record.getId());
        }

        MiniCheckinShareDetailResponse snapshot = buildDetailFromRecord(record);
        if (!includeRemark) {
            snapshot.setRemark(null);
        }
        if (!includeImgs) {
            snapshot.setImgs(null);
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new ParamException("分享内容生成失败");
        }
    }

    /**
     * 读取动态分享详情
     *
     * @param share 分享记录
     * @return 分享详情
     */
    private MiniCheckinShareDetailResponse readDynamicShareDetail(MiniCheckinShare share) {
        MiniCheckinRecords record = miniCheckinRecordsService.getOne(Wrappers.<MiniCheckinRecords>lambdaQuery()
                .eq(MiniCheckinRecords::getId, share.getRecordId())
                .eq(MiniCheckinRecords::getIsDeleted, 0)
                .last("limit 1"));
        if (record == null) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        return buildDetailFromRecord(record);
    }

    /**
     * 读取静态分享详情
     *
     * @param share 分享记录
     * @return 分享详情
     */
    private MiniCheckinShareDetailResponse readStaticShareDetail(MiniCheckinShare share) {
        try {
            return objectMapper.readValue(share.getContent(), MiniCheckinShareDetailResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
    }

    /**
     * 将打卡记录转换为分享详情
     *
     * @param record 打卡记录
     * @return 分享详情
     */
    private MiniCheckinShareDetailResponse buildDetailFromRecord(MiniCheckinRecords record) {
        MiniCheckinShareDetailResponse detail = new MiniCheckinShareDetailResponse();
        detail.setRecordId(record.getId());
        detail.setLongitude(record.getLongitude());
        detail.setLatitude(record.getLatitude());
        detail.setName(record.getName());
        detail.setAddress(record.getAddress());
        detail.setRemark(record.getRemark());
        detail.setImgs(record.getImgs());
        detail.setCheckinTime(record.getCheckinTime());
        return detail;
    }

    /**
     * 根据分享勾选项清理不允许展示的字段
     *
     * @param detail 分享详情
     * @param share  分享记录
     */
    private void applyIncludeOptions(MiniCheckinShareDetailResponse detail, MiniCheckinShare share) {
        if (!Integer.valueOf(1).equals(share.getIncludeRemark())) {
            detail.setRemark(null);
        }
        if (!Integer.valueOf(1).equals(share.getIncludeImgs())) {
            detail.setImgs(null);
        }
    }

    /**
     * 标准化分享类型
     *
     * @param shareType 前端传入分享类型
     * @return 标准分享类型
     */
    private String normalizeShareType(String shareType) {
        if (!StringUtils.hasText(shareType)) {
            return SHARE_TYPE_DYNAMIC;
        }
        String normalized = shareType.trim().toLowerCase(); // 标准分享类型
        if (!SHARE_TYPE_DYNAMIC.equals(normalized) && !SHARE_TYPE_STATIC.equals(normalized)) {
            throw new ParamException("分享类型有误");
        }
        return normalized;
    }
}
