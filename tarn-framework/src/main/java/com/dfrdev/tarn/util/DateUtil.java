package com.dfrdev.tarn.util;


import com.dfrdev.tarn.core.TarnException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DateUtil {
    public static final String Format_Date = "yyyy-MM-dd";
    public static final String Format_Time = "HH:mm:ss";
    public static final String Format_LastModified = "EEE, dd MMM yyyy HH:mm:ss";
    public static final String Format_DateTime = "yyyy-MM-dd HH:mm:ss";
    private static ThreadLocal<Formats> formats = new ThreadLocal();

    public static SimpleDateFormat getFormat(String format) {
        if (formats.get() == null) {
            formats.set(new Formats());
        }
        if (format.equals("yyyy-MM-dd")) {
            return getDefaultDateFormat();
        }
        if (format.equals("HH:mm:ss")) {
            return getDefaultTimeFormat();
        }
        if (format.equals("yyyy-MM-dd HH:mm:ss")) {
            return getDefaultDateTimeFormat();
        }
        if (format.equals("EEE, dd MMM yyyy HH:mm:ss")) {
            return getLastModifiedFormat();
        }
        if ((formats.get()).Others == null) {
            (formats.get()).Others = new HashMap();
        }
        if (!(formats.get()).Others.containsKey(format)) {
            (formats.get()).Others.put(format, new SimpleDateFormat(format, Locale.ENGLISH));
        }
        return (SimpleDateFormat) (formats.get()).Others.get(format);
    }

    /**
     * 标准化日期，默认处理以空格区分的日期时间格式，空格前为日期，空格后为时间：<br>
     * 将以下字符替换为"-"
     *
     * <pre>
     * "."
     * "/"
     * "年"
     * "月"
     * </pre>
     * <p>
     * 将以下字符去除
     *
     * <pre>
     * "日"
     * </pre>
     * <p>
     * 将以下字符替换为":"
     *
     * <pre>
     * "时"
     * "分"
     * "秒"
     * </pre>
     * <p>
     * 当末位是":"时去除之（不存在毫秒时）
     *
     * @param dateStr 日期时间字符串
     * @return 格式化后的日期字符串
     */
    private static String normalize(CharSequence dateStr, boolean subtwo) {
        if (StringUtil.isEmpty(dateStr.toString())) {
            return dateStr.toString();
        }

        // 日期时间分开处理
        final List<String> dateAndTime = Arrays.asList(dateStr.toString().trim().split(" "));
        final int size = dateAndTime.size();
//        System.out.println("data size:" + size);
        if (!subtwo) {
            if (size < 1 || size > 2) {
                // 非可被标准处理的格式
                return dateStr.toString();
            }
        }

        final StringBuilder builder = new StringBuilder();

        // 日期部分（"\"、"/"、"."、"年"、"月"都替换为"-"）
        String datePart = dateAndTime.get(0).replaceAll("[/.年月]", "-");
        datePart = removeSuffix(datePart, "日");
        builder.append(datePart);

        // 时间部分
        if (size >= 2) {
            builder.append(' ');
            String timePart = dateAndTime.get(1).replaceAll("[时分秒]", ":");
            timePart = removeSuffix(timePart, ":");
            //将ISO8601中的逗号替换为.
            timePart = timePart.replace(',', '.');
            builder.append(timePart);
        }


        return builder.toString();
    }


    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static String subPre(CharSequence string, int toIndexExclude) {
        return sub(string, 0, toIndexExclude);
    }

    public static String sub(CharSequence str, int fromIndexInclude, int toIndexExclude) {
        if (isEmpty(str)) {
            return str.toString();
        }
        int len = str.length();

        if (fromIndexInclude < 0) {
            fromIndexInclude = len + fromIndexInclude;
            if (fromIndexInclude < 0) {
                fromIndexInclude = 0;
            }
        } else if (fromIndexInclude > len) {
            fromIndexInclude = len;
        }

        if (toIndexExclude < 0) {
            toIndexExclude = len + toIndexExclude;
            if (toIndexExclude < 0) {
                toIndexExclude = len;
            }
        } else if (toIndexExclude > len) {
            toIndexExclude = len;
        }

        if (toIndexExclude < fromIndexInclude) {
            int tmp = fromIndexInclude;
            fromIndexInclude = toIndexExclude;
            toIndexExclude = tmp;
        }

        if (fromIndexInclude == toIndexExclude) {
            return "";
        }

        return str.toString().substring(fromIndexInclude, toIndexExclude);
    }

    public static String removeSuffix(CharSequence str, CharSequence suffix) {
        if (isEmpty(str) || isEmpty(suffix)) {
            return str.toString();
        }

        final String str2 = str.toString();
        if (str2.endsWith(suffix.toString())) {
            return subPre(str2, str2.length() - suffix.length());// 截取前半段
        }
        return str2;
    }

    public static SimpleDateFormat getDefaultDateTimeFormat() {
        if (formats.get() == null) {
            formats.set(new Formats());
        }
        if ((formats.get()).DateTime == null) {
            (formats.get()).DateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        return (formats.get()).DateTime;
    }

    public static SimpleDateFormat getDefaultDateFormat() {
        if (formats.get() == null) {
            formats.set(new Formats());
        }
        if ((formats.get()).DateOnly == null) {
            (formats.get()).DateOnly = new SimpleDateFormat("yyyy-MM-dd");
        }
        return (formats.get()).DateOnly;
    }

    public static SimpleDateFormat getDefaultTimeFormat() {
        if (formats.get() == null) {
            formats.set(new Formats());
        }
        if ((formats.get()).TimeOnly == null) {
            (formats.get()).TimeOnly = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        }
        return (formats.get()).TimeOnly;
    }


    public static SimpleDateFormat getLastModifiedFormat() {
        if (formats.get() == null) {
            formats.set(new Formats());
        }
        if ((formats.get()).LastModified == null) {
            (formats.get()).LastModified = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        }
        return (formats.get()).LastModified;
    }

    public static String getCurrentDate() {
        return getDefaultDateFormat().format(new Date());
    }

    public static String getCurrentDate(String format) {
        return getFormat(format).format(new Date());
    }

    public static String getCurrentDateTime() {
        return getDefaultDateTimeFormat().format(new Date());
    }

    public static int getDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(7);
    }

    public static int getDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(5);
    }

    public static int getMaxDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.getActualMaximum(5);
    }

    public static int getDayOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(6);
    }

    public static String getCurrentDateTime(String format) {
        return getFormat(format).format(new Date());
    }

    public static String toString(Date date) {
        if (date == null) {
            return "";
        }
        return getDefaultDateFormat().format(date);
    }

    public static String toDateTimeString(Date date) {
        if (date == null) {
            return "";
        }
        return getDefaultDateTimeFormat().format(date);
    }

    public static String toString(Date date, String format) {
        if (date == null) {
            return "";
        }
        return getFormat(format).format(date);
    }

    public static int compare(String date1, String date2) {
        return compare(date1, date2, "yyyy-MM-dd");
    }

    public static int compare(String date1, String date2, String format) {
        Date d1 = parse(date1, format);
        Date d2 = parse(date2, format);
        return d1.compareTo(d2);
    }

    public static boolean isTime(String time) {
        String[] arr = time.split(":");
        if (arr.length < 2)
            return false;
        try {
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            int s = 0;
            if (arr.length == 3) {
                s = Integer.parseInt(arr[2]);
            }
            if ((h < 0) || (h > 23) || (m < 0) || (m > 59) || (s < 0) || (s > 59))
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDate(String date) {
        String[] arr = date.split("-");
        if (arr.length < 3)
            return false;
        try {
            int y = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            int d = Integer.parseInt(arr[2]);
            if ((y < 0) || (m > 12) || (m < 0) || (d < 0) || (d > 31))
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDateTime(String str) {
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        if (str.endsWith(".0")) {
            str = str.substring(0, str.length() - 2);
        }
        if (str.indexOf(" ") > 0) {
            String[] arr = str.split(" ");
            if (arr.length == 2) {
                return (isDate(arr[0])) && (isTime(arr[1]));
            }
            return false;
        }

        return isDate(str);
    }

    public static boolean isWeekend(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int t = cal.get(7);
        if ((t == 7) || (t == 1)) {
            return true;
        }
        return false;
    }

    public static Date parse(String str) {
        if (StringUtil.isEmpty(str))
            return null;
        try {
            return getDefaultDateFormat().parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean contains(CharSequence str, char searchChar) {
        return indexOf(str, searchChar) > -1;
    }

    /**
     * 指定范围内查找指定字符
     *
     * @param str        字符串
     * @param searchChar 被查找的字符
     * @return 位置
     */
    public static int indexOf(CharSequence str, char searchChar) {
        return indexOf(str, searchChar, 0);
    }

    /**
     * 指定范围内查找指定字符
     *
     * @param str        字符串
     * @param searchChar 被查找的字符
     * @param start      起始位置，如果小于0，从0开始查找
     * @return 位置
     */
    public static int indexOf(CharSequence str, char searchChar, int start) {
        if (str instanceof String) {
            return ((String) str).indexOf(searchChar, start);
        } else {
            return -1;
        }
    }

    public static int indexOf(CharSequence str, String searchChar, int start) {
        if (str instanceof String) {
            return ((String) str).indexOf(searchChar, start);
        } else {
            return -1;
        }
    }

    public static String subAfter(CharSequence string, char separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : "";
        }
        final String str = string.toString();
        final int pos = isLastSeparator ? str.lastIndexOf(separator) : str.indexOf(separator);
        if (-1 == pos) {
            return "";
        }
        return str.substring(pos + 1);
    }

    public static String subBefore(CharSequence string, char separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : "";
        }

        final String str = string.toString();
        final int pos = isLastSeparator ? str.lastIndexOf(separator) : str.indexOf(separator);
        if (-1 == pos) {
            return str;
        }
        if (0 == pos) {
            return "";
        }
        return str.substring(0, pos);
    }

    /**
     * 解析UTC时间，格式：<br>
     * <ol>
     * <li>yyyy-MM-dd'T'HH:mm:ss'Z'</li>
     * <li>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</li>
     * <li>yyyy-MM-dd'T'HH:mm:ssZ</li>
     * <li>yyyy-MM-dd'T'HH:mm:ss.SSSZ</li>
     * <li>yyyy-MM-dd'T'HH:mm:ss+0800</li>
     * <li>yyyy-MM-dd'T'HH:mm:ss+08:00</li>
     * </ol>
     *
     * @param utcString UTC时间
     * @return 日期对象
     * @since 4.1.14
     */
    public static Date parseUTC(String utcString) {
        if (utcString == null) {
            return null;
        }
        int length = utcString.length();
        char DOT = '.';
        if (contains(utcString, 'Z')) {
            if (length == Formats.UTC_PATTERN.length() - 4) {
                // 格式类似：2018-09-13T05:34:31Z，-4表示减去4个单引号的长度
                return parse(utcString, Formats.UTC_PATTERN);
            }

            final int patternLength = Formats.UTC_MS_PATTERN.length();
            // 格式类似：2018-09-13T05:34:31.999Z，-4表示减去4个单引号的长度
            // -4 ~ -6范围表示匹配毫秒1~3位的情况
            if (length <= patternLength - 4 && length >= patternLength - 6) {
                return parse(utcString, Formats.UTC_MS_PATTERN);
            }
        } else if (contains(utcString, '+')) {
            // 去除类似2019-06-01T19:45:43 +08:00加号前的空格
            utcString = utcString.replace(" +", "+");
            final String zoneOffset = subAfter(utcString, '+', true);
            if (StringUtil.isEmpty(zoneOffset)) {
                throw new TarnException("Invalid format: [{}]", utcString);
            }
            if (false == contains(zoneOffset, ':')) {
                // +0800转换为+08:00
                final String pre = subBefore(utcString, '+', true);
                utcString = pre + "+" + zoneOffset.substring(0, 2) + ":" + "00";
            }

            if (contains(utcString, DOT)) {
                // 带毫秒，格式类似：2018-09-13T05:34:31.999+08:00
                return parse(utcString, Formats.UTC_MS_WITH_XXX_OFFSET_PATTERN);
            } else {
                // 格式类似：2018-09-13T05:34:31+08:00
                return parse(utcString, Formats.UTC_WITH_XXX_OFFSET_PATTERN);
            }
        } else {
            if (length == Formats.UTC_SIMPLE_PATTERN.length() - 2) {
                // 格式类似：2018-09-13T05:34:31
                return parse(utcString, Formats.UTC_SIMPLE_PATTERN);
            } else if (contains(utcString, DOT)) {
                // 可能为：  2021-03-17T06:31:33.99
                return parse(utcString, Formats.UTC_SIMPLE_MS_PATTERN);
            }else  {
                // 可能为：  2021-03-17T06:31
                return parse(utcString, Formats.UTC_SIMPLE_MS_PATTERN_NOM);
            }
        }
        // 没有更多匹配的时间格式
        throw new TarnException("No format fit for date String [{}] !", utcString);
    }

    public static String str(CharSequence cs) {
        return null == cs ? null : cs.toString();
    }

    public static boolean containsAnyIgnoreCase(CharSequence str, CharSequence... testStrs) {
        return null != contains(str, testStrs);
    }

    public static String contains(CharSequence str, CharSequence... chars) {
        if (isEmpty(str) || chars == null) {
            return null;
        }
        for (CharSequence testStr : chars) {
            if (containsIgnoreCase(str, testStr)) {
                return testStr.toString();
            }
        }
        return null;
    }

    public static int indexOfIgnoreCase(final CharSequence str, String searchStr, int fromIndex) {
        return indexOf(str, searchStr, fromIndex);
    }

    public static boolean containsIgnoreCase(CharSequence str, CharSequence testStr) {
        if (null == str) {
            // 如果被监测字符串和
            return null == testStr;
        }
        return indexOfIgnoreCase(str.toString().toLowerCase(), testStr.toString().toLowerCase(), 0) > -1;
    }

    /**
     * 解析时间，格式HH:mm 或 HH:mm:ss，日期默认为今天
     *
     * @param timeString 标准形式的日期字符串
     * @return 日期对象
     * @since 3.1.1
     */
    public static Date parseTimeToday(CharSequence timeString) {
        timeString = TextKit.format("{} {}", getCurrentDate(), timeString);
        if (1 == StringUtil.count(timeString.toString(), ":")) {
            // 时间格式为 HH:mm
            return parse(timeString.toString(), Formats.NORM_DATETIME_MINUTE_PATTERN);
        } else {
            // 时间格式为 HH:mm:ss
            return parse(timeString.toString(), Formats.NORM_DATETIME_PATTERN);
        }
    }

    public static Date parseCST(CharSequence cstString) {
        if (cstString == null) {
            return null;
        }

        return parse(cstString.toString(), Formats.JDK_DATETIME_PATTERN);
    }

    public static String COLON = ":";
    public static char DOT = '.';

    public static Date parseFull(String dateStr) {
        if (StringUtil.isEmpty(dateStr)) {
            return null;
        }
        // 去掉两边空格并去掉中文日期中的“日”和“秒”，以规范长度
        dateStr = StringUtil.removeAll(dateStr.trim(), '日', '秒');
        int length = dateStr.length();

        if (NumberUtil.isNumber(dateStr)) {
            // 纯数字形式
            if (length == Formats.PURE_DATETIME_PATTERN.length()) {
                return parse(dateStr, Formats.PURE_DATETIME_PATTERN);
            } else if (length == Formats.PURE_DATETIME_MS_PATTERN.length()) {
                return parse(dateStr, Formats.PURE_DATETIME_MS_PATTERN);
            } else if (length == Formats.PURE_DATE_PATTERN.length()) {
                return parse(dateStr, Formats.PURE_DATE_PATTERN);
            } else if (length == Formats.PURE_TIME_PATTERN.length()) {
                return parse(dateStr, Formats.PURE_TIME_PATTERN);
            }
        } else if (StringUtil.isMatch(PatternPool.TIME, dateStr)) {
            // HH:mm:ss 或者 HH:mm 时间格式匹配单独解析
            return parseTimeToday(dateStr);
        } else if (containsAnyIgnoreCase(dateStr, wtb)) {
            // JDK的Date对象toString默认格式，类似于：
            // Tue Jun 4 16:25:15 +0800 2019
            // Thu May 16 17:57:18 GMT+08:00 2019
            // Wed Aug 01 00:00:00 CST 2012
            return parseCST(dateStr);
        } else if (contains(dateStr, 'T')) {
            // UTC时间
            return parseUTC(dateStr);
        }

        //标准日期格式（包括单个数字的日期时间）
        dateStr = normalize(dateStr, false);

        if (StringUtil.isMatch(PatternPool.REGEX_NORM, dateStr)) {
            return parseNormal(dateStr);
        } else {
            dateStr = normalize(dateStr, true);
            return parseNormal(dateStr);
        }

    }

    public static Date parseNormal(String dateStr) {
        final int colonCount = StringUtil.count(dateStr, COLON);
        switch (colonCount) {
            case 0:
                // yyyy-MM-dd
                return parse(dateStr, Formats.NORM_DATE_PATTERN);
            case 1:
                // yyyy-MM-dd HH:mm
                return parse(dateStr, Formats.NORM_DATETIME_MINUTE_PATTERN);
            case 2:
                final int indexOfDot = indexOf(dateStr, DOT);
                if (indexOfDot > 0) {
                    final int length1 = dateStr.length();
                    // yyyy-MM-dd HH:mm:ss.SSS 或者 yyyy-MM-dd HH:mm:ss.SSSSSS
                    if (length1 - indexOfDot > 4) {
                        // 类似yyyy-MM-dd HH:mm:ss.SSSSSS，采取截断操作
                        dateStr = subPre(dateStr, indexOfDot + 4);
                    }
                    return parse(dateStr, Formats.NORM_DATETIME_MS_PATTERN);
                }
                // yyyy-MM-dd HH:mm:ss
                return parse(dateStr, Formats.NORM_DATETIME_PATTERN);
        }
        throw new TarnException("No format fit for date String [{}] !", dateStr);
    }

    private final static String[] wtb = { //
            "sun", "mon", "tue", "wed", "thu", "fri", "sat", // 星期
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", // 月份
            "gmt", "ut", "utc", "est", "edt", "cst", "cdt", "mst", "mdt", "pst", "pdt"// 时间标准
    };

    public static Date parse(String str, String format) {
        if (StringUtil.isEmpty(str))
            return null;
        try {
            SimpleDateFormat t = getFormat(format);
//            SimpleDateFormat t = new SimpleDateFormat(format);
            return t.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseLastModified(String str) {
        if (StringUtil.isEmpty(str))
            return null;
        try {
            return getLastModifiedFormat().parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseDateTime(String str) {
        if (StringUtil.isEmpty(str)) {
            return null;
        }
        if (str.length() <= 10)
            return parse(str);
        try {
            return getDefaultDateTimeFormat().parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseDateTime(String str, String format) {
        if (StringUtil.isEmpty(str))
            return null;
        try {
            SimpleDateFormat t = new SimpleDateFormat(format);
            return t.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date addSecond(Date date, int count) {
        return new Date(date.getTime() + 1000L * count);
    }

    public static Date addMinute(Date date, int count) {
        return new Date(date.getTime() + 60000L * count);
    }

    public static Date addHour(Date date, int count) {
        return new Date(date.getTime() + 3600000L * count);
    }


    public static Date cutDay(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, count);
        return c.getTime();
    }

    public static Date addDay(Date date, int count) {
        return new Date(date.getTime() + 86400000L * count);
    }

    public static Date addWeek(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(3, count);
        return c.getTime();
    }


    public static Date addMonth(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(2, count);
        return c.getTime();
    }

    public static Date addYear(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(1, count);
        return c.getTime();
    }

    public static String convertChineseNumber(String strDate) {
        strDate = StringUtil.replaceEx(strDate, "一十一", "11");
        strDate = StringUtil.replaceEx(strDate, "一十二", "12");
        strDate = StringUtil.replaceEx(strDate, "一十三", "13");
        strDate = StringUtil.replaceEx(strDate, "一十四", "14");
        strDate = StringUtil.replaceEx(strDate, "一十五", "15");
        strDate = StringUtil.replaceEx(strDate, "一十六", "16");
        strDate = StringUtil.replaceEx(strDate, "一十七", "17");
        strDate = StringUtil.replaceEx(strDate, "一十八", "18");
        strDate = StringUtil.replaceEx(strDate, "一十九", "19");
        strDate = StringUtil.replaceEx(strDate, "二十一", "21");
        strDate = StringUtil.replaceEx(strDate, "二十二", "22");
        strDate = StringUtil.replaceEx(strDate, "二十三", "23");
        strDate = StringUtil.replaceEx(strDate, "二十四", "24");
        strDate = StringUtil.replaceEx(strDate, "二十五", "25");
        strDate = StringUtil.replaceEx(strDate, "二十六", "26");
        strDate = StringUtil.replaceEx(strDate, "二十七", "27");
        strDate = StringUtil.replaceEx(strDate, "二十八", "28");
        strDate = StringUtil.replaceEx(strDate, "二十九", "29");
        strDate = StringUtil.replaceEx(strDate, "十一", "11");
        strDate = StringUtil.replaceEx(strDate, "十二", "12");
        strDate = StringUtil.replaceEx(strDate, "十三", "13");
        strDate = StringUtil.replaceEx(strDate, "十四", "14");
        strDate = StringUtil.replaceEx(strDate, "十五", "15");
        strDate = StringUtil.replaceEx(strDate, "十六", "16");
        strDate = StringUtil.replaceEx(strDate, "十七", "17");
        strDate = StringUtil.replaceEx(strDate, "十八", "18");
        strDate = StringUtil.replaceEx(strDate, "十九", "19");
        strDate = StringUtil.replaceEx(strDate, "十", "10");
        strDate = StringUtil.replaceEx(strDate, "二十", "20");
        strDate = StringUtil.replaceEx(strDate, "三十", "20");
        strDate = StringUtil.replaceEx(strDate, "三十一", "31");
        strDate = StringUtil.replaceEx(strDate, "零", "0");
        strDate = StringUtil.replaceEx(strDate, "○", "0");
        strDate = StringUtil.replaceEx(strDate, "一", "1");
        strDate = StringUtil.replaceEx(strDate, "二", "2");
        strDate = StringUtil.replaceEx(strDate, "三", "3");
        strDate = StringUtil.replaceEx(strDate, "四", "4");
        strDate = StringUtil.replaceEx(strDate, "五", "5");
        strDate = StringUtil.replaceEx(strDate, "六", "6");
        strDate = StringUtil.replaceEx(strDate, "七", "7");
        strDate = StringUtil.replaceEx(strDate, "八", "8");
        strDate = StringUtil.replaceEx(strDate, "九", "9");
        return strDate;
    }

    private static class Formats {
        private SimpleDateFormat DateOnly;
        private SimpleDateFormat TimeOnly;
        private SimpleDateFormat DateTime;
        private SimpleDateFormat LastModified;
        private HashMap<String, SimpleDateFormat> Others;


        /**
         * 标准日期时间正则，每个字段支持单个数字或2个数字，包括：
         * <pre>
         *     yyyy-MM-dd HH:mm:ss.SSSSSS
         *     yyyy-MM-dd HH:mm:ss.SSS
         *     yyyy-MM-dd HH:mm:ss
         *     yyyy-MM-dd HH:mm
         *     yyyy-MM-dd
         * </pre>
         *
         * @since 5.3.6
         */
        public static final Pattern REGEX_NORM = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}(\\s\\d{1,2}:\\d{1,2}(:\\d{1,2})?)?(.\\d{1,6})?");

        //-------------------------------------------------------------------------------------------------------------------------------- Normal
        /**
         * 年格式：yyyy
         */
        public static final String NORM_YEAR_PATTERN = "yyyy";
        /**
         * 年月格式：yyyy-MM
         */
        public static final String NORM_MONTH_PATTERN = "yyyy-MM";


        /**
         * 简单年月格式：yyyyMM
         */
        public static final String SIMPLE_MONTH_PATTERN = "yyyyMM";

        /**
         * 标准日期格式：yyyy-MM-dd
         */
        public static final String NORM_DATE_PATTERN = "yyyy-MM-dd";

        /**
         * 标准时间格式：HH:mm:ss
         */
        public static final String NORM_TIME_PATTERN = "HH:mm:ss";

        /**
         * 标准日期时间格式，精确到分：yyyy-MM-dd HH:mm
         */
        public static final String NORM_DATETIME_MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
        /**
         * 标准日期时间格式，精确到秒：yyyy-MM-dd HH:mm:ss
         */
        public static final String NORM_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

        /**
         * 标准日期时间格式，精确到毫秒：yyyy-MM-dd HH:mm:ss.SSS
         */
        public static final String NORM_DATETIME_MS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

        /**
         * ISO8601日期时间格式，精确到毫秒：yyyy-MM-dd HH:mm:ss,SSS
         */
        public static final String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

        /**
         * 标准日期格式：yyyy年MM月dd日
         */
        public static final String CHINESE_DATE_PATTERN = "yyyy年MM月dd日";

        /**
         * 标准日期格式：yyyy年MM月dd日 HH时mm分ss秒
         */
        public static final String CHINESE_DATE_TIME_PATTERN = "yyyy年MM月dd日HH时mm分ss秒";

        //-------------------------------------------------------------------------------------------------------------------------------- Pure
        /**
         * 标准日期格式：yyyyMMdd
         */
        public static final String PURE_DATE_PATTERN = "yyyyMMdd";
        /**
         * 标准日期格式：HHmmss
         */
        public static final String PURE_TIME_PATTERN = "HHmmss";
        /**
         * 标准日期格式：yyyyMMddHHmmss
         */
        public static final String PURE_DATETIME_PATTERN = "yyyyMMddHHmmss";

        /**
         * 标准日期格式：yyyyMMddHHmmssSSS
         */
        public static final String PURE_DATETIME_MS_PATTERN = "yyyyMMddHHmmssSSS";

        //-------------------------------------------------------------------------------------------------------------------------------- Others
        /**
         * HTTP头中日期时间格式：EEE, dd MMM yyyy HH:mm:ss z
         */
        public static final String HTTP_DATETIME_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

        /**
         * JDK中日期时间格式：EEE MMM dd HH:mm:ss zzz yyyy
         */
        public static final String JDK_DATETIME_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";
        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ss
         */
        public static final String UTC_SIMPLE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ss.SSS
         */
        public static final String UTC_SIMPLE_MS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        public static final String UTC_SIMPLE_MS_PATTERN_NOM = "yyyy-MM-dd'T'HH:mm";
        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ss'Z'
         */
        public static final String UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ssZ
         */
        public static final String UTC_WITH_ZONE_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ssXXX
         */
        public static final String UTC_WITH_XXX_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
         */
        public static final String UTC_MS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ssZ
         */
        public static final String UTC_MS_WITH_ZONE_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

        /**
         * UTC时间：yyyy-MM-dd'T'HH:mm:ss.SSSXXX
         */
        public static final String UTC_MS_WITH_XXX_OFFSET_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";


    }
}
