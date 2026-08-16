package ikun.yc.ycpage.common;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import ikun.yc.ycpage.entity.MiniAccountMemo;
import ikun.yc.ycpage.entity.MiniCommonPhrase;
import ikun.yc.ycpage.entity.MiniUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 公共审计时间自动填充测试。 */
class MyMetaObjecthandlerTest {
    private final MyMetaObjecthandler handler = new MyMetaObjecthandler(); // 被测自动填充处理器

    /** 初始化测试实体的 MyBatis Plus 表信息。 */
    @BeforeAll
    static void initTableInfo() {
        initTableInfo(MiniCommonPhrase.class);
        initTableInfo(MiniAccountMemo.class);
        initTableInfo(MiniUser.class);
    }

    /** 标准 createTime/updateTime 字段应在插入时同时填充。 */
    @Test
    void insertShouldFillStandardAuditFields() {
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 标准审计字段实体

        handler.insertFill(SystemMetaObject.forObject(phrase));

        assertNotNull(phrase.getCreateTime());
        assertNotNull(phrase.getUpdateTime());
    }

    /** 历史 createdAt/updatedAt 字段应在插入时同时填充。 */
    @Test
    void insertShouldFillLegacyAuditFields() {
        MiniAccountMemo memo = new MiniAccountMemo(); // 历史审计字段命名实体

        handler.insertFill(SystemMetaObject.forObject(memo));

        assertNotNull(memo.getCreatedAt());
        assertNotNull(memo.getUpdatedAt());
    }

    /** 两套更新时间字段都应在更新时填充。 */
    @Test
    void updateShouldFillBothAuditFieldNames() {
        MiniCommonPhrase phrase = new MiniCommonPhrase(); // 标准更新时间实体
        MiniAccountMemo memo = new MiniAccountMemo(); // 历史更新时间实体

        handler.updateFill(SystemMetaObject.forObject(phrase));
        handler.updateFill(SystemMetaObject.forObject(memo));

        assertNotNull(phrase.getUpdateTime());
        assertNotNull(memo.getUpdatedAt());
    }

    /** 业务时间字段不属于公共审计时间，不应被自动填充。 */
    @Test
    void insertShouldNotFillBusinessTimeFields() {
        MiniUser user = new MiniUser(); // 同时包含审计时间与业务时间的实体

        handler.insertFill(SystemMetaObject.forObject(user));

        assertNotNull(user.getCreateTime());
        assertNotNull(user.getUpdateTime());
        assertNull(user.getLastLoginTime());
    }

    /** 初始化单个实体的 MyBatis Plus 表信息。 */
    private static void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) return;
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), ""); // 测试表信息构建器
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
