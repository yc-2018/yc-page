package ikun.yc.ycpage.entity;

import ikun.yc.ycpage.common.exception.ParamException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 备忘图片字段校验测试
 *
 * @author Codex
 * @since 2026/07/13
 */
class MemoTest {

    /** 校验 999 个字符允许保存 */
    @Test
    void validateImgArrShouldAllow999Characters() {
        Memo memo = new Memo(); // 待校验备忘
        memo.setImgArr(String.join("", Collections.nCopies(999, "a")));

        assertDoesNotThrow(memo::validateImgArr);
    }

    /** 校验超过 999 个字符会被拒绝 */
    @Test
    void validateImgArrShouldReject1000Characters() {
        Memo memo = new Memo(); // 待校验备忘
        memo.setImgArr(String.join("", Collections.nCopies(1000, "a")));

        assertThrows(ParamException.class, memo::validateImgArr);
    }

    /** 校验后端不限制图片地址数量 */
    @Test
    void validateImgArrShouldNotRejectImageCount() {
        Memo memo = new Memo(); // 包含七个短图片地址的备忘
        memo.setImgArr("a,b,c,d,e,f,g");

        assertDoesNotThrow(memo::validateImgArr);
    }
}
