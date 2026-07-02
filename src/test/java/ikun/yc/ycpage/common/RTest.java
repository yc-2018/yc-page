package ikun.yc.ycpage.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ikun.yc.ycpage.entity.dto.MemoIncompleteCountDto;
import ikun.yc.ycpage.service.MemoService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 通用响应结构测试
 *
 * @author cgl
 * @since 2026/07/02
 */
class RTest {

    /** 校验通用响应不再暴露动态 map 字段 */
    @Test
    void successShouldNotSerializeDynamicMap() {
        ObjectMapper objectMapper = new ObjectMapper(); // JSON 处理器
        JsonNode responseJson = objectMapper.valueToTree(R.success("ok")); // 响应 JSON

        assertEquals(1, responseJson.get("code").asInt());
        assertEquals("ok", responseJson.get("data").asText());
        assertFalse(responseJson.has("map"));
    }

    /** 校验待办预加载统计使用明确 DTO 契约 */
    @Test
    void memoServiceShouldExposeIncompleteCountDtoContract() throws Exception {
        Method method = MemoService.class.getMethod("getIncompleteCounts", Integer.class); // 预加载统计方法
        ParameterizedType returnType = assertInstanceOf(ParameterizedType.class, method.getGenericReturnType()); // 带泛型返回值

        assertEquals(List.class, method.getReturnType());
        assertEquals(MemoIncompleteCountDto.class, returnType.getActualTypeArguments()[0]);
    }
}
