package ikun.yc.ycpage.entity.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 备忘类型
 *
 * @author ChenGuangLong
 * @since 2024/04/23 14:17:02
 */
@Getter
@AllArgsConstructor
public enum MemoType {
  NORMAL("0 ", "普通"),
  LOOP("1 ", "循环"),
  LONG_TERM("2 ", "长期"),
  URGENT("3 ", "紧急"),
  ENGLISH("4 ", "英语"),
  DIARY("5 ", "日记"),
  WORK("6 ", "工作"),
  OTHER("7 ", "其他");

  private final String code;
  private final String name;
}
