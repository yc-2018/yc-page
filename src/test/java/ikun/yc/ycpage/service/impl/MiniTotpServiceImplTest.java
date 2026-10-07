package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniTotp;
import ikun.yc.ycpage.mapper.MiniTotpMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 小程序2FA验证码 Service 单元测试。 */
class MiniTotpServiceImplTest {
    private MiniTotpServiceImpl service; // 被测2FA服务
    private MiniTotpMapper mapper; // 模拟数据访问层

    /** 构造一个新增用的2FA请求 */
    private static MiniTotp newTotp(String name, String account, String secret) {
        MiniTotp totp = new MiniTotp(); // 新增请求
        totp.setName(name);
        totp.setAccount(account);
        totp.setSecret(secret);
        return totp;
    }

    /** 初始化当前用户和模拟 Mapper */
    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(MiniTotp.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), ""); // 测试用表信息构建器
            TableInfoHelper.initTableInfo(assistant, MiniTotp.class);
        }
        service = new MiniTotpServiceImpl();
        mapper = mock(MiniTotpMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        BaseContext.setCurrentId("openid-a");
    }

    /** 清除测试线程中的用户信息 */
    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    /** 新增时应去除首尾空白、归一化密钥并绑定当前用户 */
    @Test
    void addShouldNormalizeFieldsAndBindCurrentUser() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(MiniTotp.class))).thenAnswer(invocation -> {
            MiniTotp totp = invocation.getArgument(0); // 实际插入实体
            assertEquals("openid-a", totp.getUserOpenid());
            assertEquals("Com.MP Domains", totp.getName());
            assertEquals("cgl556@88.com", totp.getAccount());
            assertEquals("JG7Y3TK4RO4KJLND", totp.getSecret());
            return 1;
        });

        assertTrue(service.addCurrentUserTotp(
                newTotp("  Com.MP Domains  ", "  cgl556@88.com  ", " jg7y-3tk4 ro4kjlnd== ")));
    }

    /** 账号为空时应落库为空串而不是 null */
    @Test
    void addShouldAllowBlankAccount() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(MiniTotp.class))).thenAnswer(invocation -> {
            MiniTotp totp = invocation.getArgument(0); // 实际插入实体
            assertEquals("", totp.getAccount());
            return 1;
        });

        assertTrue(service.addCurrentUserTotp(newTotp("openai", null, "JG7Y3TK4RO4KJLND")));
    }

    /** 空名称应被拒绝 */
    @Test
    void addShouldRejectBlankName() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("   ", "", "JG7Y3TK4RO4KJLND")));

        assertEquals("名称不能为空", exception.getMessage());
    }

    /** 超长名称应被拒绝 */
    @Test
    void addShouldRejectOverlongName() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("a".repeat(65), "", "JG7Y3TK4RO4KJLND")));

        assertEquals("名称不能超过64个字", exception.getMessage());
    }

    /** 含非 Base32 字符的密钥应被拒绝，0/1/8/9 不在字符集里 */
    @Test
    void addShouldRejectNonBase32Secret() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "JG7Y3TK4RO4KJL01")));

        assertEquals("密钥只能包含A-Z和2-7", exception.getMessage());
    }

    /** 空密钥应被拒绝 */
    @Test
    void addShouldRejectBlankSecret() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "  ")));

        assertEquals("密钥不能为空", exception.getMessage());
    }

    /** 过短密钥应被拒绝 */
    @Test
    void addShouldRejectTooShortSecret() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "ABCDEFG")));

        assertEquals("密钥长度不能少于8位", exception.getMessage());
    }

    /** 超长密钥应被拒绝 */
    @Test
    void addShouldRejectOverlongSecret() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "A".repeat(129))));

        assertEquals("密钥不能超过128位", exception.getMessage());
    }

    /** 同一用户的重复密钥应被拒绝 */
    @Test
    void addShouldRejectDuplicateSecret() {
        when(mapper.selectCount(any())).thenReturn(1L);

        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "JG7Y3TK4RO4KJLND")));

        assertEquals("该2FA密钥已存在", exception.getMessage());
    }

    /** 查重条件必须带当前用户，避免跨用户误判 */
    @Test
    void addShouldScopeDuplicateCheckToCurrentUser() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(MiniTotp.class))).thenReturn(1);

        service.addCurrentUserTotp(newTotp("openai", "", "JG7Y3TK4RO4KJLND"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> countCaptor = ArgumentCaptor.forClass(Wrapper.class); // 查重条件
        verify(mapper).selectCount(countCaptor.capture());
        LambdaQueryWrapper<MiniTotp> countWrapper = (LambdaQueryWrapper<MiniTotp>) countCaptor.getValue(); // 查重 Wrapper
        countWrapper.getSqlSegment();
        assertTrue(countWrapper.getParamNameValuePairs().containsValue("openid-a"));
    }

    /** 修改时查询不到当前用户数据应拒绝操作 */
    @Test
    void updateShouldRejectTotpNotOwnedByCurrentUser() {
        when(mapper.selectOne(any())).thenReturn(null);
        MiniTotp totp = newTotp("改名后的名称", "", "JG7Y3TK4RO4KJLND"); // 修改请求
        totp.setId(9);

        ParamException exception = assertThrows(ParamException.class,
                () -> service.updateCurrentUserTotpName(totp));

        assertEquals("2FA条目不存在", exception.getMessage());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class); // 用户归属查询条件
        verify(mapper).selectOne(queryCaptor.capture());
        LambdaQueryWrapper<MiniTotp> queryWrapper = (LambdaQueryWrapper<MiniTotp>) queryCaptor.getValue(); // 归属查询 Wrapper
        queryWrapper.getSqlSegment();
        Map<String, Object> values = queryWrapper.getParamNameValuePairs(); // 查询参数值
        assertTrue(values.containsValue(9));
        assertTrue(values.containsValue("openid-a"));
    }

    /** 没有 id 的修改请求应被拒绝 */
    @Test
    void updateShouldRejectMissingId() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.updateCurrentUserTotpName(newTotp("改名后的名称", "", "JG7Y3TK4RO4KJLND")));

        assertEquals("2FA条目不存在", exception.getMessage());
    }

    /** 修改只能落名称：即使请求带了新密钥和新账号也不能进更新实体 */
    @Test
    void updateShouldOnlyPersistName() {
        MiniTotp stored = newTotp("openai", "old@mail.com", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(3);
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.update(any(MiniTotp.class), any())).thenReturn(1);

        MiniTotp request = newTotp("  OpenAI  ", "hacker@mail.com", "AAAAAAAAAAAAAAAA"); // 试图顺带改密钥的请求
        request.setId(3);
        assertTrue(service.updateCurrentUserTotpName(request));

        ArgumentCaptor<MiniTotp> entityCaptor = ArgumentCaptor.forClass(MiniTotp.class); // 更新实体
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class); // 更新条件
        verify(mapper).update(entityCaptor.capture(), updateCaptor.capture());

        MiniTotp updateEntity = entityCaptor.getValue(); // 实际提交的更新实体
        assertEquals("OpenAI", updateEntity.getName());
        assertNull(updateEntity.getSecret());
        assertNull(updateEntity.getAccount());
        assertNull(updateEntity.getUserOpenid());
        assertNull(updateEntity.getId());

        LambdaUpdateWrapper<MiniTotp> updateWrapper = (LambdaUpdateWrapper<MiniTotp>) updateCaptor.getValue(); // 更新归属条件
        updateWrapper.getSqlSegment();
        Map<String, Object> values = updateWrapper.getParamNameValuePairs(); // 更新条件参数值
        assertTrue(values.containsValue(3));
        assertTrue(values.containsValue("openid-a"));
    }

    /** 改名时空名称应被拒绝 */
    @Test
    void updateShouldRejectBlankName() {
        MiniTotp stored = newTotp("openai", "", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(3);
        when(mapper.selectOne(any())).thenReturn(stored);

        MiniTotp request = newTotp("   ", "", null); // 空名称请求
        request.setId(3);

        ParamException exception = assertThrows(ParamException.class,
                () -> service.updateCurrentUserTotpName(request));

        assertEquals("名称不能为空", exception.getMessage());
    }

    /** 置顶应写入接近当前时间的毫秒时间戳 */
    @Test
    void topShouldWriteCurrentTimestamp() {
        MiniTotp stored = newTotp("openai", "", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(3);
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.update(any(MiniTotp.class), any())).thenReturn(1);

        long before = System.currentTimeMillis(); // 调用前的时间戳
        assertTrue(service.setCurrentUserTotpTop(3, true));
        long after = System.currentTimeMillis(); // 调用后的时间戳

        ArgumentCaptor<MiniTotp> entityCaptor = ArgumentCaptor.forClass(MiniTotp.class); // 置顶更新实体
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class); // 置顶更新条件
        verify(mapper).update(entityCaptor.capture(), updateCaptor.capture());

        MiniTotp updateEntity = entityCaptor.getValue(); // 实际提交的更新实体
        assertTrue(updateEntity.getSortOrder() >= before && updateEntity.getSortOrder() <= after);
        // 置顶不能顺带改动名称、密钥等业务字段
        assertNull(updateEntity.getName());
        assertNull(updateEntity.getSecret());
        assertNull(updateEntity.getAccount());

        LambdaUpdateWrapper<MiniTotp> updateWrapper = (LambdaUpdateWrapper<MiniTotp>) updateCaptor.getValue(); // 置顶归属条件
        updateWrapper.getSqlSegment();
        Map<String, Object> values = updateWrapper.getParamNameValuePairs(); // 置顶条件参数值
        assertTrue(values.containsValue(3));
        assertTrue(values.containsValue("openid-a"));
    }

    /** 后置顶的条目排序值应大于先置顶的，保证排在更前面 */
    @Test
    void laterTopShouldOutrankEarlierTop() throws InterruptedException {
        MiniTotp stored = newTotp("openai", "", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(3);
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.update(any(MiniTotp.class), any())).thenReturn(1);

        service.setCurrentUserTotpTop(3, true);
        Thread.sleep(2); // 拉开两次置顶的毫秒时间戳
        service.setCurrentUserTotpTop(3, true);

        ArgumentCaptor<MiniTotp> entityCaptor = ArgumentCaptor.forClass(MiniTotp.class); // 两次置顶的更新实体
        verify(mapper, times(2)).update(entityCaptor.capture(), any());
        assertTrue(entityCaptor.getAllValues().get(1).getSortOrder()
                > entityCaptor.getAllValues().get(0).getSortOrder());
    }

    /** 取消置顶应把排序值重置为 0 */
    @Test
    void cancelTopShouldResetSortOrder() {
        MiniTotp stored = newTotp("openai", "", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(3);
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.update(any(MiniTotp.class), any())).thenReturn(1);

        assertTrue(service.setCurrentUserTotpTop(3, false));

        ArgumentCaptor<MiniTotp> entityCaptor = ArgumentCaptor.forClass(MiniTotp.class); // 取消置顶更新实体
        verify(mapper).update(entityCaptor.capture(), any());
        assertEquals(0L, entityCaptor.getValue().getSortOrder());
    }

    /** 置顶别人的条目应被拒绝 */
    @Test
    void topShouldRejectTotpNotOwnedByCurrentUser() {
        when(mapper.selectOne(any())).thenReturn(null);

        ParamException exception = assertThrows(ParamException.class,
                () -> service.setCurrentUserTotpTop(9, true));

        assertEquals("2FA条目不存在", exception.getMessage());
    }

    /** 新增时排序值应为未置顶的 0 */
    @Test
    void addShouldStartUnpinned() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(MiniTotp.class))).thenAnswer(invocation -> {
            MiniTotp totp = invocation.getArgument(0); // 实际插入实体
            assertEquals(0L, totp.getSortOrder());
            return 1;
        });

        assertTrue(service.addCurrentUserTotp(newTotp("openai", "", "JG7Y3TK4RO4KJLND")));
    }

    /** 列表应先按置顶排序值倒序，再按 id 升序，避免改名把条目顶上去 */
    @Test
    void listShouldOrderBySortOrderThenId() {
        service.listCurrentUserTotps();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> listCaptor = ArgumentCaptor.forClass(Wrapper.class); // 列表查询条件
        verify(mapper).selectList(listCaptor.capture());
        String sqlSegment = listCaptor.getValue().getSqlSegment(); // 列表查询 SQL 片段
        assertTrue(sqlSegment.contains("ORDER BY"));
        assertTrue(sqlSegment.replaceAll("\\s+", " ").contains("sort_order DESC"));
        assertTrue(sqlSegment.replaceAll("\\s+", " ").contains("id ASC"));
        assertTrue(sqlSegment.indexOf("sort_order") < sqlSegment.indexOf("id ASC"));
        // 兜底刻意不用 update_time，否则改名会让条目往上跳
        assertFalse(sqlSegment.contains("update_time"));
    }

    /** 删除时必须带当前用户条件 */
    @Test
    void deleteShouldScopeToCurrentUser() {
        MiniTotp stored = newTotp("openai", "", "JG7Y3TK4RO4KJLND"); // 库里已有的条目
        stored.setId(5);
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.delete(any())).thenReturn(1);

        assertTrue(service.deleteCurrentUserTotp(5));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> deleteCaptor = ArgumentCaptor.forClass(Wrapper.class); // 删除条件
        verify(mapper).delete(deleteCaptor.capture());
        LambdaQueryWrapper<MiniTotp> deleteWrapper = (LambdaQueryWrapper<MiniTotp>) deleteCaptor.getValue(); // 删除 Wrapper
        deleteWrapper.getSqlSegment();
        Map<String, Object> values = deleteWrapper.getParamNameValuePairs(); // 删除条件参数值
        assertTrue(values.containsValue(5));
        assertTrue(values.containsValue("openid-a"));
    }

    /** 删除不存在的条目应被拒绝 */
    @Test
    void deleteShouldRejectMissingId() {
        ParamException exception = assertThrows(ParamException.class,
                () -> service.deleteCurrentUserTotp(null));

        assertEquals("2FA条目不存在", exception.getMessage());
    }

    /** 列表查询必须按当前用户隔离 */
    @Test
    void listShouldScopeToCurrentUser() {
        service.listCurrentUserTotps();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniTotp>> listCaptor = ArgumentCaptor.forClass(Wrapper.class); // 列表查询条件
        verify(mapper).selectList(listCaptor.capture());
        LambdaQueryWrapper<MiniTotp> listWrapper = (LambdaQueryWrapper<MiniTotp>) listCaptor.getValue(); // 列表 Wrapper
        listWrapper.getSqlSegment();
        assertTrue(listWrapper.getParamNameValuePairs().containsValue("openid-a"));
    }

    /** 未登录时应拒绝操作 */
    @Test
    void shouldRejectWhenCurrentUserMissing() {
        BaseContext.removeCurrentId();

        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserTotp(newTotp("openai", "", "JG7Y3TK4RO4KJLND")));

        assertEquals("登录信息有误", exception.getMessage());
    }
}
