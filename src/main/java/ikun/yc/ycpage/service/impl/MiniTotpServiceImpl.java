package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniTotp;
import ikun.yc.ycpage.mapper.MiniTotpMapper;
import ikun.yc.ycpage.service.MiniTotpService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 小程序2FA验证码服务实现。
 * 名称、账号和密钥都允许修改；排序值只能通过置顶接口变更。
 *
 * @author cgl
 * @since 2026/10/06
 */
@Service
public class MiniTotpServiceImpl
        extends ServiceImpl<MiniTotpMapper, MiniTotp>
        implements MiniTotpService {
    private static final int MAX_NAME_LENGTH = 64; // 名称最大长度
    private static final int MAX_ACCOUNT_LENGTH = 128; // 账号标签最大长度
    private static final int MIN_SECRET_LENGTH = 8; // 密钥最短长度，挡住明显的输入错误
    private static final int MAX_SECRET_LENGTH = 128; // 密钥最大长度
    private static final Pattern BASE32_PATTERN = Pattern.compile("^[A-Z2-7]+$"); // RFC 4648 Base32 字符集
    private static final Pattern SECRET_IGNORE_PATTERN = Pattern.compile("[\\s\\-_=]"); // 密钥里可忽略的分隔符和填充符

    /** 查询当前用户的全部2FA条目，导出功能需要全量数据所以不分页 */
    @Override
    public List<MiniTotp> listCurrentUserTotps() {
        // 置顶的按置顶时间戳倒序；未置顶的 sort_order 都是 0，再按 id 倒序，新加的排在前面。
        // 这里刻意不用 update_time 兜底：改条目会动 update_time，会让条目莫名往上跳。
        return baseMapper.selectList(Wrappers.<MiniTotp>lambdaQuery()
                .eq(MiniTotp::getUserOpenid, requireCurrentUserId())
                .orderByDesc(MiniTotp::getSortOrder)
                .orderByDesc(MiniTotp::getId));
    }

    /** 新增当前用户的2FA条目 */
    @Override
    public boolean addCurrentUserTotp(MiniTotp totp) {
        if (totp == null) throw new ParamException("2FA信息不能为空");
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        String name = normalizeName(totp.getName()); // 标准化后的名称
        String account = normalizeAccount(totp.getAccount()); // 标准化后的账号标签
        String secret = normalizeSecret(totp.getSecret()); // 标准化后的密钥
        ensureSecretUnique(userOpenid, secret, null);

        MiniTotp entity = new MiniTotp(); // 待保存的2FA实体
        entity.setUserOpenid(userOpenid);
        entity.setName(name);
        entity.setAccount(account);
        entity.setSecret(secret);
        entity.setSortOrder(0L);
        try {
            return baseMapper.insert(entity) > 0;
        } catch (DuplicateKeyException ex) {
            throw new ParamException("该2FA密钥已存在");
        }
    }

    /**
     * 修改当前用户的2FA条目。
     * 名称、账号和密钥都可以改；改密钥时要排除自身再查重，否则原值会被当成重复。
     */
    @Override
    public boolean updateCurrentUserTotp(MiniTotp totp) {
        if (totp == null || totp.getId() == null) throw new ParamException("2FA条目不存在");
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniTotp oldTotp = getCurrentUserTotp(totp.getId(), userOpenid); // 原2FA条目
        String name = normalizeName(totp.getName()); // 标准化后的名称
        String account = normalizeAccount(totp.getAccount()); // 标准化后的账号标签
        String secret = normalizeSecret(totp.getSecret()); // 标准化后的密钥
        ensureSecretUnique(userOpenid, secret, oldTotp.getId());

        MiniTotp updateEntity = new MiniTotp(); // 待更新的字段，顺带触发更新时间自动填充
        updateEntity.setName(name);
        updateEntity.setAccount(account);
        updateEntity.setSecret(secret);
        try {
            return baseMapper.update(updateEntity, Wrappers.<MiniTotp>lambdaUpdate()
                    .eq(MiniTotp::getId, oldTotp.getId())
                    .eq(MiniTotp::getUserOpenid, userOpenid)
            ) > 0;
        } catch (DuplicateKeyException ex) {
            throw new ParamException("该2FA密钥已存在");
        }
    }

    /**
     * 置顶或取消置顶当前用户的指定2FA条目。
     * 置顶写入当前毫秒时间戳，天然单调递增，所以后置顶的会排在先置顶的前面，不用再查一次最大排序值。
     */
    @Override
    public boolean setCurrentUserTotpTop(Integer id, boolean top) {
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniTotp totp = getCurrentUserTotp(id, userOpenid); // 待置顶或取消置顶的2FA条目
        long sortOrder = top ? System.currentTimeMillis() : 0L; // 新的置顶排序值

        MiniTotp updateEntity = new MiniTotp(); // 只含排序值的更新实体
        updateEntity.setSortOrder(sortOrder);
        return baseMapper.update(updateEntity, Wrappers.<MiniTotp>lambdaUpdate()
                .eq(MiniTotp::getId, totp.getId())
                .eq(MiniTotp::getUserOpenid, userOpenid)
        ) > 0;
    }

    /** 删除当前用户的指定2FA条目 */
    @Override
    public boolean deleteCurrentUserTotp(Integer id) {
        String userOpenid = requireCurrentUserId(); // 当前登录用户openid
        MiniTotp totp = getCurrentUserTotp(id, userOpenid); // 待删除2FA条目
        return baseMapper.delete(Wrappers.<MiniTotp>lambdaQuery()
                .eq(MiniTotp::getId, totp.getId())
                .eq(MiniTotp::getUserOpenid, userOpenid)) > 0;
    }

    /** 查询并校验2FA条目属于当前用户 */
    private MiniTotp getCurrentUserTotp(Integer id, String userOpenid) {
        if (id == null) throw new ParamException("2FA条目不存在");
        MiniTotp totp = baseMapper.selectOne(Wrappers.<MiniTotp>lambdaQuery()
                .eq(MiniTotp::getId, id)
                .eq(MiniTotp::getUserOpenid, userOpenid)); // 当前用户的2FA条目
        if (totp == null) throw new ParamException("2FA条目不存在");
        return totp;
    }

    /** 校验同一用户不存在重复密钥，excludeId 用于修改时排除自身 */
    private void ensureSecretUnique(String userOpenid, String secret, Integer excludeId) {
        long duplicateCount = baseMapper.selectCount(Wrappers.<MiniTotp>lambdaQuery()
                .eq(MiniTotp::getUserOpenid, userOpenid)
                .eq(MiniTotp::getSecret, secret)
                .ne(excludeId != null, MiniTotp::getId, excludeId)); // 同密钥条目数量
        if (duplicateCount > 0) throw new ParamException("该2FA密钥已存在");
    }

    /** 去除名称首尾空白并校验长度 */
    private String normalizeName(String name) {
        String normalizedName = name == null ? "" : name.trim(); // 标准化后的名称
        if (normalizedName.isEmpty()) throw new ParamException("名称不能为空");
        if (normalizedName.length() > MAX_NAME_LENGTH) throw new ParamException("名称不能超过64个字");
        return normalizedName;
    }

    /** 去除账号标签首尾空白并校验长度，账号可以为空 */
    private String normalizeAccount(String account) {
        String normalizedAccount = account == null ? "" : account.trim(); // 标准化后的账号标签
        if (normalizedAccount.length() > MAX_ACCOUNT_LENGTH) throw new ParamException("账号不能超过128个字");
        return normalizedAccount;
    }

    /** 去掉密钥里的空白、连字符和填充符并转大写，再校验是否为合法Base32 */
    private String normalizeSecret(String secret) {
        // 固定用 ROOT 区域转大写，避免土耳其语等区域把小写 i 转成 İ 导致合法密钥被误判
        String normalizedSecret = secret == null
                ? ""
                : SECRET_IGNORE_PATTERN.matcher(secret).replaceAll("").toUpperCase(Locale.ROOT); // 标准化后的密钥
        if (normalizedSecret.isEmpty()) throw new ParamException("密钥不能为空");
        if (normalizedSecret.length() < MIN_SECRET_LENGTH) throw new ParamException("密钥长度不能少于8位");
        if (normalizedSecret.length() > MAX_SECRET_LENGTH) throw new ParamException("密钥不能超过128位");
        if (!BASE32_PATTERN.matcher(normalizedSecret).matches()) {
            throw new ParamException("密钥只能包含A-Z和2-7");
        }
        return normalizedSecret;
    }

    /** 获取当前登录用户openid */
    private String requireCurrentUserId() {
        String userOpenid = BaseContext.getCurrentId(); // 当前登录用户openid
        if (userOpenid == null || userOpenid.isBlank()) throw new ParamException("登录信息有误");
        return userOpenid;
    }
}
