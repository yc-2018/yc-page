package ikun.yc.ycpage.service.impl;

import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.common.exception.ParamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 小程序订阅消息 Service 单元测试
 *
 * @author cgl
 * @since 2026/11/05
 */
class MiniSubscribeMessageServiceImplTest {
    private static final String COMMENT_TEMPLATE_ID = "comment-template"; // 测试用留言通知模板
    private static final String REPLY_TEMPLATE_ID = "reply-template"; // 测试用回复通知模板
    private static final String COMMENT_QUOTA_KEY = "mini:subscribe:quota:owner:" + COMMENT_TEMPLATE_ID; // 主人留言额度key
    private static final String REPLY_QUOTA_KEY = "mini:subscribe:quota:user-2:" + REPLY_TEMPLATE_ID; // 被回复人回复额度key

    private MiniSubscribeMessageServiceImpl service; // 被测订阅消息服务
    private StringRedisTemplate redisTemplate; // 模拟Redis模板
    private ValueOperations<String, String> valueOperations; // 模拟Redis字符串操作
    private WechatMiniAuthService wechatMiniAuthService; // 模拟微信发送接口

    /** 初始化测试依赖和模板配置 */
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        wechatMiniAuthService = mock(WechatMiniAuthService.class);
        service = new MiniSubscribeMessageServiceImpl(redisTemplate, wechatMiniAuthService);
        ReflectionTestUtils.setField(service, "commentTemplateId", COMMENT_TEMPLATE_ID);
        ReflectionTestUtils.setField(service, "replyTemplateId", REPLY_TEMPLATE_ID);
    }

    /** 不支持的模板不能记录授权 */
    @Test
    void recordAcceptedTemplatesShouldRejectUnsupportedTemplate() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.recordAcceptedTemplates("user-1", List.of("unknown")));

        assertEquals("订阅模板不存在", exception.getMessage());
        verifyNoInteractions(valueOperations);
    }

    /** 同意授权后累加剩余次数并设置过期时间 */
    @Test
    void recordAcceptedTemplatesShouldIncreaseQuota() {
        when(valueOperations.increment(COMMENT_QUOTA_KEY)).thenReturn(1L);

        service.recordAcceptedTemplates("owner", List.of(COMMENT_TEMPLATE_ID));

        verify(valueOperations).increment(COMMENT_QUOTA_KEY);
        verify(redisTemplate).expire(COMMENT_QUOTA_KEY, Duration.ofDays(30));
    }

    /** 没有剩余次数时不调用微信发送接口 */
    @Test
    void notifyNewCommentShouldSkipWithoutQuota() {
        when(valueOperations.get(COMMENT_QUOTA_KEY)).thenReturn("0");

        service.notifyNewComment("owner", "西湖", "小红", "你好",
                LocalDateTime.of(2026, 11, 5, 14, 30), 5, "dynamic");

        verifyNoInteractions(wechatMiniAuthService);
    }

    /** 发送留言通知成功后扣减一次额度 */
    @Test
    void notifyNewCommentShouldSendAndConsumeQuota() {
        when(valueOperations.get(COMMENT_QUOTA_KEY)).thenReturn("1");
        when(wechatMiniAuthService.sendSubscribeMessage(eq("owner"), eq(COMMENT_TEMPLATE_ID),
                eq("pages/checkinShare/checkinShare?id=5"), anyMap())).thenReturn(0);

        service.notifyNewComment("owner", "西湖", "小红", "你好",
                LocalDateTime.of(2026, 11, 5, 14, 30), 5, "dynamic");

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class); // 模板字段
        verify(wechatMiniAuthService).sendSubscribeMessage(eq("owner"), eq(COMMENT_TEMPLATE_ID),
                eq("pages/checkinShare/checkinShare?id=5"), dataCaptor.capture());
        assertEquals("小红", dataCaptor.getValue().get("name1"));
        assertEquals("2026年11月5日 14:30", dataCaptor.getValue().get("date4"));
        assertEquals("西湖", dataCaptor.getValue().get("thing2"));
        assertEquals("你好", dataCaptor.getValue().get("thing3"));
        verify(valueOperations).decrement(COMMENT_QUOTA_KEY);
    }

    /** 发送回复通知时打开对应多地分享页并截断过长文本 */
    @Test
    void notifyReplyShouldSendToMultiSharePage() {
        when(valueOperations.get(REPLY_QUOTA_KEY)).thenReturn("1");
        when(wechatMiniAuthService.sendSubscribeMessage(eq("user-2"), eq(REPLY_TEMPLATE_ID),
                eq("pages/multiCheckinShare/multiCheckinShare?id=8"), anyMap())).thenReturn(0);
        String longContent = "一二三四五六七八九十一二三四五六七八九十一二三四五"; // 超长回复内容

        service.notifyReply("user-2", "这是一个超过二十个字的评论项目名称用于测试截断",
                "一个超过十个字的用户昵称", longContent,
                LocalDateTime.of(2026, 11, 5, 14, 30), 8, "multi");

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class); // 模板字段
        verify(wechatMiniAuthService).sendSubscribeMessage(eq("user-2"), eq(REPLY_TEMPLATE_ID),
                eq("pages/multiCheckinShare/multiCheckinShare?id=8"), dataCaptor.capture());
        Map<String, String> data = dataCaptor.getValue(); // 实际发送的模板字段
        assertEquals(20, data.get("thing6").codePointCount(0, data.get("thing6").length()));
        assertEquals(10, data.get("thing7").codePointCount(0, data.get("thing7").length()));
        assertEquals(20, data.get("thing8").codePointCount(0, data.get("thing8").length()));
        assertTrue(data.get("thing8").endsWith("…"));
        assertEquals("2026年11月5日 14:30", data.get("time9"));
        verify(valueOperations).decrement(REPLY_QUOTA_KEY);
    }

    /** 微信返回未订阅时清除本地剩余次数 */
    @Test
    void notifyReplyShouldClearQuotaWhenWechatRejects() {
        when(valueOperations.get(REPLY_QUOTA_KEY)).thenReturn("1");
        when(wechatMiniAuthService.sendSubscribeMessage(eq("user-2"), eq(REPLY_TEMPLATE_ID),
                eq("pages/checkinShare/checkinShare?id=5"), anyMap())).thenReturn(43101);

        service.notifyReply("user-2", "西湖", "小红", "你好",
                LocalDateTime.of(2026, 11, 5, 14, 30), 5, "dynamic");

        verify(redisTemplate).delete(REPLY_QUOTA_KEY);
        verify(valueOperations, never()).decrement(REPLY_QUOTA_KEY);
    }
}
