package ikun.yc.ycpage.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 循环记录评论 Mapper XML 回归测试
 */
class LoopMemoItemCommentMapperXmlTest {

    /**
     * 评论预览必须返回乐观锁版本号，确保前端可以直接修改预览评论
     */
    @Test
    void previewQueryShouldSelectVersion() throws IOException {
        String resourcePath = "/mapper/LoopMemoItemCommentMapper.xml"; // Mapper XML 资源路径
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            String mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8); // Mapper XML 内容
            assertTrue(mapperXml.contains("c.version,"));
        }
    }
}
