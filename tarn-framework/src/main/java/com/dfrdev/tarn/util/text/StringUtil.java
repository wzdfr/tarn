package com.dfrdev.tarn.util.text;

import com.dfrdev.tarn.collection.CharUtil;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.commons.ArrayUtils;
import com.dfrdev.tarn.util.commons.StringEscapeUtils;
import com.dfrdev.tarn.util.xml.XMLParser;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dfrdev.tarn.util.commons.ArrayUtils.INDEX_NOT_FOUND;
import com.dfrdev.tarn.util.LogUtil;

import com.dfrdev.tarn.util.classloader.ObjectUtil;
import com.dfrdev.tarn.util.io.FileUtil;
/**
 * Created by dfr on 14-9-27.
 */
public class StringUtil {
    public static final byte[] BOM = {-17, -69, -65};

    public static final char[] HexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static final Pattern PTitle = Pattern.compile("<title>(.+?)</title>", 34);
    public static final String EMPTY = "";
    public static final String SLASH = "/";
    public static Pattern PHtmlTag = Pattern.compile("<[^<>]*>", 32);

    public static String PComment = "<\\!\\-\\-.*?\\-\\->";

    public static String PScript = "<script[^>]*?>[\\s\\S]*?<\\/script>";

    public static String PStyle = "<style[^>]*?>[\\s\\S]*?<\\/style>";

    public static String PHtml = "<[^>]+>";

    public static final Pattern PLetterOrDigit = Pattern.compile("^\\w*$", 34);

    public static final Pattern PLetter = Pattern.compile("^[A-Za-z]*$", 34);

    public static final Pattern PDigit = Pattern.compile("^\\d*$", 34);

    private static Pattern PChinese = Pattern.compile("[^一-龥]+", 34);

    private static Pattern PID = Pattern.compile("[\\w\\s\\_\\.\\,]*", 34);
    private static int[] filter = new int[128];
    private static int[] filterEnd = new int[128];

    static {
        filter['<'] = Integer.MAX_VALUE / 2;
        filterEnd['<'] = '>';

        filter['&'] = 10;
        filterEnd['&'] = ';';

        filter[';'] = -1;
        filter['\n'] = -1;

        filter['\r'] = -1;
        filter['\t'] = -1;
        filter[' '] = 1;
        filter['*'] = 1;
        filter['-'] = 1;
        filter['.'] = 1;
        filter['#'] = 1;

    }

    public static String md5Hex(String src) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] md = md5.digest(src.getBytes());
            return hexEncode(md);
        } catch (Exception e) {
        }
        return null;
    }

    public static String sha1Hex(String src) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("SHA1");
            byte[] md = md5.digest(src.getBytes());
            return hexEncode(md);
        } catch (Exception e) {
        }
        return null;
    }

    public static String md5Base64(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return base64Encode(md5.digest(str.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static String md5Base64FromHex(String md5str) {
        char[] cs = md5str.toCharArray();
        byte[] bs = new byte[16];
        for (int i = 0; i < bs.length; i++) {
            char c1 = cs[(i * 2)];
            char c2 = cs[(i * 2 + 1)];
            byte m1 = 0;
            byte m2 = 0;
            for (byte k = 0; k < 16; k = (byte) (k + 1)) {
                if (HexDigits[k] == c1) {
                    m1 = k;
                }
                if (HexDigits[k] == c2) {
                    m2 = k;
                }
            }
            bs[i] = (byte) (m1 << 4 | 0 + m2);
        }

        String newstr = base64Encode(bs);
        return newstr;
    }

    public static String md5HexFromBase64(String base64str) {
        return hexEncode(base64Decode(base64str));
    }

    public static String hexEncode(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        int i = 0;
        for (int j = 0; i < l; i++) {
            out[(j++)] = HexDigits[((0xF0 & data[i]) >>> 4)];
            out[(j++)] = HexDigits[(0xF & data[i])];
        }
        return new String(out);
    }

    public static byte[] hexDecode(String str) {
        char[] data = str.toCharArray();
        int len = data.length;
        if ((len & 0x1) != 0) {
            throw new TarnException("StringUtil.hexEncode:Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];
        int i = 0;
        for (int j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f |= toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    private static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new TarnException("StringUtil.hexDecode:Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    public static String byteToBin(byte[] bs) {
        char[] cs = new char[bs.length * 9];
        for (int i = 0; i < bs.length; i++) {
            byte b = bs[i];
            int j = i * 9;
            cs[j] = ((b >>> 7 & 0x1) == 1 ? 49 : '0');
            cs[(j + 1)] = ((b >>> 6 & 0x1) == 1 ? 49 : '0');
            cs[(j + 2)] = ((b >>> 5 & 0x1) == 1 ? 49 : '0');
            cs[(j + 3)] = ((b >>> 4 & 0x1) == 1 ? 49 : '0');
            cs[(j + 4)] = ((b >>> 3 & 0x1) == 1 ? 49 : '0');
            cs[(j + 5)] = ((b >>> 2 & 0x1) == 1 ? 49 : '0');
            cs[(j + 6)] = ((b >>> 1 & 0x1) == 1 ? 49 : '0');
            cs[(j + 7)] = ((b & 0x1) == 1 ? 49 : '0');
            cs[(j + 8)] = ',';
        }
        return new String(cs);
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        byte[] arrayOfByte = b;
        int j = b.length;
        for (int i = 0; i < j; i++) {
            byte element = arrayOfByte[i];
            resultSb.append(byteToHexString(element));
            resultSb.append(" ");
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return String.valueOf(HexDigits[d1] + HexDigits[d2]);
    }

    public static boolean isUTF8(byte[] bs) {
        if ((bs.length > 3) && (bs[0] == 239) && (bs[1] == 187) && (bs[2] == 191)) {
            return true;
        }
        int encodingBytesCount = 0;
        byte[] arrayOfByte = bs;
        int j = bs.length;
        for (int i = 0; i < j; i++) {
            byte element = arrayOfByte[i];
            byte c = element;
            if (encodingBytesCount == 0) {
                if ((c & 0x80) != 0) {
                    if ((c & 0xC0) == 192) {
                        encodingBytesCount = 1;
                        c = (byte) (c << 2);

                        while ((c & 0x80) == 128) {
                            c = (byte) (c << 1);
                            encodingBytesCount++;
                        }
                    } else {
                        return false;
                    }
                }
            } else if ((c & 0xC0) == 128)
                encodingBytesCount--;
            else {
                return false;
            }
        }

        if (encodingBytesCount != 0) {
            return false;
        }
        return true;
    }

    public static String base64Encode(byte[] b) {
        if (b == null) {
            return null;
        }
        return new String(Base64.encodeBase64(b));
    }

    public static byte[] base64Decode(String s) {
        if (s != null) {
            try {
                return Base64.decodeBase64(s.getBytes());
            } catch (Exception e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public static String javaEncode(String txt) {
        FastStringBuilder sb = new FastStringBuilder();
        javaEncode(txt, sb);
        return sb.toStringAndClose();
    }

    public static void javaEncode(String txt, FastStringBuilder sb) {
        if ((txt == null) || (txt.length() == 0)) {
            return;
        }
        int last = 0;
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if ((c == '"') || (c == '\\') || (c == '\'')) {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append(c);
            } else if (c == '\n') {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append('n');
            } else if (c == '\r') {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append('r');
            } else if (c == '\t') {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append('t');
            } else if (c == '\f') {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append('f');
            } else if (c == '\b') {
                if (last != i) {
                    sb.append(txt, last, i - last);
                }
                last = i + 1;
                sb.append('\\').append('b');
            }
        }
        if (last == 0)
            sb.append(txt);
        else
            sb.append(txt, last, txt.length() - last);
    }

    public static String javaDecode(String txt) {
        if ((txt == null) || (txt.length() == 0)) {
            return txt;
        }
        FastStringBuilder sb = null;
        int lastIndex = 0;
        while (true) {
            int index = txt.indexOf('\\', lastIndex);
            if (index < 0) {
                break;
            }
            if (sb == null) {
                sb = new FastStringBuilder();
            }
            sb.append(txt.substring(lastIndex, index));
            if (index < txt.length() - 1) {
                char c = txt.charAt(index + 1);
                if (c == 'n')
                    sb.append('\n');
                else if (c == 'r')
                    sb.append('\r');
                else if (c == 't')
                    sb.append('\t');
                else if (c == 'f')
                    sb.append('\f');
                else if (c == '\'')
                    sb.append('\'');
                else if (c == '"')
                    sb.append('"');
                else if (c == '\\') {
                    sb.append('\\');
                }
                lastIndex = index + 2;
            } else {
                sb.append(txt.substring(index, index + 1));

                lastIndex = index + 1;
            }
        }
        if (lastIndex == 0) {
            return txt;
        }
        sb.append(txt.substring(lastIndex));

        return sb.toStringAndClose();
    }

    public static String[] splitEx(String str, String spliter) {
        char escapeChar = '\\';
        if (spliter.equals("\\")) {
            escapeChar = '&';
        }
        return splitEx(str, spliter, escapeChar);
    }

    public static String[] splitEx(String str, String spliter, char escapeChar) {
        if (str == null) {
            return null;
        }
        ArrayList list = new ArrayList();
        if ((spliter == null) || (spliter.equals("")) || (str.length() < spliter.length())) {
            return new String[]{str};
        }
        int length = spliter.length();
        int lastIndex = 0;
        int lastStart = 0;
        while (true) {
            int i = str.indexOf(spliter, lastIndex);
            if (i < 0) break;
            if ((i > 0) && (str.charAt(i - 1) == escapeChar)) {
                lastIndex = i + 1;
            } else {
                list.add(str.substring(lastStart, i));
                lastStart = lastIndex = i + length;
            }
        }
        if (lastStart <= str.length()) {
            list.add(str.substring(lastStart));
        }

        String[] arr = new String[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ((String) list.get(i));
        }
        return arr;
    }

    public static String replaceEx(String str, String subStr, String reStr) {
        if ((str == null) || (str.length() == 0) || (reStr == null)) {
            return str;
        }
        if ((subStr == null) || (subStr.length() == 0) || (subStr.length() > str.length())) {
            return str;
        }
        StringBuilder sb = null;
        int lastIndex = 0;
        while (true) {
            int index = str.indexOf(subStr, lastIndex);
            if (index < 0) {
                break;
            }
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append(str.substring(lastIndex, index));
            sb.append(reStr);

            lastIndex = index + subStr.length();
        }
        if (lastIndex == 0) {
            return str;
        }
        sb.append(str.substring(lastIndex));
        return sb.toString();
    }

    public static String replaceAllIgnoreCase(String source, String oldstring, String newstring) {
        Pattern p = Pattern.compile(oldstring, 34);
        Matcher m = p.matcher(source);
        return m.replaceAll(newstring);
    }

    public static String urlEncode(String str) {
        return urlEncode(str, Config.getGlobalCharset());
    }

    public static String urlDecode(String str) {
        return urlDecode(str, Config.getGlobalCharset());
    }

    public static String urlEncode(String str, String charset) {
        try {
            return URLEncoder.encode(str, charset);
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static String urlDecode(String str, String charset) {
        try {
            return URLDecoder.decode(str, charset);
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static String htmlEncode(String txt) {
        return StringEscapeUtils.escapeHtml4(txt);
    }

    public static String htmlDecode(String txt) {
        txt = replaceEx(txt, "&#8226;", "·");
        return StringEscapeUtils.unescapeHtml4(txt);
    }

    public static String escape(String src) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char j = src.charAt(i);
            if ((Character.isDigit(j)) || (Character.isLowerCase(j)) || (Character.isUpperCase(j))) {
                sb.append(j);
            } else if (j < 'Ā') {
                sb.append("%");
                if (j < '\020') {
                    sb.append("0");
                }
                sb.append(Integer.toString(j, 16));
            } else {
                sb.append("%u");
                sb.append(Integer.toString(j, 16));
            }
        }
        return sb.toString();
    }

    public static String unescape(String src) {
        StringBuilder sb = new StringBuilder();
        int lastPos = 0;
        int pos = 0;

        while (lastPos < src.length()) {
            pos = src.indexOf("%", lastPos);
            if (pos == lastPos) {
                if (src.charAt(pos + 1) == 'u') {
                    char ch = (char) Integer.parseInt(src.substring(pos + 2, pos + 6), 16);
                    sb.append(ch);
                    lastPos = pos + 6;
                } else {
                    char ch = (char) Integer.parseInt(src.substring(pos + 1, pos + 3), 16);
                    sb.append(ch);
                    lastPos = pos + 3;
                }
            } else if (pos == -1) {
                sb.append(src.substring(lastPos));
                lastPos = src.length();
            } else {
                sb.append(src.substring(lastPos, pos));
                lastPos = pos;
            }
        }

        return sb.toString();
    }

    public static String leftPad(String srcString, char c, int length) {
        if (srcString == null) {
            srcString = "";
        }
        int tLen = srcString.length();

        if (tLen >= length) {
            return srcString;
        }
        int iMax = length - tLen;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iMax; i++) {
            sb.append(c);
        }
        sb.append(srcString);
        return sb.toString();
    }

    public static String subString(String src, int length) {
        if (src == null) {
            return null;
        }
        int i = src.length();
        if (i > length) {
            return src.substring(0, length);
        }
        return src;
    }

    public static String subStringEx(String src, int length) {
        if (src == null) {
            return null;
        }
        int m = 0;
        boolean unixFlag = false;
        String osname = System.getProperty("os.name").toLowerCase();
        if ((osname.indexOf("linux") != -1) || (osname.indexOf("sunos") != -1) || (osname.indexOf("solaris") != -1) ||
                (osname.indexOf("aix") != -1) || (
                (osname.indexOf("windows") >= 0) && (osname.indexOf("7") >= 0))) {
            unixFlag = true;
        }
        try {
            byte[] b = src.getBytes("Unicode");
            for (int i = 2; i < b.length; i += 2) {
                byte flag = b[(i + 1)];
                if (unixFlag) {
                    flag = b[i];
                }
                if (flag == 0)
                    m++;
                else {
                    m += 2;
                }
                if (m > length)
                    return src.substring(0, (i - 2) / 2);
            }
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
            throw new RuntimeException("String.getBytes(\"Unicode\") failed");
        }
        return src;
    }

    public static int lengthEx(String src) {
        return lengthEx(src, false);
    }

    public static int lengthEx(String src, boolean UTF8Flag) {
        if (ObjectUtil.empty(src)) {
            return 0;
        }
        int length = 0;
        boolean bigFlag = true;
        try {
            byte[] b = src.getBytes("Unicode");
            if (b[0] == -2) {
                bigFlag = false;
            }
            for (int i = 2; i < b.length; i += 2) {
                byte flag = b[(i + 1)];
                if (!bigFlag) {
                    flag = b[i];
                }
                if (flag == 0) {
                    length++;
                } else if (UTF8Flag)
                    length += 3;
                else
                    length += 2;
            }
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
            throw new RuntimeException("String.getBytes(\"Unicode\") failed");
        }
        return length;
    }

    public static String rightPad(String srcString, char c, int length) {
        if (srcString == null) {
            srcString = "";
        }
        int tLen = srcString.length();

        if (tLen >= length) {
            return srcString;
        }
        int iMax = length - tLen;
        StringBuilder sb = new StringBuilder();
        sb.append(srcString);
        for (int i = 0; i < iMax; i++) {
            sb.append(c);
        }
        return sb.toString();
    }


    public static String rightTrim(String src) {
        if (ObjectUtil.empty(src)) {
            return src;
        }
        for (int i = src.length() - 1; i >= 0; i--) {
            if (!XMLParser.isSpace(src.charAt(i))) {
                return src.substring(0, i + 1);
            }
        }
        return src;
    }

    public static String leftTrim(String src) {
        if (ObjectUtil.empty(src)) {
            return src;
        }
        for (int i = 0; i < src.length(); i++) {
            if (!XMLParser.isSpace(src.charAt(i))) {
                return src.substring(i);
            }
        }
        return src;
    }

    public static void printStringWithAnyCharset(String str) {
        Map map = Charset.availableCharsets();
        Iterator localIterator2;
        for (Iterator localIterator1 = map.keySet().iterator(); localIterator1.hasNext();
             localIterator2.hasNext()) {
            String key1 = (String) localIterator1.next();
            LogUtil.info(key1);
            localIterator2 = map.keySet().iterator();
            String key2 = (String) localIterator2.next();
            LogUtil.debug("\t");
            try {
                LogUtil.debug("From " + key1 + " To " + key2 + ":" + new String(str.getBytes(key1.toString()), key2.toString()));
            } catch (Exception e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            }
        }
    }

    public static String toSBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '　';
            } else if (((c[i] <= '@') || (c[i] >= '[')) && ((c[i] <= '`') || (c[i] >= '{'))) {
                if (c[i] < '')
                    c[i] = (char) (c[i] + 65248);
            }
        }
        return new String(c);
    }

    public static String toNSBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '　';
            } else if (c[i] < '') {
                c[i] = (char) (c[i] + 65248);
            }
        }
        return new String(c);
    }

    public static String toDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '　') {
                c[i] = ' ';
            } else if ((c[i] > 65280) && (c[i] < 65375)) {
                c[i] = (char) (c[i] - 65248);
            }
        }
        return new String(c);
    }

    public static String getChineseFullSpelling(String cnStr) {
        if ((cnStr == null) || ("".equals(cnStr.trim()))) {
            return cnStr;
        }
        return ChineseSpelling.getSpelling(cnStr);
    }

    public static String getChinesePersonNameSpelling(String cnStr) {
        if ((cnStr == null) || ("".equals(cnStr.trim()))) {
            return cnStr;
        }
        return ChineseSpelling.getPersonNameSpelling(cnStr);
    }

    public static String getChineseCapitalizedSpelling(String cnStr) {
        if ((cnStr == null) || ("".equals(cnStr.trim()))) {
            return cnStr;
        }
        return ChineseSpelling.getCapitalizedSpelling(cnStr);
    }

    public static String getHtmlTitle(File f) {
        String html = FileUtil.readText(f);
        String title = getHtmlTitle(html);
        return title;
    }

    public static String getHtmlTitle(String html) {
        Matcher m = PTitle.matcher(html);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static String clearHtmlTag(String htmlStr) {
        return clearHtmlTag(htmlStr, true);
    }

    public static String clearHtmlTag(String htmlStr, boolean cleanSpace) {
        Pattern p_script = Pattern.compile(PScript, 2);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll("");

        Pattern p_style = Pattern.compile(PStyle, 2);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll("");

        Pattern p_html = Pattern.compile(PHtml, 2);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll("");

        Pattern p_comment = Pattern.compile(PComment, 2);
        Matcher m_comment = p_comment.matcher(htmlStr);
        htmlStr = m_comment.replaceAll("");

        htmlStr = htmlStr.trim();
        htmlStr = htmlDecode(htmlStr);

        if (cleanSpace) {
            htmlStr = htmlStr.replaceAll("[\\s　]{2,}", " ");
        }
        return htmlStr;
    }


    /**
     * 去除html标签
     *
     * @param input
     * @return
     */
    public static String rmHtmlTag(String input) {
        if (isEmpty(input)) {
            return "";
        }
        int length = input.length();
        int tl = 0;
        StringBuilder sb = new StringBuilder();
        char c = 0;
        for (int i = 0; i < length; i++) {
            c = input.charAt(i);

            if (c > 127) {
                sb.append(c);
                continue;
            }

            switch (filter[c]) {
                case -1:
                    break;
                case 0:
                    sb.append(c);
                    break;
                case 1:
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != c)
                        sb.append(c);
                    do {
                        i++;
                    } while (i < length && input.charAt(i) == c);

                    if (i < length || input.charAt(length - 1) != c)
                        i--;
                    break;
                default:
                    tl = filter[c] + i;
                    int tempOff = i;
                    boolean flag = false;
                    char end = (char) filterEnd[c];
                    for (i++; i < length && i < tl; i++) {
                        c = input.charAt(i);
                        if (c > 127)
                            continue;
                        if (c == end) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        i = tempOff;
                        sb.append(input.charAt(i));
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 将一个字符串.转换成排序后的字符数组
     *
     * @param str
     * @return
     */
    public static char[] sortCharArray(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return chars;
    }

    public static String capitalize(String str) {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }
        return Character.toTitleCase(str.charAt(0)) + str.substring(1);
    }

    public static boolean isEmpty(String str) {
        return (str == null) || (str.length() == 0);
    }

    public static boolean isBlank(CharSequence str) {
        int length;

        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }

        for (int i = 0; i < length; i++) {
            // 只要有一个非空字符即为非空字符串
            if (false ==isBlankChar(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }
    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c)
                || Character.isSpaceChar(c)
                || c == '\ufeff'
                || c == '\u202a';
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isNull(String str) {
        return (isEmpty(str)) || ("null".equals(str));
    }

    public static boolean isNotNull(String str) {
        return (isNotEmpty(str)) && (!"null".equals(str));
    }

    public static final String noNull(String string, String defaultString) {
        return isEmpty(string) ? defaultString : string;
    }

    public static final String noNull(String string) {
        return noNull(string, "");
    }

    public static String join(Object arr) {
        return join(arr, ",");
    }

    public static String join(Object arr, String spliter) {
        if (arr == null) {
            return null;
        }
        if (!arr.getClass().isArray()) {
            return arr.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Array.getLength(arr); i++) {
            if (i != 0) {
                sb.append(spliter);
            }
            sb.append(Array.get(arr, i));
        }
        return sb.toString();
    }

    public static String join(Object[] arr) {
        return join(arr, ",");
    }

    public static String join(Object[][] arr) {
        return join(arr, "\n", ",");
    }

    public static String join(Object[] arr, String spliter) {
        if (arr == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append(spliter);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    public static String join(Object[][] arr, String spliter1, String spliter2) {
        if (arr == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append(spliter2);
            }
            sb.append(join(arr[i], spliter2));
        }
        return sb.toString();
    }

    public static String join(Collection<?> list) {
        return join(list, ",");
    }

    public static String join(Collection<?> c, String spliter) {
        if (c == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Iterator localIterator = c.iterator(); localIterator.hasNext(); ) {
            Object name = localIterator.next();
            if (first)
                first = false;
            else {
                sb.append(spliter);
            }
            sb.append(name);
        }
        return sb.toString();
    }

    public static int count(String str, String findStr) {
        int lastIndex = 0;
        int length = findStr.length();
        int count = 0;
        int start = 0;
        while ((start = str.indexOf(findStr, lastIndex)) >= 0) {
            lastIndex = start + length;
            count++;
        }
        return count;
    }



    public static boolean isLetterOrDigit(String str) {
        return PLetterOrDigit.matcher(str).find();
    }

    public static boolean isLetter(String str) {
        return PLetter.matcher(str).find();
    }

    public static boolean isDigit(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return PDigit.matcher(str).find();
    }

    public static boolean containsChinese(String str) {
        if (!PChinese.matcher(str).matches()) {
            return true;
        }
        return false;
    }
    /**
     * 给定内容是否匹配正则
     *
     * @param pattern 模式
     * @param content 内容
     * @return 正则为null或者""则不检查，返回true，内容为null返回false
     */
    public static boolean isMatch(Pattern pattern, CharSequence content) {
        if (content == null || pattern == null) {
            // 提供null的字符串为不匹配
            return false;
        }
        return pattern.matcher(content).matches();
    }

    public static String str(CharSequence cs) {
        return null == cs ? null : cs.toString();
    }
    public static String removeAll(CharSequence str, char... chars) {
        if (null == str ||chars==null) {
            return str(str);
        }
        final int len = str.length();
        if (0 == len) {
            return str(str);
        }
        final StringBuilder builder = new StringBuilder(len);
        char c;
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (false == StringUtil.contains(chars, c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static boolean checkID(String str) {
        if (isEmpty(str)) {
            return true;
        }
        if (PID.matcher(str).matches()) {
            return true;
        }
        return false;
    }
    public static boolean contains(char[] array, char value) {
        return indexOf(array, value) > -1;
    }
    public static boolean contains(CharSequence str, CharSequence searchStr) {
        if (null == str || null == searchStr) {
            return false;
        }
        return str.toString().contains(searchStr);
    }
    public static int indexOf(char[] array, char value) {
        if (null != array) {
            for (int i = 0; i < array.length; i++) {
                if (value == array[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static boolean equals(CharSequence str1, CharSequence str2, boolean ignoreCase) {
        if (null == str1) {
            // 只有两个都为null才判断相等
            return str2 == null;
        }
        if (null == str2) {
            // 字符串2空，字符串1非空，直接false
            return false;
        }

        if (ignoreCase) {
            return str1.toString().equalsIgnoreCase(str2.toString());
        } else {
            return str1.toString().contentEquals(str2);
        }
    }

    public static Map<String, String> splitToMapx(String str, String entrySpliter, String keySpliter) {
        return splitToMapx(str, entrySpliter, keySpliter, '\\');
    }

    public static Map<String, String> splitToMapx(String str, String entrySpliter, String keySpliter, char escapeChar) {
        Map map = new HashMap();
        String[] list = splitEx(str, entrySpliter, escapeChar);
        for (String element : list) {
            String[] list2 = splitEx(element, keySpliter, escapeChar);
            String key = list2[0];
            if (!isEmpty(key)) {
                key = key.trim();
                String value = null;
                if (list2.length > 1) {
                    value = list2[1];
                }
                map.put(key, value);
            }
        }
        return map;
    }

    public static String getURLExtName(String url) {
        if (isEmpty(url)) {
            return null;
        }
        int index1 = url.indexOf('?');
        if (index1 == -1) {
            index1 = url.length();
        }
        int index2 = url.lastIndexOf('.', index1);
        if (index2 == -1) {
            return null;
        }
        int index3 = url.indexOf('/', 8);
        if (index3 == -1) {
            return null;
        }
        String ext = url.substring(index2 + 1, index1);
        if (ext.matches("[^\\/\\\\]*")) {
            return ext;
        }
        return null;
    }

    public static String getURLFileName(String url) {
        if (isEmpty(url)) {
            return null;
        }
        int index1 = url.indexOf('?');
        if (index1 == -1) {
            index1 = url.length();
        }
        int index2 = url.lastIndexOf('/', index1);
        if ((index2 == -1) || (index2 < 8)) {
            return null;
        }
        String ext = url.substring(index2 + 1, index1);
        return ext;
    }

    public static byte[] GBKToUTF8(String chinese) {
        return GBKToUTF8(chinese, false);
    }

    public static byte[] GBKToUTF8(String chinese, boolean bomFlag) {
        return CharsetConvert.GBKToUTF8(chinese, bomFlag);
    }

    public static byte[] UTF8ToGBK(String chinese) {
        return CharsetConvert.UTF8ToGBK(chinese);
    }

    public static String clearForXML(String str) {
        char[] cs = str.toCharArray();
        char[] ncs = new char[cs.length];
        int j = 0;
        for (char element : cs)
            if (element <= 65533) {
                if (element < ' ') {
                    if (((element != '\t' ? 1 : 0) & (element != '\n' ? 1 : 0) & (element != '\r' ? 1 : 0)) != 0) ;
                } else
                    ncs[(j++)] = element;
            }
        ncs = ArrayUtils.subarray(ncs, 0, j);
        return new String(ncs);
    }

    public static String concat(Object[] args) {
        if (ObjectUtil.empty(args)) {
            return null;
        }
        FastStringBuilder sb = new FastStringBuilder();
        Object[] arrayOfObject = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            Object arg = arrayOfObject[i];
            if (ObjectUtil.notEmpty(arg)) {
                sb.append(arg);
            }
        }
        return sb.toStringAndClose();
    }

    public static String quickHtmlEncode(String html) {
        boolean flag = false;
        for (int j = 0; j < html.length(); j++) {
            char c = html.charAt(j);
            if ((c == ' ') || (c == '"') || (c == '\'') || (c == '<') || (c == '>') || (c == '&')) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            return html;
        }
        FastStringBuilder sb = new FastStringBuilder();
        int last = 0;
        for (int j = 0; j < html.length(); j++) {
            char c = html.charAt(j);
            if ((c == ' ') || (c == '"') || (c == '\'') || (c == '<') || (c == '>') || (c == '&')) {
                if (last != j) {
                    sb.append(html.substring(last, j));
                }
                if (c == '"')
                    sb.append("&quot;");
                else if (c == '\'')
                    sb.append("&apos;");
                else if (c == ' ')
                    sb.append("&#32;");
                else if (c == '<')
                    sb.append("&lt;");
                else if (c == '>')
                    sb.append("&gt;");
                else if (c == '&') {
                    sb.append("&amp;");
                }
                last = j + 1;
            }
        }
        if (last != html.length()) {
            sb.append(html.substring(last));
        }
        return sb.toStringAndClose();
    }

    public static String quickHtmlDecode(String html) {
        boolean flag = false;
        for (int j = 0; j < html.length(); j++) {
            if (html.charAt(j) == '&') {
                flag = true;
                break;
            }
        }
        if (!flag) {
            return html;
        }
        char[] buf = new char[html.length()];
        int j = 0;
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '&') {
                if (html.startsWith("&quot;", j)) {
                    buf[(j++)] = '"';
                    i += 5;
                } else if (html.startsWith("&amp;", i)) {
                    buf[(j++)] = '&';
                    i += 4;
                } else if (html.startsWith("&lt;", i)) {
                    buf[(j++)] = '<';
                    i += 3;
                } else if (html.startsWith("&gt;", i)) {
                    buf[(j++)] = '>';
                    i += 3;
                } else if (html.startsWith("&apos;", i)) {
                    buf[(j++)] = '\'';
                    i += 5;
                } else if (html.startsWith("&nbsp;", i)) {
                    buf[(j++)] = ' ';
                    i += 5;
                } else if (html.startsWith("&#32;", i)) {
                    buf[(j++)] = ' ';
                    i += 4;
                } else if (html.charAt(i + 1) == '#') {
                    int k = i + 2;
                    flag = false;
                    int radix = 10;
                    if ((html.charAt(k) == 'x') || (html.charAt(k) == 'X')) {
                        radix = 16;
                        k++;
                    }
                    for (; (k < i + 9) && (k < html.length()); k++) {
                        if (html.charAt(k) == ';') {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        char ch = (char) Integer.parseInt(html.substring(radix == 10 ? i + 2 : i + 3, k), radix);
                        buf[(j++)] = ch;
                        i += k;
                    }
                }
            } else buf[(j++)] = c;
        }

        return new String(buf, 0, j);
    }

    @Deprecated
    public static String htmlTagEncode(String str) {
        if (isEmpty(str))
            return str;
        while (true) {
            Matcher matcher = PHtmlTag.matcher(str);
            if (!matcher.find()) break;
            String content = matcher.group(0);
            str = matcher.replaceFirst(htmlEncode(content));
        }

        return str.replaceAll("[\\s　]{2,}", " ");
    }

    /**
     * 格式化文本, {} 表示占位符<br>
     * 此方法只是简单将占位符 {} 按照顺序替换为参数<br>
     * 如果想输出 {} 使用 \\转义 { 即可，如果想输出 {} 之前的 \ 使用双转义符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "a", "b") =》 this is a for b<br>
     * 转义{}： format("this is \\{} for {}", "a", "b") =》 this is \{} for a<br>
     * 转义\： format("this is \\\\{} for {}", "a", "b") =》 this is \a for b<br>
     *
     * @param template 文本模板，被替换的部分用 {} 表示，如果模板为null，返回"null"
     * @param params   参数值
     * @return 格式化后的文本，如果模板为null，返回"null"
     */
    public static String format(CharSequence template, Object... params) {
        if (null == template) {
            return null;
        }
        if (ArrayUtils.isEmpty(params) || isBlank(template)) {
            return template.toString();
        }
        return StrFormatter.format(template.toString(), params);
    }
    public static String getMessage(Throwable e) {
        if (null == e) {
            return null;
        }
        return format("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * 如果给定字符串不是以suffix结尾的，在尾部补充 suffix
     *
     * @param str    字符串
     * @param suffix 后缀
     * @return 补充后的字符串
     */
    public static String addSuffixIfNot(CharSequence str, CharSequence suffix) {
        if (isBlank(str) || isBlank(suffix)) {
            return str(str);
        }

        final String str2 = str.toString();
        final String suffix2 = suffix.toString();
        if (false == str2.endsWith(suffix2)) {
            return str2.concat(suffix2);
        }
        return str2;
    }

    /**
     * 当给定字符串为null时，转换为Empty
     *
     * @param str 被转换的字符串
     * @return 转换后的字符串
     */
    public static String nullToEmpty(CharSequence str) {
        return nullToDefault(str, EMPTY);
    }


    public static String nullToDefault(CharSequence str, String defaultStr) {
        return (str == null) ? defaultStr : str.toString();
    }
    public static Charset charset(String charsetName) throws UnsupportedCharsetException {
        return isBlank(charsetName) ? Charset.defaultCharset() : Charset.forName(charsetName);
    }
    /**
     * 编码字符串
     *
     * @param str     字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 编码后的字节码
     */
    public static byte[] bytes(CharSequence str, Charset charset) {
        if (str == null) {
            return null;
        }

        if (null == charset) {
            return str.toString().getBytes();
        }
        return str.toString().getBytes(charset);
    }

    /**
     * 去掉指定前缀
     *
     * @param str    字符串
     * @param prefix 前缀
     * @return 切掉后的字符串，若前缀不是 preffix， 返回原字符串
     */
    public static String removePrefix(CharSequence str, CharSequence prefix) {
        if (isBlank(str) || isBlank(prefix)) {
            return str(str);
        }

        final String str2 = str.toString();
        if (str2.startsWith(prefix.toString())) {
            return subString(str2, prefix.length());// 截取后半段
        }
        return str2;
    }

    /**
     * 截取分隔字符串之前的字符串，不包括分隔字符串<br>
     * 如果给定的字符串为空串（null或""）或者分隔字符串为null，返回原字符串<br>
     * 如果分隔字符串为空串""，则返回空串，如果分隔字符串未找到，返回原字符串，举例如下：
     *
     * <pre>
     * StrUtil.subBefore(null, *, false)      = null
     * StrUtil.subBefore("", *, false)        = ""
     * StrUtil.subBefore("abc", "a", false)   = ""
     * StrUtil.subBefore("abcba", "b", false) = "a"
     * StrUtil.subBefore("abc", "c", false)   = "ab"
     * StrUtil.subBefore("abc", "d", false)   = "abc"
     * StrUtil.subBefore("abc", "", false)    = ""
     * StrUtil.subBefore("abc", null, false)  = "abc"
     * </pre>
     *
     * @param string          被查找的字符串
     * @param separator       分隔字符串（不包括）
     * @param isLastSeparator 是否查找最后一个分隔字符串（多次出现分隔字符串时选取最后一个），true为选取最后一个
     * @return 切割后的字符串
     * @since 3.1.1
     */
    public static String subBefore(CharSequence string, CharSequence separator, boolean isLastSeparator) {
        if (isBlank(string) || separator == null) {
            return null == string ? null : string.toString();
        }

        final String str = string.toString();
        final String sep = separator.toString();
        if (sep.isEmpty()) {
            return EMPTY;
        }
        final int pos = isLastSeparator ? str.lastIndexOf(sep) : str.indexOf(sep);
        if (INDEX_NOT_FOUND == pos) {
            return str;
        }
        if (0 == pos) {
            return EMPTY;
        }
        return str.substring(0, pos);
    }

    public static String trim(CharSequence str) {
        return (null == str) ? null : trim(str, 0);
    }
    public static String trim(CharSequence str, int mode) {
        if (str == null) {
            return null;
        }

        int length = str.length();
        int start = 0;
        int end = length;

        // 扫描字符串头部
        if (mode <= 0) {
            while ((start < end) && (CharUtil.isBlankChar(str.charAt(start)))) {
                start++;
            }
        }

        // 扫描字符串尾部
        if (mode >= 0) {
            while ((start < end) && (CharUtil.isBlankChar(str.charAt(end - 1)))) {
                end--;
            }
        }

        if ((start > 0) || (end < length)) {
            return str.toString().substring(start, end);
        }

        return str.toString();
    }

}
