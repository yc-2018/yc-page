package ikun.yc.ycpage.service.impl;

import ikun.yc.ycpage.common.WechatMiniAuthService;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.service.MiniSubscribeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 小程序一次性订阅消息 Service 实现
 *
 * @author cgl
 * @since 2026/11/05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiniSubscribeMessageServiceImpl implements MiniSubscribeMessageService {
    private static final String QUOTA_KEY_PREFIX = "mini:subscribe:quota:"; // 一次性订阅剩余次数 key 前缀
    private static final String COOLDOWN_KEY_PREFIX = "mini:subscribe:cooldown:"; // 微信拒绝后的冷却 key 前缀
    private static final int QUOTA_MAX = 20; // 单个模板最多累计的待发送次数
    private static final Duration COOLDOWN_TTL = Duration.ofDays(1); // 微信返回未订阅后的重试间隔
    private static final String SHARE_TYPE_MULTI = "multi"; // 多地分享类型
    private static final String DEFAULT_NICKNAME = "微信用户"; // 昵称缺失时的兜底
    private static final String DEFAULT_PROJECT = "打卡分享"; // 项目名称缺失时的兜底
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"); // 留言时间格式，date 类型要求年月日
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"); // 回复时间格式

    private final StringRedisTemplate stringRedisTemplate;
    private final WechatMiniAuthService wechatMiniAuthService;

    @Value("${mini.subscribe.comment-template-id:}")
    private String commentTemplateId; // 评论留言通知模板ID
    @Value("${mini.subscribe.reply-template-id:}")
    private String replyTemplateId; // 评论回复通知模板ID

    /**
     * 记录用户已同意接收的订阅消息模板
     *
     * <p>微信一次性订阅不限时间，本地次数只作辅助记录，因此不设置过期时间。</p>
     *
     * @param openid      当前用户openid
     * @param templateIds 用户已同意的模板ID
     */
    @Override
    public void recordAcceptedTemplates(String openid, List<String> templateIds) {
        if (!StringUtils.hasText(openid)) {
            throw new ParamException("登录信息有误");
        }
        if (templateIds == null || templateIds.isEmpty()) {
            throw new ParamException("订阅模板不能为空");
        }
        for (String templateId : new LinkedHashSet<>(templateIds)) {
            if (!isSupportedTemplate(templateId)) {
                throw new ParamException("订阅模板不存在");
            }
            String key = quotaKey(openid, templateId); // 当前模板的剩余次数 key
            Long remain = stringRedisTemplate.opsForValue().increment(key); // 本次授权后的剩余次数
            log.info("记录订阅授权, openid={}, templateId={}, remain={}", openid, templateId, remain);
            if (remain != null && remain > QUOTA_MAX) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(QUOTA_MAX));
            }
            clearCooldown(openid, templateId);
        }
    }

    /**
     * 发送评论留言通知，提醒记录主人有新评论
     *
     * @param ownerOpenid   记录主人openid
     * @param projectName   评论项目名称
     * @param commenterName 留言人昵称
     * @param content       留言内容
     * @param time          留言时间
     * @param shareId       分享ID，可为空
     * @param shareType     分享类型，可为空
     */
    @Override
    public void notifyNewComment(String ownerOpenid, String projectName, String commenterName,
                                 String content, LocalDateTime time, Integer shareId, String shareType) {
        try {
            if (!StringUtils.hasText(ownerOpenid)) {
                return;
            }
            if (!StringUtils.hasText(commentTemplateId)) {
                log.warn("未配置评论留言通知模板，跳过发送");
                return;
            }
            if (inCooldown(ownerOpenid, commentTemplateId)) {
                log.info("跳过评论留言通知：订阅失效冷却中, openid={}", ownerOpenid);
                return;
            }
            Map<String, String> data = new LinkedHashMap<>(); // 评论留言模板字段
            data.put("name1", limitText(commenterName, 10, DEFAULT_NICKNAME));
            data.put("date4", DATE_TIME_FORMAT.format(time == null ? LocalDateTime.now() : time));
            data.put("thing2", limitText(projectName, 20, DEFAULT_PROJECT));
            data.put("thing3", limitText(content, 20, "有新评论"));
            send(ownerOpenid, commentTemplateId, buildPage(shareId, shareType), data);
        } catch (Exception ex) {
            log.warn("发送评论留言通知失败, openid={}", ownerOpenid, ex);
        }
    }

    /**
     * 发送评论回复通知，提醒被回复人有新回复
     *
     * @param targetOpenid 被回复人openid
     * @param projectName  评论项目名称
     * @param replierName  回复用户昵称
     * @param content      回复内容
     * @param time         回复时间
     * @param shareId      分享ID，可为空
     * @param shareType    分享类型，可为空
     */
    @Override
    public void notifyReply(String targetOpenid, String projectName, String replierName,
                            String content, LocalDateTime time, Integer shareId, String shareType) {
        try {
            if (!StringUtils.hasText(targetOpenid)) {
                return;
            }
            if (!StringUtils.hasText(replyTemplateId)) {
                log.warn("未配置评论回复通知模板，跳过发送");
                return;
            }
            if (inCooldown(targetOpenid, replyTemplateId)) {
                log.info("跳过评论回复通知：订阅失效冷却中, openid={}", targetOpenid);
                return;
            }
            Map<String, String> data = new LinkedHashMap<>(); // 评论回复模板字段
            data.put("thing6", limitText(projectName, 20, DEFAULT_PROJECT));
            data.put("thing7", limitText(replierName, 10, DEFAULT_NICKNAME));
            data.put("thing8", limitText(content, 20, "有新回复"));
            data.put("time9", TIME_FORMAT.format(time == null ? LocalDateTime.now() : time));
            send(targetOpenid, replyTemplateId, buildPage(shareId, shareType), data);
        } catch (Exception ex) {
            log.warn("发送评论回复通知失败, openid={}", targetOpenid, ex);
        }
    }

    /**
     * 判断接收人是否处于微信拒绝后的冷却期
     *
     * @param openid     接收人openid
     * @param templateId 模板ID
     * @return 是否仍在冷却期
     */
    private boolean inCooldown(String openid, String templateId) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey(openid, templateId)));
        } catch (Exception ex) {
            log.warn("读取订阅冷却状态失败，继续尝试发送, openid={}, templateId={}", openid, templateId);
            return false;
        }
    }

    /**
     * 发送订阅消息，发送成功扣减额度，微信返回未订阅时进入冷却期
     *
     * @param openid     接收人openid
     * @param templateId 模板ID
     * @param page       点击消息打开的页面
     * @param data       模板字段值
     */
    private void send(String openid, String templateId, String page, Map<String, String> data) {
        Integer errcode = wechatMiniAuthService.sendSubscribeMessage(openid, templateId, page, data); // 微信发送结果
        if (errcode != null && errcode == 0) {
            consumeQuota(openid, templateId);
            log.info("订阅消息发送成功, openid={}, templateId={}, page={}", openid, templateId, page);
            return;
        }
        if (errcode != null && errcode == 43101) {
            clearQuota(openid, templateId);
            markCooldown(openid, templateId);
            log.info("用户未订阅或已取消订阅，暂停尝试1天, openid={}, templateId={}", openid, templateId);
            return;
        }
        log.warn("发送订阅消息失败, openid={}, templateId={}, errcode={}", openid, templateId, errcode);
    }

    /**
     * 发送成功后扣减一次订阅次数，Redis 异常不影响已发送结果
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     */
    private void consumeQuota(String openid, String templateId) {
        try {
            String key = quotaKey(openid, templateId); // 剩余次数 key
            Long remain = stringRedisTemplate.opsForValue().decrement(key); // 扣减后的剩余次数
            if (remain != null && remain <= 0) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception ex) {
            log.warn("扣减订阅额度失败, openid={}, templateId={}", openid, templateId, ex);
        }
    }

    /**
     * 清除指定模板的订阅次数
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     */
    private void clearQuota(String openid, String templateId) {
        try {
            stringRedisTemplate.delete(quotaKey(openid, templateId));
        } catch (Exception ex) {
            log.warn("清除订阅额度失败, openid={}, templateId={}", openid, templateId, ex);
        }
    }

    /**
     * 记录微信拒绝后的冷却状态，避免持续无效发送
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     */
    private void markCooldown(String openid, String templateId) {
        try {
            stringRedisTemplate.opsForValue().set(cooldownKey(openid, templateId), "1", COOLDOWN_TTL);
        } catch (Exception ex) {
            log.warn("记录订阅冷却状态失败, openid={}, templateId={}", openid, templateId, ex);
        }
    }

    /**
     * 清除冷却状态，用户重新授权后立即恢复发送
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     */
    private void clearCooldown(String openid, String templateId) {
        try {
            stringRedisTemplate.delete(cooldownKey(openid, templateId));
        } catch (Exception ex) {
            log.warn("清除订阅冷却状态失败, openid={}, templateId={}", openid, templateId, ex);
        }
    }

    /**
     * 判断模板是否属于当前小程序配置的订阅模板
     *
     * @param templateId 模板ID
     * @return 是否为有效模板
     */
    private boolean isSupportedTemplate(String templateId) {
        return StringUtils.hasText(templateId)
                && (templateId.equals(commentTemplateId) || templateId.equals(replyTemplateId));
    }

    /**
     * 生成订阅消息剩余次数缓存 key
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     * @return 缓存 key
     */
    private String quotaKey(String openid, String templateId) {
        return QUOTA_KEY_PREFIX + openid + ":" + templateId;
    }

    /**
     * 生成订阅消息冷却状态缓存 key
     *
     * @param openid     用户openid
     * @param templateId 模板ID
     * @return 缓存 key
     */
    private String cooldownKey(String openid, String templateId) {
        return COOLDOWN_KEY_PREFIX + openid + ":" + templateId;
    }

    /**
     * 生成点击订阅消息后打开的页面路径
     *
     * @param shareId   分享ID，可为空
     * @param shareType 分享类型，可为空
     * @return 小程序页面路径
     */
    private String buildPage(Integer shareId, String shareType) {
        if (shareId == null || shareId <= 0) {
            return "pages/records/records";
        }
        if (SHARE_TYPE_MULTI.equals(shareType)) {
            return "pages/multiCheckinShare/multiCheckinShare?id=" + shareId;
        }
        return "pages/checkinShare/checkinShare?id=" + shareId;
    }

    /**
     * 按微信模板长度限制截断文本
     *
     * @param text      原始文本
     * @param maxLength 最大字符数
     * @param fallback  文本为空时的兜底
     * @return 截断后的文本
     */
    private String limitText(String text, int maxLength, String fallback) {
        String value = StringUtils.hasText(text) ? text.trim() : fallback; // 清洗后的文本
        if (!StringUtils.hasText(value)) {
            value = fallback;
        }
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, Math.max(1, maxLength - 1)); // 保留省略号位置
        return value.substring(0, endIndex) + "…";
    }
}
