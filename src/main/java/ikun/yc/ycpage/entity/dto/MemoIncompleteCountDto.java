package ikun.yc.ycpage.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待办未完成预加载统计
 *
 * @author cgl
 * @since 2026/07/02
 */
@Data
@NoArgsConstructor
public class MemoIncompleteCountDto {

    private Integer itemType; // 待办类型

    private Long count; // 未完成数量
}
