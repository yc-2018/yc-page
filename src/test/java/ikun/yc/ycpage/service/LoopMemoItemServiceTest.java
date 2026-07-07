package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferRequest;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 循环备忘记录服务契约测试
 *
 * @author Codex
 * @since 2026/07/08
 */
class LoopMemoItemServiceTest {

    /** 校验循环记录转移接口使用明确请求和响应 DTO */
    @Test
    void transferLoopMemoItemsShouldExposeDtoContract() throws Exception {
        Method method = LoopMemoItemService.class.getMethod("transferLoopMemoItems", LoopMemoItemTransferRequest.class); // 转移接口方法

        assertEquals(LoopMemoItemTransferResponse.class, method.getReturnType());
    }

    /** 校验循环记录转移请求保留源、目标和多选记录 ID */
    @Test
    void transferRequestShouldHoldSourceTargetAndLoopItemIds() {
        LoopMemoItemTransferRequest request = new LoopMemoItemTransferRequest(); // 循环记录转移请求
        request.setSourceMemoId(1);
        request.setTargetMemoId(2);
        request.setLoopItemIds(Arrays.asList(11, 12));

        assertEquals(1, request.getSourceMemoId());
        assertEquals(2, request.getTargetMemoId());
        assertEquals(Arrays.asList(11, 12), request.getLoopItemIds());
    }

    /** 校验循环记录转移响应带回两边最新循环次数 */
    @Test
    void transferResponseShouldHoldLatestCounts() {
        LoopMemoItemTransferResponse response = new LoopMemoItemTransferResponse(); // 循环记录转移响应
        response.setSourceMemoId(1);
        response.setTargetMemoId(2);
        response.setMovedCount(2);
        response.setSourceNumberOfRecurrences(3);
        response.setTargetNumberOfRecurrences(7);

        assertEquals(1, response.getSourceMemoId());
        assertEquals(2, response.getTargetMemoId());
        assertEquals(2, response.getMovedCount());
        assertEquals(3, response.getSourceNumberOfRecurrences());
        assertEquals(7, response.getTargetNumberOfRecurrences());
    }
}
