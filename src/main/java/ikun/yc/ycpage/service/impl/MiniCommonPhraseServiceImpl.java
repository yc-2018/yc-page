package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniCommonPhrase;
import ikun.yc.ycpage.mapper.MiniCommonPhraseMapper;
import ikun.yc.ycpage.service.MiniCommonPhraseService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序打卡常用语服务实现
 *
 * @author cgl
 * @since 2026/08/16
 */
@Service
public class MiniCommonPhraseServiceImpl
        extends ServiceImpl<MiniCommonPhraseMapper, MiniCommonPhrase>
        implements MiniCommonPhraseService {
    private static final int MAX_CONTENT_LENGTH = 255; // 单条常用语最大长度

    /** 查询当前用户的全部常用语 */
    @Override
    public List<MiniCommonPhrase> listCurrentUserPhrases() {
        return baseMapper.selectList(Wrappers.<MiniCommonPhrase>lambdaQuery()
                .eq(MiniCommonPhrase::getUserOpenid, requireCurrentUserId())
                .orderByDesc(MiniCommonPhrase::getSortOrder)
                .orderByAsc(MiniCommonPhrase::getId));
    }

    /** 新增当前用户的常用语 */
    @Override
    public boolean addCurrentUserPhrase(MiniCommonPhrase phrase) {
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        String content = normalizeContent(phrase == null ? null : phrase.getContent()); // 标准化后的内容
        ensureContentUnique(userOpenid, content, null);

        MiniCommonPhrase entity = new MiniCommonPhrase(); // 待保存的常用语实体
        entity.setUserOpenid(userOpenid);
        entity.setContent(content);
        entity.setSortOrder(0L);
        try {
            return baseMapper.insert(entity) > 0;
        } catch (DuplicateKeyException ex) {
            throw new ParamException("常用语已存在");
        }
    }

    /** 修改当前用户的常用语 */
    @Override
    public boolean updateCurrentUserPhrase(MiniCommonPhrase phrase) {
        if (phrase == null || phrase.getId() == null) throw new ParamException("常用语不存在");
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniCommonPhrase oldPhrase = getCurrentUserPhrase(phrase.getId(), userOpenid); // 原常用语
        String content = normalizeContent(phrase.getContent()); // 标准化后的内容
        ensureContentUnique(userOpenid, content, oldPhrase.getId());

        try {
            return baseMapper.update(null, Wrappers.<MiniCommonPhrase>lambdaUpdate()
                    .eq(MiniCommonPhrase::getId, oldPhrase.getId())
                    .eq(MiniCommonPhrase::getUserOpenid, userOpenid)
                    .set(MiniCommonPhrase::getContent, content)
                    .set(MiniCommonPhrase::getUpdateTime, LocalDateTime.now())
            ) > 0;
        } catch (DuplicateKeyException ex) {
            throw new ParamException("常用语已存在");
        }
    }

    /** 将当前用户的指定常用语置顶 */
    @Override
    public boolean topCurrentUserPhrase(Integer id) {
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniCommonPhrase phrase = getCurrentUserPhrase(id, userOpenid); // 待置顶常用语
        MiniCommonPhrase currentTop = baseMapper.selectOne(Wrappers.<MiniCommonPhrase>lambdaQuery()
                .select(MiniCommonPhrase::getSortOrder)
                .eq(MiniCommonPhrase::getUserOpenid, userOpenid)
                .orderByDesc(MiniCommonPhrase::getSortOrder)
                .last("LIMIT 1")); // 当前排序值最大的常用语
        long topSortOrder = currentTop == null || currentTop.getSortOrder() == null
                ? 1L
                : currentTop.getSortOrder() + 1L; // 新置顶排序值

        return baseMapper.update(null, Wrappers.<MiniCommonPhrase>lambdaUpdate()
                .eq(MiniCommonPhrase::getId, phrase.getId())
                .eq(MiniCommonPhrase::getUserOpenid, userOpenid)
                .set(MiniCommonPhrase::getSortOrder, topSortOrder)
                .set(MiniCommonPhrase::getUpdateTime, LocalDateTime.now())
        ) > 0;
    }

    /** 删除当前用户的指定常用语 */
    @Override
    public boolean deleteCurrentUserPhrase(Integer id) {
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniCommonPhrase phrase = getCurrentUserPhrase(id, userOpenid); // 待删除常用语
        return baseMapper.delete(Wrappers.<MiniCommonPhrase>lambdaQuery()
                .eq(MiniCommonPhrase::getId, phrase.getId())
                .eq(MiniCommonPhrase::getUserOpenid, userOpenid)) > 0;
    }

    /** 查询并校验常用语属于当前用户 */
    private MiniCommonPhrase getCurrentUserPhrase(Integer id, String userOpenid) {
        if (id == null) throw new ParamException("常用语不存在");
        MiniCommonPhrase phrase = baseMapper.selectOne(Wrappers.<MiniCommonPhrase>lambdaQuery()
                .eq(MiniCommonPhrase::getId, id)
                .eq(MiniCommonPhrase::getUserOpenid, userOpenid)); // 当前用户的常用语
        if (phrase == null) throw new ParamException("常用语不存在");
        return phrase;
    }

    /** 校验同一用户不存在重复常用语 */
    private void ensureContentUnique(String userOpenid, String content, Integer excludeId) {
        long duplicateCount = baseMapper.selectCount(Wrappers.<MiniCommonPhrase>lambdaQuery()
                .eq(MiniCommonPhrase::getUserOpenid, userOpenid)
                .eq(MiniCommonPhrase::getContent, content)
                .ne(excludeId != null, MiniCommonPhrase::getId, excludeId)); // 同内容常用语数量
        if (duplicateCount > 0) throw new ParamException("常用语已存在");
    }

    /** 去除内容首尾空白并校验长度 */
    private String normalizeContent(String content) {
        String normalizedContent = content == null ? "" : content.trim(); // 标准化后的常用语内容
        if (normalizedContent.isEmpty()) throw new ParamException("常用语不能为空");
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) throw new ParamException("常用语不能超过255个字");
        return normalizedContent;
    }

    /** 获取当前登录用户openid */
    private String requireCurrentUserId() {
        String userOpenid = BaseContext.getCurrentId(); // 当前登录用户openid
        if (userOpenid == null || userOpenid.isBlank()) throw new ParamException("登录信息有误");
        return userOpenid;
    }
}
