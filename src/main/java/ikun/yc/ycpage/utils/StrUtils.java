package ikun.yc.ycpage.utils;

public class StrUtils {

    /**
     * 使用StringBuilder将多个字符串拼接成一个字符串
     *
     * @param strings 多个待拼接的字符串
     * @return 拼接后的字符串
     */
    public static String joins(String... strings) {
        // 参数校验
        if (strings == null || strings.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 遍历所有字符串并拼接
      for (String string : strings) {
        // 防止空指针异常，如果字符串为null，则添加空字符串
        String str = string != null ? string : "";
        sb.append(str);
      }
        return sb.toString();
    }

}
