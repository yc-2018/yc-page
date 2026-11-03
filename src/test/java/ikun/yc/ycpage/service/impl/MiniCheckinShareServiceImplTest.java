package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.entity.MiniCheckinRecords;
import ikun.yc.ycpage.entity.MiniCheckinShare;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareDetailResponse;
import ikun.yc.ycpage.entity.dto.MiniCheckinShareManageItem;
import ikun.yc.ycpage.service.MiniCheckinRecordsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    /** 校验多地分享内容按传入顺序保存记录ID和名称 */
    @Test
    void buildMultiShareContentShouldKeepRecordOrder() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildMultiShareContent", String.class, List.class); // 多地分享内容构建方法

        method.setAccessible(true);
        String content = (String) method.invoke(service, "周末游", Arrays.asList(3, 1, 2)); // 多地分享内容

        assertEquals("{\"title\":\"周末游\",\"recordIds\":[3,1,2]}", content);
    }

    /** 校验多地分享内容异常时解析结果为空 */
    @Test
    void parseShareRecordIdsShouldReturnEmptyWhenContentInvalid() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务

        assertEquals(Collections.emptyList(), invokeParseShareRecordIds(service, "not-json"));
        assertEquals(Collections.emptyList(), invokeParseShareRecordIds(service, ""));
    }

    /** 校验多地分享记录ID会过滤空值并按原顺序去重 */
    @Test
    void normalizeRecordIdsShouldDropNullAndDuplicates() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("normalizeRecordIds", List.class); // 记录ID清洗方法

        method.setAccessible(true);
        List<?> recordIds = (List<?>) method.invoke(service, Arrays.asList(3, null, 1, 3)); // 清洗后的记录ID

        assertEquals(Arrays.asList(3, 1), recordIds);
    }

    /** 校验多地分享详情按内容顺序返回仍然存在的记录 */
    @Test
    void readMultiShareDetailShouldKeepContentOrder() throws Exception {
        MiniCheckinRecordsService recordsService = mock(MiniCheckinRecordsService.class); // 打卡记录服务
        when(recordsService.list(ArgumentMatchers.<Wrapper<MiniCheckinRecords>>any())).thenReturn(Arrays.asList(
                buildRecord(2, "地点B"),
                buildRecord(1, "地点A"))); // 数据库中仍存在的记录
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(recordsService, new ObjectMapper()); // 被测服务
        MiniCheckinShare share = new MiniCheckinShare(); // 多地分享记录
        share.setContent("{\"title\":\"周末游\",\"recordIds\":[1,2,3]}");
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("readMultiShareDetail", MiniCheckinShare.class); // 多地分享详情读取方法

        method.setAccessible(true);
        MiniCheckinShareDetailResponse detail = (MiniCheckinShareDetailResponse) method.invoke(service, share); // 多地分享详情

        assertEquals(2, detail.getPoints().size());
        assertEquals("地点A", detail.getPoints().get(0).getName());
        assertEquals("地点B", detail.getPoints().get(1).getName());
        assertEquals("周末游", detail.getName());
        assertEquals("共2个地点", detail.getAddress());
    }

    /** 校验多地分享管理项展示名称和地点数量，未命名时回退默认名称 */
    @Test
    void buildManageItemShouldShowMultiPointCount() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildManageItem", MiniCheckinShare.class, MiniCheckinRecords.class, LocalDateTime.class); // 管理项构建方法
        method.setAccessible(true);

        MiniCheckinShare namedShare = new MiniCheckinShare(); // 已命名的多地分享记录
        namedShare.setId(9);
        namedShare.setRecordId(4);
        namedShare.setShareType("multi");
        namedShare.setContent("{\"title\":\"出差路线\",\"recordIds\":[4,5,6]}");
        namedShare.setExpireTime(LocalDateTime.now().plusDays(1));
        MiniCheckinShareManageItem namedItem = (MiniCheckinShareManageItem) method.invoke(service, namedShare, null, LocalDateTime.now()); // 已命名管理列表项

        assertEquals("出差路线", namedItem.getName());
        assertEquals("共3个地点", namedItem.getAddress());
        assertEquals(Integer.valueOf(3), namedItem.getPointCount());

        MiniCheckinShare unnamedShare = new MiniCheckinShare(); // 未命名的多地分享记录
        unnamedShare.setId(10);
        unnamedShare.setRecordId(4);
        unnamedShare.setShareType("multi");
        unnamedShare.setContent("{\"title\":\"\",\"recordIds\":[4,5]}");
        unnamedShare.setExpireTime(LocalDateTime.now().plusDays(1));
        MiniCheckinShareManageItem unnamedItem = (MiniCheckinShareManageItem) method.invoke(service, unnamedShare, null, LocalDateTime.now()); // 未命名管理列表项

        assertEquals("多地打卡分享", unnamedItem.getName());
        assertEquals(Integer.valueOf(2), unnamedItem.getPointCount());
    }

    /** 校验管理列表优先使用多地分享名称和地点数量独立字段 */
    @Test
    void buildManageItemShouldUseMultiShareColumns() throws Exception {
        MiniCheckinShareServiceImpl service = new MiniCheckinShareServiceImpl(null, new ObjectMapper()); // 被测服务
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildManageItem", MiniCheckinShare.class, MiniCheckinRecords.class, LocalDateTime.class); // 管理项构建方法
        method.setAccessible(true);

        MiniCheckinShare share = new MiniCheckinShare(); // 已有独立字段的多地分享记录
        share.setId(11);
        share.setRecordId(4);
        share.setShareType("multi");
        share.setShareTitle("新路线");
        share.setPointCount(4);
        share.setExpireTime(LocalDateTime.now().plusDays(1));
        MiniCheckinShareManageItem item = (MiniCheckinShareManageItem) method.invoke(service, share, null, LocalDateTime.now()); // 管理列表项

        assertEquals("新路线", item.getName());
        assertEquals("共4个地点", item.getAddress());
        assertEquals(Integer.valueOf(4), item.getPointCount());
    }

    /**
     * 构造打卡记录，便于多地分享测试
     *
     * @param id   记录ID
     * @param name 地点名称
     * @return 打卡记录
     */
    private MiniCheckinRecords buildRecord(Integer id, String name) {
        MiniCheckinRecords record = new MiniCheckinRecords();
        record.setId(id);
        record.setName(name);
        return record;
    }

    /**
     * 调用多地分享内容解析方法
     *
     * @param service 被测服务
     * @param content 分享内容
     * @return 解析出的记录ID
     */
    private List<Integer> invokeParseShareRecordIds(MiniCheckinShareServiceImpl service, String content) throws Exception {
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("parseShareRecordIds", String.class); // 多地分享内容解析方法
        method.setAccessible(true);
        return castRecordIds(method.invoke(service, content));
    }

    /**
     * 将反射结果转换为记录ID列表，避免泛型擦除警告
     *
     * @param value 反射返回值
     * @return 记录ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Integer> castRecordIds(Object value) {
        return (List<Integer>) value;
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
