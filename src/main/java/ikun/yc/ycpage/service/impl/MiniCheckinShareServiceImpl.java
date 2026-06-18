package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.common.exception.SqlSaveException;
import ikun.yc.ycpage.common.exception.SqlUpdateException;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateRequest;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareCreateResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareManageItem;
import ikun.yc.ycpage.entity.enumeration.MiniCheckinShareDurationType;
import ikun.yc.ycpage.mapper.MiniCheckinShareMapper;
import ikun.yc.ycpage.service.MiniCheckinRecordsService;
import ikun.yc.ycpage.service.MiniCheckinShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (share == null || isShareExpired(share, LocalDateTime.now())) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        return buildShareDetail(share, false, false);
    }

    /**
     * 获取带当前登录人身份的分享详情
     *
     * @param id 分享ID
     * @return 分享详情
     */
    @Override
    public MiniCheckinShareDetailResponse getOwnerShareDetail(Integer id) {
        if (id == null) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        MiniCheckinShare share = getById(id);
        if (share == null) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        boolean isOwner = StringUtils.hasText(BaseContext.getCurrentId())
                && BaseContext.getCurrentId().equals(share.getUserOpenid()); // 当前访问者是否作者
        boolean isExpired = isShareExpired(share, LocalDateTime.now()); // 当前分享是否过期
        if (!isOwner && isExpired) {
            throw new ParamException(SHARE_NOT_FOUND_MSG);
        }
        return buildShareDetail(share, isOwner, isExpired);
    }

    /**
     * 分页获取当前用户的分享列表
     *
     * @param page 第几页
     * @return 分享管理列表
     */
    @Override
    public Page<MiniCheckinShareManageItem> getManageList(Integer page) {
        String userOpenid = BaseContext.getCurrentId(); // 当前登录用户openid
        Page<MiniCheckinShare> sharePage = page(new Page<>(page == null ? 1 : page, 10),
                Wrappers.<MiniCheckinShare>lambdaQuery()
                        .eq(MiniCheckinShare::getUserOpenid, userOpenid)
                        .orderByDesc(MiniCheckinShare::getUpdateTime)
                        .orderByDesc(MiniCheckinShare::getCreateTime));
        Page<MiniCheckinShareManageItem> resultPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        resultPage.setPages(sharePage.getPages());

        List<MiniCheckinShare> shares = sharePage.getRecords(); // 当前页分享记录
        if (shares.isEmpty()) {
            resultPage.setRecords(java.util.Collections.emptyList());
            return resultPage;
        }

        List<Integer> recordIds = shares.stream()
                .map(MiniCheckinShare::getRecordId)
                .distinct()
                .collect(Collectors.toList()); // 当前页涉及的打卡记录ID
        Map<Integer, MiniCheckinRecords> recordMap = miniCheckinRecordsService.list(Wrappers.<MiniCheckinRecords>lambdaQuery()
                .in(MiniCheckinRecords::getId, recordIds))
                .stream()
                .collect(Collectors.toMap(MiniCheckinRecords::getId, record -> record, (left, right) -> left));

        LocalDateTime now = LocalDateTime.now(); // 当前服务器时间
        List<MiniCheckinShareManageItem> records = shares.stream()
                .map(share -> buildManageItem(share, recordMap.get(share.getRecordId()), now))
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * 从当前时间重新计算分享过期时间
     *
     * @param id           分享ID
     * @param durationType 分享时长类型
     * @return 分享ID和新的过期时间
     */
    @Override
    public MiniCheckinShareCreateResponse extendShare(Integer id, String durationType) {
        MiniCheckinShare share = getCurrentUserShare(id); // 当前用户的分享记录
        LocalDateTime expireTime = MiniCheckinShareDurationType.fromCode(durationType)
                .resolveExpireTime(LocalDateTime.now()); // 新有效截止时间
        boolean updateOk = update(Wrappers.<MiniCheckinShare>lambdaUpdate()
                .eq(MiniCheckinShare::getId, share.getId())
                .eq(MiniCheckinShare::getUserOpenid, BaseContext.getCurrentId())
                .set(MiniCheckinShare::getExpireTime, expireTime)
                .set(MiniCheckinShare::getUpdateTime, LocalDateTime.now()));
        if (!updateOk) {
            throw new SqlUpdateException("修改分享时间失败");
        }
        return new MiniCheckinShareCreateResponse(share.getId(), expireTime);
    }

    /**
     * 停用分享
     *
     * @param id 分享ID
     * @return 是否停用成功
     */
    @Override
    public Boolean disableShare(Integer id) {
        MiniCheckinShare share = getCurrentUserShare(id); // 当前用户的分享记录
        LocalDateTime now = LocalDateTime.now(); // 当前服务器时间
        if (share.getExpireTime() == null || !share.getExpireTime().isAfter(now)) {
            throw new ParamException("分享已经停用");
        }
        boolean updateOk = update(Wrappers.<MiniCheckinShare>lambdaUpdate()
                .eq(MiniCheckinShare::getId, share.getId())
                .eq(MiniCheckinShare::getUserOpenid, BaseContext.getCurrentId())
                .set(MiniCheckinShare::getExpireTime, now)
                .set(MiniCheckinShare::getUpdateTime, now));
        if (!updateOk) {
            throw new SqlUpdateException("停用分享失败");
        }
        return true;
    }

    /**
     * 删除分享记录
     *
     * @param id 分享ID
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteShare(Integer id) {
        MiniCheckinShare share = getCurrentUserShare(id); // 当前用户的分享记录
        boolean removeOk = removeById(share.getId()); // 删除结果
        if (!removeOk) {
            throw new SqlUpdateException("删除分享失败");
        }
        return true;
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
     * 构建最终分享详情
     *
     * @param share     分享记录
     * @param isOwner   当前访问者是否作者
     * @param isExpired 分享是否已过期
     * @return 分享详情
     */
    private MiniCheckinShareDetailResponse buildShareDetail(MiniCheckinShare share, boolean isOwner, boolean isExpired) {
        MiniCheckinShareDetailResponse detail = SHARE_TYPE_STATIC.equals(share.getShareType())
                ? readStaticShareDetail(share)
                : readDynamicShareDetail(share);
        detail.setId(share.getId());
        detail.setRecordId(share.getRecordId());
        detail.setExpireTime(share.getExpireTime());
        detail.setShareType(share.getShareType());
        detail.setIsOwner(isOwner);
        detail.setIsExpired(isExpired);
        applyIncludeOptions(detail, share);
        return detail;
    }

    /**
     * 判断分享是否已过期
     *
     * @param share 分享记录
     * @param now   当前服务器时间
     * @return 是否过期
     */
    private boolean isShareExpired(MiniCheckinShare share, LocalDateTime now) {
        return share.getExpireTime() == null || !share.getExpireTime().isAfter(now);
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
     * 获取当前用户自己的分享记录
     *
     * @param id 分享ID
     * @return 分享记录
     */
    private MiniCheckinShare getCurrentUserShare(Integer id) {
        if (id == null) {
            throw new ParamException("分享不存在");
        }
        MiniCheckinShare share = getOne(Wrappers.<MiniCheckinShare>lambdaQuery()
                .eq(MiniCheckinShare::getId, id)
                .eq(MiniCheckinShare::getUserOpenid, BaseContext.getCurrentId())
                .last("limit 1"));
        if (share == null) {
            throw new ParamException("分享不存在");
        }
        return share;
    }

    /**
     * 构建分享管理列表项
     *
     * @param share  分享记录
     * @param record 原打卡记录
     * @param now    当前服务器时间
     * @return 管理列表项
     */
    private MiniCheckinShareManageItem buildManageItem(MiniCheckinShare share, MiniCheckinRecords record, LocalDateTime now) {
        MiniCheckinShareManageItem item = new MiniCheckinShareManageItem();
        item.setId(share.getId());
        item.setRecordId(share.getRecordId());
        item.setExpireTime(share.getExpireTime());
        item.setShareType(share.getShareType());
        item.setIncludeRemark(share.getIncludeRemark());
        item.setIncludeImgs(share.getIncludeImgs());
        item.setIsExpired(share.getExpireTime() == null || !share.getExpireTime().isAfter(now));
        if (record != null) {
            item.setName(record.getName());
            item.setAddress(record.getAddress());
            item.setCheckinTime(record.getCheckinTime());
        } else {
            item.setName("原打卡记录已删除");
            item.setAddress("");
        }
        return item;
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
