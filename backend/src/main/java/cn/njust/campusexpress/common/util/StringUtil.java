package cn.njust.campusexpress.common.util;

//处理字符串工具类
public final class StringUtil {

    private StringUtil() {
    }

    //清理字符串：去除前后空格，空串设为null
    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
