package com.dfrdev.tarn.util.text;


import com.dfrdev.tarn.core.TarnException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串，时间通用工具类
 */
public final class TextKit {

    private String str;


    @SuppressWarnings("unused")
    private TextKit() {
    }

    /**
     * 创建一个新的Txt字符串处理对象.
     * 可以使用该对象处理与正则表达式相关的操作.
     *
     * @param str 要被处理的字符串.
     */
    public TextKit(String str) {
        this.str = str;
    }

    public static String firstLetterToUpper(String string) {
        char[] buffer = string.toCharArray();
        buffer[0] = Character.toUpperCase(string.charAt(0));
        return new String(buffer);
    }

    public static String firstLetterToUppers(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 首字母变小写
     */
    public static String firstCharToLowerCase(String str) {
        Character firstChar = str.charAt(0);
        String tail = str.substring(1);
        str = Character.toLowerCase(firstChar) + tail;
        return str;
    }

    /**
     * 首字母变大写
     */
    public static String firstCharToUpperCase(String str) {
        Character firstChar = str.charAt(0);
        String tail = str.substring(1);
        str = Character.toUpperCase(firstChar) + tail;
        return str;
    }

    /**
     * 字符串为 null 或者为  "" 时返回 true
     */
    public static boolean isBlank(String str) {
        return str == null || "".equals(str.trim()) ? true : false;
    }

    /**
     * 字符串不为 null 而且不为  "" 时返回 true
     */
    public static boolean notBlank(String str) {
        return str == null || "".equals(str.trim()) ? false : true;
    }

    public static boolean notBlank(String... strings) {
        if (strings == null)
            return false;
        for (String str : strings)
            if (str == null || "".equals(str.trim()))
                return false;
        return true;
    }

    public static boolean notNull(Object... paras) {
        if (paras == null)
            return false;
        for (Object obj : paras)
            if (obj == null)
                return false;
        return true;
    }

    /**
     * 截字符串前面的指定数量个字符.
     * 如果字节长度超过该数量,则后面用省略号代替(因为省略号会占去两个字符，因此实际显示的字符数=toCount-2).
     *
     * @param str     要截取取的字字符串。
     * @param toCount 要截取的字符数（一个汉字计两个字符）,此参数值为负数或0时，将不截取直接返回该str.
     * @return 截取后的新字符串。
     */
    public static String cut(String str, int toCount) {
        if (toCount <= 0) return str;
        try {
            StringBuffer reStr = new StringBuffer("");
            char[] tempChar = str.toCharArray();
            for (int i = 0; i < tempChar.length; i++) {
                byte[] b = String.valueOf(tempChar[i]).getBytes("utf-8");
                toCount -= b.length > 1 ? 2 : 1;
                if (toCount <= 0) {
                    if (i == tempChar.length - 1) {
                        reStr.append(tempChar[i]);
                    } else {
                        reStr.append("…");
                        break;
                    }
                } else {
                    reStr.append(tempChar[i]);
                }
            }
            return reStr.toString();
        } catch (java.io.UnsupportedEncodingException ex) {
            return str;
        }
    }

    /**
     * 将字符串数组元素按指定的字符串连成一个新的字符串。
     *
     * @param arr     要连接的字符串数组.
     * @param joinStr 连接字符串. 如果此字符串为空，则使用逗号加一个空格(, )进行连接。
     * @return 返回arr的每个下标元素按joinStr连接成的新的字符串. 如果数组arr为空(null),则返回结果也将为null.
     */
    public static String join(String[] arr, String joinStr) {
        if (null == arr) return null;
        if (null == joinStr) joinStr = ", ";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(joinStr);
            sb.append(arr[i]);
        }
        return sb.toString();
    }


    /**
     * 将指定的字符串内容进行HTML标签转换过滤. <br />
     * 此方法会将内容中的HTML标签转换成普通字符输出以防止非法的HTML代码被执行，并自动为URL地址、邮件地址添加链接.<br />
     * <strong>注意：</strong> 由编辑器添加的内容，不能调用此方法，否则编辑器上传的所有HTML代码将被转换成普通字符输出。
     *
     * @param str 要进行HTML标签过滤处理处理的原字符串。
     * @return 转换之后的字符串。
     */
    @SuppressWarnings("unused")
    public static String encodeHTML(String str) {
        String s = str.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&acute; ").replaceAll("  ", " &nbsp;").replaceAll("\\r\\n", "<br />").replaceAll("\\n", "<br />");
        TextKit rep = new TextKit(s);
        rep.replace("http://([%#=&\\?\\./a-zA-Z0-9]+)", "<a href=\"http://$1\" target=\"_blank\">http://$1</a>");
        rep.replace("([-_a-z0-9]+?)@([-_\\.a-zA-Z0-9]{5,})", "<a href=\"mailto:$1@$2\" target=\"_blank\">$1@$2</a>");
        return rep.toString();
    }

    /**
     * 以%YYYY-%MM-%DD %hh:%mm:%ss的形式格式化指定的日期时间字符串.
     *
     * @param date 参数中的日期字符串必须是标准的%Y-%M-%D %h:%m:%s格式，或者为空(null或"")，否则将出错；
     *             如果date的值为空，将返回当前的日期时间。
     * @return 格式化后的日期时间字符串
     * @see #parseDate(String, String)
     */
    public static String parseDate(String date) throws TarnException {
        return parseDate(date, null);
    }

    /**
     * 用于格式化日期时间. <br />
     *
     * @param date  要格式化的日期时间字符串. <blockquote>为空(null)或空字符串(""),将格式化当前的时间.</blockquote>
     * @param style 格式化字符串样式.<blockquote> 为空(null)或空字符串(""),将按照此格式进行格式化：<span style="color:#c00">%YYYY-%MM-%DD %hh:%mm:%ss</span><br />
     *              style中可取的格式及其所代表的内容如下：<br /><pre>
     *                                                                                                                                   %YYYY   4个字符年
     *                                                                                                                                   %YY     2个字符年
     *                                                                                                                                   %MMM    3个字符英文简写月
     *                                                                                                                                   %MM     2个字符月(1-9月前补0)
     *                                                                                                                                   %M      月
     *                                                                                                                                   %DD     2个字符日(1-9日前补0)
     *                                                                                                                                   %D      月中的日期数字
     *                                                                                                                                   %hh     24小时制2个字符时(1-9前补0)
     *                                                                                                                                   %h      24小时制时
     *                                                                                                                                   %HH     12小时制2个字符时(1-9前补0)
     *                                                                                                                                   %H      12小时制时
     *                                                                                                                                   %mm     2位数分(1-9前补0)
     *                                                                                                                                   %m      分
     *                                                                                                                                   %ss     2位数秒(1-9前补0)
     *                                                                                                                                   %s      秒
     *                                                                                                                                   %w      周中的日（数字）
     *                                                                                                                                   %W      周中的日（汉字）
     *                                                                                                                                   %A      上午还是下午（返回汉字的“上午”或“下午”）
     *                                                                                                                                   %a      上午还是下午（返回英文字母 AM 或 PM）</pre><br /><br />
     *              <strong>示例：</strong><br />  现在是： <span style="color:#c00">
     *              &lt;%=App.fmtDate(null,"%YYYY年%M月%D日　星期%W　时间是：%h:%m")%&gt;</span></blockquote>
     * @return 指定格式的日期时间字符串.
     * @see #parseDate(String)
     */
    public static String parseDate(String date, String style) throws TarnException {
        return parseDate(date, style, false);
    }

    /**
     * 用于格式化日期时间. <br />
     *
     * @param date           要格式化的日期时间字符串. <blockquote>为空(null)或空字符串(""),将格式化当前的时间.</blockquote>
     * @param style          格式化字符串样式.<blockquote> 为空(null)或空字符串(""),将按照此格式进行格式化：<span style="color:#c00">%YYYY-%MM-%DD %hh:%mm:%ss</span><br />
     *                       style中可取的格式及其所代表的内容如下：<br /><pre>
     *                                                                                                                                                                                         %YYYY   4个字符年
     *                                                                                                                                                                                         %YY     2个字符年
     *                                                                                                                                                                                         %MMM    3个字符英文简写月
     *                                                                                                                                                                                         %MM     2个字符月(1-9月前补0)
     *                                                                                                                                                                                         %M      月
     *                                                                                                                                                                                         %DD     2个字符日(1-9日前补0)
     *                                                                                                                                                                                         %D      月中的日期数字
     *                                                                                                                                                                                         %hh     24小时制2个字符时(1-9前补0)
     *                                                                                                                                                                                         %h      24小时制时
     *                                                                                                                                                                                         %HH     12小时制2个字符时(1-9前补0)
     *                                                                                                                                                                                         %H      12小时制时
     *                                                                                                                                                                                         %mm     2位数分(1-9前补0)
     *                                                                                                                                                                                         %m      分
     *                                                                                                                                                                                         %ss     2位数秒(1-9前补0)
     *                                                                                                                                                                                         %s      秒
     *                                                                                                                                                                                         %w      周中的日（数字）
     *                                                                                                                                                                                         %W      周中的日（汉字）
     *                                                                                                                                                                                         %A      上午还是下午（返回汉字的“上午”或“下午”）
     *                                                                                                                                                                                         %a      上午还是下午（返回英文字母 AM 或 PM）</pre><br /><br />
     *                       <strong>示例：</strong><br />  现在是： <span style="color:#c00">
     *                       &lt;%=App.fmtDate(null,"%YYYY年%M月%D日　星期%W　时间是：%h:%m")%&gt;</span></blockquote>
     * @param isNullDefTdate 如果要转换的date为空时，是否取当天时间。true:是，false:不是
     * @return 指定格式的日期时间字符串.
     * @see #parseDate(String)
     */
    public static String parseDate(String date, String style, boolean isNullDefTdate) throws TarnException {
        final String[] wa = {"", "日", "一", "二", "三", "四", "五", "六"};
        final String[] MMM = {"Jan", "Feb", "Mar", "Apr", "Mar", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int YYYY, M, D, h, m, s, w, H;
        String YY;
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (date == null || date.equals("")) {
            if (isNullDefTdate) date = df.format(new java.util.Date());
            if (style == null || style.equals("")) {
                return date;
            }
        } else if (date.indexOf(" ") == -1) {
            date += " 0:0:0";
        }
        String returnStr = "";
        if (!date.equals("")) {
            date = date.replaceAll("T|t", " ");
            java.util.Date dt;
            try {
                dt = df.parse(date);
            } catch (java.text.ParseException e) {
                throw new TarnException("无效的日期时间格式" + date);
            }
            if (style == null || style.equals("")) {
                style = "%YYYY-%MM-%DD %hh:%mm:%ss";
            }
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(dt);
            w = calendar.get(java.util.Calendar.DAY_OF_WEEK);
            YYYY = calendar.get(java.util.Calendar.YEAR);
            YY = String.valueOf(YYYY).substring(2);
            M = calendar.get(java.util.Calendar.MONTH) + 1;
            D = calendar.get(java.util.Calendar.DAY_OF_MONTH);
            h = calendar.get(java.util.Calendar.HOUR_OF_DAY);
            m = calendar.get(java.util.Calendar.MINUTE);
            s = calendar.get(java.util.Calendar.SECOND);
            H = calendar.get(java.util.Calendar.HOUR);

            returnStr = style
                    .replaceAll("%YYYY", "" + YYYY)
                    .replaceAll("%MMM", MMM[M - 1])
                    .replaceAll("%YY", YY)
                    .replaceAll("%MM", M < 10 ? "0" + M : "" + M)
                    .replaceAll("%DD", D < 10 ? "0" + D : "" + D)
                    .replaceAll("%hh", h < 10 ? "0" + h : "" + h)
                    .replaceAll("%mm", m < 10 ? "0" + m : "" + m)
                    .replaceAll("%ss", s < 10 ? "0" + s : "" + s)
                    .replaceAll("%M", "" + M)
                    .replaceAll("%D", "" + D)
                    .replaceAll("%h", "" + h)
                    .replaceAll("%m", "" + m)
                    .replaceAll("%s", "" + s)
                    .replaceAll("%w", "" + (w + 1))
                    .replaceAll("%W", "" + wa[w])
                    .replaceAll("%HH", H < 10 ? "0" + H : "" + H)
                    .replaceAll("%H", "" + H)
                    .replaceAll("%A", h >= 12 ? "下午" : "上午")
                    .replaceAll("%a", h >= 12 ? "AM" : "PM");
        }
        return returnStr;

    }

    /**
     * 将给定的字符串s转化为一个双精度浮点数. 如果s不能转化, 则返回0.0D
     *
     * @param s 要被转化的字符串
     * @return 一个有效的双精度浮点数.
     * @see #parseDouble(String, double)
     * @see #parseDouble(String, double, int)
     */
    public static double parseDouble(String s) {
        return parseDouble(s, 0.0d);
    }

    /**
     * 将给定的字符串s转化为一个双精度浮点数. 如果s不能转化, 则返回defV
     *
     * @param s    要被转化的字符串
     * @param defV 当指定的字符串不能转换时,要返回的默认值.
     * @return 一个有效的双精度浮点数.
     * @see #parseDouble(String)
     * @see #parseDouble(String, double, int)
     */
    public static double parseDouble(String s, double defV) {
        try {
            return Double.parseDouble(new TextKit(s).replace("^(\\d+)(\\.*)(\\d*)(.|\r|\n)*$", "$1$2$3").toString());
        } catch (Exception e) {
            return defV;
        }
    }

    /**
     * 将给定的字符串s转化为一个指定小数位数的双精度浮点数. 如果s不能转化, 则返回defV
     *
     * @param s    要被转化的字符串
     * @param defV 当指定的字符串不能转换时,要返回的默认值.
     * @param dec  要保留的小数位数.
     *             dec的取值应该是一个大于1的整数，否则将直接以{@link #parseDouble(String)}的形式返回.
     * @return 一个有效的双精度浮点数
     * @see #parseDouble(String, double)
     * @see #parseDouble(String, double)
     */
    public static double parseDouble(String s, double defV, int dec) {
        double d = parseDouble(s, defV);
        if (dec > 2 && dec < 8) {
            double t = Math.pow(10.0, dec);
            return Math.round(d * t) / t;
        } else {
            return d;
        }
    }

    /**
     * 将给定的字符串s转化为一个整数. 如果s不能转化为一个整数,则返回0.
     *
     * @param s 要被转化的字符串
     * @return 一个有效的整数.
     * @see #parseInt(String, int)
     */
    public static int parseInt(String s) {
        return parseInt(s, 0);
    }

    /**
     * 将给定的字符串s转化为一个整数. 如果s不能转化为一个整数,则返回defV
     *
     * @param s    要被转化的字符串
     * @param defV 如果字符串不能被转化成一个有效的整数，程序将返回defV.
     * @return 一个有效的整数.
     * @see #parseInt(String)
     */
    public static int parseInt(String s, int defV) {
        try {
            return Integer.parseInt(new TextKit(s).replace("(\\d+)(.|\r|\n)*$", "$1").toString());
        } catch (Exception e) {
            return defV;
        }
    }

    /**
     * 将给定的双精度浮点数格式化为一个标准的千分位字符串输出（保留两位小数，不足补0）.
     * <br> 例如：1234.56将被格式化为：1,234.56
     *
     * @param d 要格式化的double数
     * @return 格式化之后生成的字符串.
     */
    public static String fmtNum(double d) {
        java.text.DecimalFormat df = new java.text.DecimalFormat(",##0.00");
        return df.format(d);
    }

    /**
     * 将给定的字符串s转化为一个长整数. 如果s不能转化为一个长整数,则返回0L
     *
     * @param s 要被转化的字符串
     * @return 一个有效的长整数.
     * @see #parseLong(String, long)
     */
    public static long parseLong(String s) {
        return parseLong(s, 0L);
    }

    /**
     * 将给定的字符串s转化为一个长整数. 如果s不能转化为一个长整数,则返回defV.
     *
     * @param s    要被转化的字符串.
     * @param defV 如果字符串不能被转化成一个有效的长整数，程序将返回defV.
     * @return 一个有效的长整数
     * @see #parseLong(String)
     */
    public static long parseLong(String s, long defV) {
        try {
            return Long.parseLong(new TextKit(s).replace("(\\d+)(.|\r|\n)*$", "$1").toString());
        } catch (Exception e) {
            return defV;
        }
    }

    /**
     * 以不区分大小写的方式检测某个字符串数组元素中是否包含另一个字符串.
     *
     * @param arr 被检测的字符串数组.
     * @param str 要检测的字符串.
     * @return true | false 如果arr的下标元素中包含str（不区分大小写）,将返回true, 否则返回false.
     */
    public static boolean containsIgnoreCase(String arr[], String str) {
        if (arr == null) return false;
        for (String anArr : arr) {
            if (anArr != null && anArr.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 以不区分大小写的方式检测某个字符串数组元素中是否包含另一个字符串.
     *
     * @param list 被检测的字符串集合.
     * @param str  要检测的字符串.
     * @return true | false 如果arr的下标元素中包含str（不区分大小写）,将返回true, 否则返回false.
     */
    public static boolean containsIgnoreCase(List<String> list, String str) {
        if (list == null) return false;
        for (String anArr : list) {
            if (anArr != null && anArr.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 测试字符串是否能够匹配该正则模式(完全匹配).
     *
     * @param regStr 要匹配的正则模式
     * @return 当匹配成功时返回true, 否则返回false.
     * @see #find(String)
     */
    public boolean test(String regStr) {
        Pattern pattern = Pattern.compile(regStr);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 测试字符串是否包含该正则模式.
     *
     * @param regStr 要匹配的正则模式
     * @return 当找到匹配时返回true, 否则返回false
     * @see #test(String)
     */
    public boolean find(String regStr) {
        Pattern pattern = Pattern.compile(regStr);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();

    }

    /**
     * 将字符串中符合regStr正则模式的字符串替换为s1. 此操作将替换所有符合正则匹配条件的字符串.
     *
     * @param regStr 用于匹配的正则表达式.
     * @param s1     要替换符合匹配内容的字符串.
     * @return 当前的Txt对象.
     * @see #replaceFirst(String, String)
     */
    public TextKit replace(String regStr, String s1) {
        Pattern pattern = Pattern.compile(regStr);
        Matcher matcher = pattern.matcher(str);
        str = matcher.replaceAll(s1);
        return this;
    }

    /**
     * 将字符串中符合正则匹配的字符串替换为s1. 此操作将仅替换一次符合正则匹配条件的字符串.
     *
     * @param regStr 用于匹配的正则表达式
     * @param s1     要替换符合匹配内容的字符串.
     * @return 当前的Txt对象.
     * @see #replace(String, String)
     */
    @SuppressWarnings("unused")
    public TextKit replaceFirst(String regStr, String s1) {
        Pattern pattern = Pattern.compile(regStr);
        Matcher matcher = pattern.matcher(str);
        str = matcher.replaceFirst(s1);
        return this;
    }

    /**
     * 将字符串中的s1替换成s2. 与{@link #replace(String, String)}方法不同的是，此方法仅限普通字符串替换，而不使用正则表达式。
     *
     * @param s1 要替换的普通字符串.
     * @param s2 要替换成的字符串
     * @return
     */
    public TextKit restr(String s1, String s2) {
        StringBuilder sb = new StringBuilder();
        int b, e = 0, len = s1.length();
        while ((b = str.indexOf(s1, e)) != -1) {
            sb.append(str.substring(e, b));
            sb.append(s2);
            e = b + len;
        }
        if (e < str.length()) sb.append(str.substring(e));
        str = sb.toString();
        return this;
    }


    /**
     * 返回字符串dest在字符串source中出现的次数.要查找的字符串是区分大小写的.
     *
     * @param source 原字符串.
     * @param dest   要搜索的字符串.
     * @return 字符串dest在字符串source中出现的次数.
     */
    public static int containSum(String source, String dest) {
        //这是一个很好的字符串查找算法： 查找dest在source中出现的次数
        return (source.length() - source.replaceAll(dest, "").length()) / dest.length();
    }

    /**
     * 格式化字符串<br>
     * 此方法只是简单将占位符 {} 按照顺序替换为参数<br>
     * 如果想输出 {} 使用 \\转义 { 即可，如果想输出 {} 之前的 \ 使用双转义符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "a", "b") =》 this is a for b<br>
     * 转义{}： format("this is \\{} for {}", "a", "b") =》 this is \{} for a<br>
     * 转义\： format("this is \\\\{} for {}", "a", "b") =》 this is \a for b<br>
     *
     * @param strPattern 字符串模板
     * @param argArray   参数列表
     * @return 结果
     */
    public static String format(String strPattern, Object... argArray) {
        return formatWith(strPattern, EMPTY_JSON, argArray);
    }

    /**
     * 格式化字符串<br>
     * 此方法只是简单将指定占位符 按照顺序替换为参数<br>
     * 如果想输出占位符使用 \\转义即可，如果想输出占位符之前的 \ 使用双转义符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "{}", "a", "b") =》 this is a for b<br>
     * 转义{}： format("this is \\{} for {}", "{}", "a", "b") =》 this is {} for a<br>
     * 转义\： format("this is \\\\{} for {}", "{}", "a", "b") =》 this is \a for b<br>
     *
     * @param strPattern  字符串模板
     * @param placeHolder 占位符，例如{}
     * @param argArray    参数列表
     * @return 结果
     * @since 5.7.14
     */
    public static char BACKSLASH = '\\';
    public static String EMPTY_JSON = "{}";

    public static String formatWith(String strPattern, String placeHolder, Object... argArray) {
        if (isBlank(strPattern) || isBlank(placeHolder) || argArray == null) {
            return strPattern;
        }
        final int strPatternLength = strPattern.length();
        final int placeHolderLength = placeHolder.length();

        // 初始化定义好的长度以获得更好的性能
        final StringBuilder sbuf = new StringBuilder(strPatternLength + 50);

        int handledPosition = 0;// 记录已经处理到的位置
        int delimIndex;// 占位符所在位置
        for (int argIndex = 0; argIndex < argArray.length; argIndex++) {
            delimIndex = strPattern.indexOf(placeHolder, handledPosition);
            if (delimIndex == -1) {// 剩余部分无占位符
                if (handledPosition == 0) { // 不带占位符的模板直接返回
                    return strPattern;
                }
                // 字符串模板剩余部分不再包含占位符，加入剩余部分后返回结果
                sbuf.append(strPattern, handledPosition, strPatternLength);
                return sbuf.toString();
            }

            // 转义符
            if (delimIndex > 0 && strPattern.charAt(delimIndex - 1) == BACKSLASH) {// 转义符
                if (delimIndex > 1 && strPattern.charAt(delimIndex - 2) == BACKSLASH) {// 双转义符
                    // 转义符之前还有一个转义符，占位符依旧有效
                    sbuf.append(strPattern, handledPosition, delimIndex - 1);
                    sbuf.append(utf8Str(argArray[argIndex]));
                    handledPosition = delimIndex + placeHolderLength;
                } else {
                    // 占位符被转义
                    argIndex--;
                    sbuf.append(strPattern, handledPosition, delimIndex - 1);
                    sbuf.append(placeHolder.charAt(0));
                    handledPosition = delimIndex + 1;
                }
            } else {// 正常占位符
                sbuf.append(strPattern, handledPosition, delimIndex);
                sbuf.append(utf8Str(argArray[argIndex]));
                handledPosition = delimIndex + placeHolderLength;
            }
        }

        // 加入最后一个占位符后所有的字符
        sbuf.append(strPattern, handledPosition, strPatternLength);

        return sbuf.toString();
    }

    /**
     * 格式化文本，使用 {varName} 占位<br>
     * map = {a: "aValue", b: "bValue"} format("{a} and {b}", map) ---=》 aValue and bValue
     *
     * @param template   文本模板，被替换的部分用 {key} 表示
     * @param map        参数值对
     * @param ignoreNull 是否忽略 {@code null} 值，忽略则 {@code null} 值对应的变量不被替换，否则替换为""
     * @return 格式化后的文本
     * @since 5.7.10
     */
    public static String format(CharSequence template, Map<?, ?> map, boolean ignoreNull) {
        if (null == template) {
            return null;
        }
        if (null == map || map.isEmpty()) {
            return template.toString();
        }

        String template2 = template.toString();
        String value;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            value = utf8Str(entry.getValue());
            if (null == value && ignoreNull) {
                continue;
            }
            template2 = template2.replaceAll("\\{" + entry.getKey() + "\\}", value);
        }
        return template2;
    }

    public static String utf8Str(Object obj) {
        return str(obj, StandardCharsets.UTF_8);
    }

    public static String str(Object obj, Charset charset) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof byte[]) {
            return str((byte[]) obj, charset);
        } else if (obj instanceof Byte[]) {
            return str((Byte[]) obj, charset);
        } else if (obj instanceof ByteBuffer) {
            return str((ByteBuffer) obj, charset);
        }

        return obj.toString();
    }

    /**
     * 返回处理之后的字符串.
     *
     * @return 处理之后的字符串.
     */
    @Override
    public String toString() {
        return str;
    }

    public static boolean containsIgnoreStr(String arr[], String str) {
        if (arr == null) return false;
        for (String anArr : arr) {
            if (anArr != null && anArr.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}
