package ikun.yc.ycpage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareManageItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 小程序打卡分享 Service 单元测试
 */
class MiniCheckinShareServiceImplTest {

    /** 校验静态分享快照会从打卡记录带出地点类型 */
    @Test
    void buildDetailFromRecordShouldIncludeLocationType() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setLocationType("餐厅");
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildDetailFromRecord", MiniCheckinRecords.class); // 快照构建方法

        method.setAccessible(true);
        MiniCheckinShareDetailResponse detail = (MiniCheckinShareDetailResponse) method.invoke(service, record); // 分享详情

        assertEquals("餐厅", detail.getLocationType());
    }

    /** 校验静态分享内容 JSON 会从打卡记录带出地点类型 */
    @Test
    void buildStaticShareContentShouldIncludeLocationType() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 处理器
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, objectMapper); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setLocationType("餐厅");
        String content = invokeBuildShareContent(service, record, "static", true, true, null, null); // 静态分享内容 JSON

        assertEquals("餐厅", objectMapper.readTree(content).get("locationType").asText());
    }

    /** 校验静态分享快照使用本次分享传入的备注和图片 */
    @Test
    void buildStaticShareContentShouldUseCustomRemarkAndImgs() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 处理器
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, objectMapper); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setRemark("原备注");
        record.setImgs("a.jpg,b.jpg,c.jpg");

        String content = invokeBuildShareContent(service, record, "static", true, true,
                "分享备注", "a.jpg,c.jpg"); // 静态分享内容 JSON

        assertEquals("分享备注", objectMapper.readTree(content).get("remark").asText());
        assertEquals("a.jpg,c.jpg", objectMapper.readTree(content).get("imgs").asText());
    }

    /** 校验静态分享快照允许把备注和图片清空 */
    @Test
    void buildStaticShareContentShouldAllowEmptyRemarkAndImgs() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 处理器
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, objectMapper); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setRemark("原备注");
        record.setImgs("a.jpg,b.jpg");

        String content = invokeBuildShareContent(service, record, "static", true, true, "", ""); // 静态分享内容 JSON

        assertEquals("", objectMapper.readTree(content).get("remark").asText());
        assertEquals("", objectMapper.readTree(content).get("imgs").asText());
    }

    /** 校验旧版请求缺少覆盖字段时仍使用原记录内容 */
    @Test
    void buildStaticShareContentShouldFallbackWhenOverridesAreMissing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 处理器
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, objectMapper); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setRemark("原备注");
        record.setImgs("a.jpg,b.jpg");

        String content = invokeBuildShareContent(service, record, "static", true, true, null, null); // 旧版请求快照

        assertEquals("原备注", objectMapper.readTree(content).get("remark").asText());
        assertEquals("a.jpg,b.jpg", objectMapper.readTree(content).get("imgs").asText());
    }

    /** 校验动态分享忽略仅供静态快照使用的覆盖字段 */
    @Test
    void buildDynamicShareContentShouldIgnoreStaticOverrides() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);

        String content = invokeBuildShareContent(service, record, "dynamic", true, true,
                "分享备注", "fake.jpg"); // 动态分享内容

        assertEquals("7", content);
    }

    /** 校验分享管理列表项会从打卡记录带出地点类型 */
    @Test
    void buildManageItemShouldIncludeLocationType() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        MiniCheckinShare share = new MiniCheckinShare(); // 分享记录
        share.setId(1);
        share.setRecordId(7);
        share.setShareType("dynamic");
        share.setExpireTime(LocalDateTime.now().plusDays(1));
        MiniCheckinRecords record = new MiniCheckinRecords(); // 原打卡记录
        record.setId(7);
        record.setLocationType("学校");
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildManageItem", MiniCheckinShare.class, MiniCheckinRecords.class, LocalDateTime.class); // 管理项构建方法

        method.setAccessible(true);
        MiniCheckinShareManageItem item = (MiniCheckinShareManageItem) method.invoke(service, share, record, LocalDateTime.now()); // 管理列表项

        assertEquals("学校", item.getLocationType());
    }

    /**
     * 调用分享内容构建方法，便于覆盖静态快照分支
     *
     * @param service       被测服务
     * @param record        原打卡记录
     * @param shareType     分享类型
     * @param includeRemark 是否包含备注
     * @param includeImgs   是否包含图片
     * @param staticRemark  静态分享备注覆盖值
     * @param staticImgs    静态分享图片覆盖值
     * @return 分享内容字段
     */
    private String invokeBuildShareContent(MiniCheckinShareServiceImpl service,
                                           MiniCheckinRecords record,
                                           String shareType,
                                           boolean includeRemark,
                                           boolean includeImgs,
                                           String staticRemark,
                                           String staticImgs) throws Exception {
        Method method = MiniCheckinShareServiceImpl.class.getDeclaredMethod(
                "buildShareContent", MiniCheckinRecords.class, String.class,
                boolean.class, boolean.class, String.class, String.class); // 分享内容构建方法
        method.setAccessible(true);
        return (String) method.invoke(service, record, shareType, includeRemark, includeImgs,
                staticRemark, staticImgs);
    }
}
