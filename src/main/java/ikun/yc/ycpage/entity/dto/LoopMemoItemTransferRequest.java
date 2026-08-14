package ikun.yc.ycpage.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 循环备忘记录转移请求
 *
 * @author Codex
 * @since 2026/07/08
 */
@Data
@NoArgsConstructor
public class LoopMemoItemTransferRequest {

    private Integer sourceMemoId; // 源循环备忘主键

    private Integer targetMemoId; // 目标循环备忘主键

    private Integer sourceMemoVersion; // 源循环备忘版本号

    private Integer targetMemoVersion; // 目标循环备忘版本号

    private List<Integer> loopItemIds; // 要转移的循环记录主键列表
}
