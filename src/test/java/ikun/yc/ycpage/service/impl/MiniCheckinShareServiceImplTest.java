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
        Method method = MiniCheckinShareServiceImpl.class
                .getDeclaredMethod("buildShareContent", MiniCheckinRecords.class, String.class, boolean.class, boolean.class); // 分享内容构建方法

        method.setAccessible(true);
        String content = (String) method.invoke(service, record, "static", true, true); // 静态分享内容 JSON

        assertEquals("餐厅", objectMapper.readTree(content).get("locationType").asText());
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
}
