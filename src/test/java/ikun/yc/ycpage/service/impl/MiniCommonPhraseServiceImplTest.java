package ikun.yc.ycpage.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniCommonPhrase;
import ikun.yc.ycpage.mapper.MiniCommonPhraseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 小程序打卡常用语 Service 单元测试。 */
class MiniCommonPhraseServiceImplTest {
    private MiniCommonPhraseServiceImpl service; // 被测常用语服务
    private MiniCommonPhraseMapper mapper; // 模拟数据访问层

    /** 初始化当前用户和模拟 Mapper */
    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(MiniCommonPhrase.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), ""); // 测试用表信息构建器
            TableInfoHelper.initTableInfo(assistant, MiniCommonPhrase.class);
        }
        service = new MiniCommonPhraseServiceImpl();
        mapper = mock(MiniCommonPhraseMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        BaseContext.setCurrentId("openid-a");
    }

    /** 清除测试线程中的用户信息 */
    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    /** 新增时应去除首尾空白并绑定当前用户 */
    @Test
    void addShouldTrimContentAndBindCurrentUser() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(MiniCommonPhrase.class))).thenAnswer(invocation -> {
            MiniCommonPhrase phrase = invocation.getArgument(0); // 实际插入实体
            assertEquals("openid-a", phrase.getUserOpenid());
            assertEquals("到店打卡", phrase.getContent());
            assertEquals(0L, phrase.getSortOrder());
            return 1;
        });
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 新增请求
        phrase.setContent("  到店打卡  ");

        assertTrue(service.addCurrentUserPhrase(phrase));
    }

    /** 空常用语应被拒绝 */
    @Test
    void addShouldRejectBlankContent() {
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 空内容请求
        phrase.setContent("   ");

        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserPhrase(phrase));

        assertEquals("常用语不能为空", exception.getMessage());
    }

    /** 超长常用语应被拒绝 */
    @Test
    void addShouldRejectOverlongContent() {
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 超长内容请求
        phrase.setContent("a".repeat(256));

        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserPhrase(phrase));

        assertEquals("常用语不能超过255个字", exception.getMessage());
    }

    /** 同一用户的重复常用语应被拒绝 */
    @Test
    void addShouldRejectDuplicateContent() {
        when(mapper.selectCount(any())).thenReturn(1L);
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 重复内容请求
        phrase.setContent("到店打卡");

        ParamException exception = assertThrows(ParamException.class,
                () -> service.addCurrentUserPhrase(phrase));

        assertEquals("常用语已存在", exception.getMessage());
    }

    /** 修改时查询不到当前用户数据应拒绝操作 */
    @Test
    void updateShouldRejectPhraseNotOwnedByCurrentUser() {
        when(mapper.selectOne(any())).thenReturn(null);
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 修改请求
        phrase.setId(9);
        phrase.setContent("修改后的内容");

        ParamException exception = assertThrows(ParamException.class,
                () -> service.updateCurrentUserPhrase(phrase));

        assertEquals("常用语不存在", exception.getMessage());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniCommonPhrase>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class); // 用户归属查询条件
        verify(mapper).selectOne(queryCaptor.capture());
        LambdaQueryWrapper<MiniCommonPhrase> queryWrapper =
                (LambdaQueryWrapper<MiniCommonPhrase>) queryCaptor.getValue(); // 用户归属查询 Wrapper
        queryWrapper.getSqlSegment();
        Map<String, Object> values = queryWrapper.getParamNameValuePairs(); // 查询参数值
        assertTrue(values.containsValue(9));
        assertTrue(values.containsValue("openid-a"));
    }

    /** 置顶时应使用当前最大排序值加一 */
    @Test
    void topShouldUseNextSortOrder() {
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 当前用户待置顶常用语
        phrase.setId(3);
        MiniCommonPhrase currentTop = new MiniCommonPhrase(); // 当前排序最前的常用语
        currentTop.setSortOrder(7L);
        when(mapper.selectOne(any())).thenReturn(phrase, currentTop);

        service.topCurrentUserPhrase(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MiniCommonPhrase>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class); // 置顶更新条件
        ArgumentCaptor<MiniCommonPhrase> entityCaptor = ArgumentCaptor.forClass(MiniCommonPhrase.class); // 置顶更新实体
        verify(mapper).update(entityCaptor.capture(), updateCaptor.capture());
        assertEquals(8L, entityCaptor.getValue().getSortOrder());
        LambdaUpdateWrapper<MiniCommonPhrase> updateWrapper =
                (LambdaUpdateWrapper<MiniCommonPhrase>) updateCaptor.getValue(); // 置顶用户归属条件
        updateWrapper.getSqlSegment();
        assertTrue(updateWrapper.getParamNameValuePairs().containsValue("openid-a"));
    }
}
