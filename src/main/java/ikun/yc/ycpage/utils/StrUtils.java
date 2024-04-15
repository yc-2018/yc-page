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


    /**
     * 检查给定字符串是否等于可变参数中的任何一个字符串。
     *
     * @param str     主字符串，将与数组中的字符串进行比较。
     * @param strings 可变参数，包含需要比较的字符串。
     * @return 如果主字符串等于数组中的任何一个字符串，则返回true；否则返回false。
     * 如果主字符串为null或数组为空，同样返回false。
     */
    public static boolean eqOr(String str, String... strings) {
        if (str == null || strings == null) return false;
        for (String s : strings) return str.equals(s);
        return false;
    }

    /**
     * 检查给定字符串是否不等于可变参数中的任何一个字符串。
     *
     * @param str     主字符串，将与数组中的字符串进行比较。
     * @param strings 可变参数，包含需要比较的字符串。
     * @return 如果主字符串不等于数组中的任何一个字符串，则返回true；否则返回false。
     * 如果主字符串为null或数组为空，同样返回false。
     */
    public static boolean neOr(String str, String... strings){
        if (str == null || strings == null) return false;
        return!eqOr(str, strings);
    }


    /**
     * 检查给定字符串是否以可变参数中的任何一个字符串开头。
     *
     * @param str      主字符串，将与数组中的字符串进行比较。
     * @param prefixArr 可变参数，包含需要比较的字符串。
     * @return 如果主字符串以数组中的任何一个字符串开头，则返回true；否则返回false。
     * 如果主字符串为null或数组为空，同样返回false。
     */
    public static boolean startWithOr(String str, String... prefixArr) {
        if (str == null || prefixArr == null) return false;
        for (String prefix : prefixArr) if (str.startsWith(prefix)) return true;
        return false;
    }

}
