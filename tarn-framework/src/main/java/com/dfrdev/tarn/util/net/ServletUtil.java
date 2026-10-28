package com.dfrdev.tarn.util.net;


import com.dfrdev.tarn.collection.Mapx;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import com.dfrdev.tarn.util.LogUtil;

import com.dfrdev.tarn.util.classloader.ObjectUtil;
import com.dfrdev.tarn.util.io.FileUtil;
import com.dfrdev.tarn.util.text.CharsetUtil;
import com.dfrdev.tarn.util.text.FastStringBuilder;
import com.dfrdev.tarn.util.text.StringUtil;
public class ServletUtil {

    public static Mapx<String, String> getParameterMap(HttpServletRequest request) {
        Mapx map = new Mapx();
        Map tmap = request.getParameterMap();
        for (Object key : tmap.keySet()) {
            Object value = tmap.get(key+"");
            map.put(key, getParameterValue(value));
        }
        Enumeration e = request.getAttributeNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            Object value = request.getAttribute(key);
            map.put(key, getParameterValue(value));
        }
        return map;
    }

    private static String getParameterValue(Object value) {
        if (value == null) {
            return null;
        }
        if (!value.getClass().isArray()) {
            return String.valueOf(value);
        }
        Object[] arr = ObjectUtil.toObjectArray(value);
        if (arr.length == 1) {
            return String.valueOf(arr[0]);
        }
        return StringUtil.join(arr);
    }

    public static Mapx<String, String> getMapFromQueryString(String url) {
        Mapx map = new Mapx();
        int index = url.indexOf('?');
        if (index < 0) {
            return map;
        }
        String str = url.substring(url.indexOf('?') + 1);
        int index2 = str.lastIndexOf('#');
        if (index2 != -1) {
            str = str.substring(0, index2);
        }
        String[] arr = str.split("\\&");
        for (String element : arr) {
            String[] arr2 = element.split("\\=");
            if (arr2.length == 2)
                map.put(arr2[0], arr2[1]);
            else if (arr2.length > 0) {
                map.put(arr2[0], "");
            }
        }
        return map;
    }

    public static String getQueryStringFromMap(Mapx<?, ?> map) {
        return getQueryStringFromMap(map, false);
    }

    public static String getQueryStringFromMap(Mapx<?, ?> map, boolean encodeFlag) {
        FastStringBuilder sb = new FastStringBuilder();
        sb.append("?");
        boolean first = true;
        for (Iterator localIterator = map.keySet().iterator(); localIterator.hasNext(); ) {
            Object k = localIterator.next();
            Object v = map.get(k);
            if (v != null) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(k);
                sb.append("=");
                if (encodeFlag)
                    sb.append(StringUtil.urlEncode(v.toString()));
                else {
                    sb.append(v.toString());
                }
                first = false;
            }
        }
        return sb.toStringAndClose();
    }

    public static String getURLContent(String url, String encoding)
            throws Exception {
        FastStringBuilder sb = new FastStringBuilder();
        InputStreamReader isr = null;
        if (StringUtil.isNotEmpty(encoding))
            isr = new InputStreamReader(new URL(url).openStream(), encoding);
        else {
            isr = new InputStreamReader(new URL(url).openStream());
        }
        BufferedReader br = new BufferedReader(isr);
        String s = null;
        while ((s = br.readLine()) != null) {
            sb.append(s);
            sb.append("\n");
        }
        br.close();
        return sb.toStringAndClose();
    }

    public static String postURLContent(String url, Map<String, String> params, String encode) {
        StringBuilder sb = null;
        String result = null;
        HttpURLConnection conn = null;
        try {
            for (Map.Entry entry : params.entrySet()) {
                if (sb == null)
                    sb = new StringBuilder();
                else {
                    sb.append('&');
                }
                sb.append((String) entry.getKey()).append('=');
                sb.append(URLEncoder.createQuery().encode((String) entry.getValue(), CharsetUtil.charset(encode)));
            }
            byte[] entity = sb.toString().getBytes(encode);
            URL _url = new URL(url);
            conn = (HttpURLConnection) _url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();
            OutputStream outStream = conn.getOutputStream();
            outStream.write(entity);
            outStream.flush();
            outStream.close();
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                result = FileUtil.readText(is, encode);
            }
        } catch (IOException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        return getCookieValue(request, cookieName, "");
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName, String defaultValue) {
        Cookie[] cookies = request.getCookies();
        for (int i = 0; (cookies != null) && (i < cookies.length); i++) {
            Cookie cookie = cookies[i];
            if (cookieName.equalsIgnoreCase(cookie.getName())) {

                return URLDecoder.decode(cookie.getValue(), CharsetUtil.charset("UTF-8"));
            }
        }
        return defaultValue;
    }

    public static void setCookieValue(HttpServletRequest request, HttpServletResponse response, String cookieName, int maxAge, String cValue) {
        Cookie[] cookies = request.getCookies();
        boolean cookieexistflag = false;
        String contextPath = request.getContextPath();
        contextPath = contextPath.substring(0, contextPath.length() - 1);
        cValue = URLEncoder.createQuery().encode(cValue, CharsetUtil.charset("UTF-8"));
        for (int i = 0; (cookies != null) && (i < cookies.length); i++) {
            Cookie cookie = cookies[i];
            if (cookieName.equalsIgnoreCase(cookie.getName())) {
                cookieexistflag = true;
                cookie.setValue(cValue);
                cookie.setPath(contextPath);
                cookie.setMaxAge(maxAge);
                response.addCookie(cookie);
            }
        }
        if (!cookieexistflag) {
            Cookie cookie = new Cookie(cookieName, cValue);
            cookie.setPath(contextPath);
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);
        }
    }

    public static String getUrlExtension(String url) {
        if (StringUtil.isEmpty(url)) {
            return "";
        }
        int index = url.indexOf('?');
        if (index > 0) {
            url = url.substring(0, index);
        }
        int i1 = url.lastIndexOf('/');
        int i2 = url.lastIndexOf('.');
        if (i1 >= i2) {
            return "";
        }
        return url.substring(i2).toLowerCase();
    }

    public static String getFileName(String url) {
        int last = 0;
        boolean colonFlag = false;
        String fileName = url;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if ((c == '?') || (c == '#')) {
                fileName = url.substring(0, i);
                break;
            }
            if (c == '/')
                last = i;
            else if (c == ':') {
                colonFlag = true;
            }
        }
        if ((colonFlag) && ((fileName.startsWith("http://")) || (fileName.startsWith("https://")))) {
            if (last < 8)
                fileName = "";
            else
                fileName = fileName.substring(last + 1);
        } else if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        fileName = FileUtil.normalizePath(fileName);
        return fileName;
    }


    public static String getQueryStringFromMap(Map<?, ?> map) {
        return getQueryStringFromMap(map, false);
    }

    public static String getQueryStringFromMap(Map<?, ?> map, boolean encodeFlag) {
        FastStringBuilder sb = new FastStringBuilder();
        sb.append("?");
        boolean first = true;
        for (Iterator localIterator = map.keySet().iterator(); localIterator.hasNext(); ) {
            Object k = localIterator.next();
            Object v = map.get(k);
            if (v != null) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(k);
                sb.append("=");
                if (encodeFlag)
                    sb.append(StringUtil.urlEncode(v.toString()));
                else {
                    sb.append(v.toString());
                }
                first = false;
            }
        }
        return sb.toStringAndClose();
    }


    public static String getHost(String url) {
        if ((!url.startsWith("http://")) && (!url.startsWith("https://"))) {
            LogUtil.error("Invalid URL" + url);
            return null;
        }
        int index = url.indexOf("//") + 2;
        int index2 = url.indexOf("/", index);
        if (index2 <= 0) {
            index2 = url.length();
        }
        return url.substring(index, index2);
    }

    /**
     * 获取客户端IP地址
     */
    public static String getRealIP(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtil.isNotEmpty(request.getHeader("Nginx-Forwarded-For"))) {
            ip = request.getHeader("Nginx-Forwarded-For");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("X-Real-IP");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if ((StringUtil.isEmpty(ip)) || ("unknown".equalsIgnoreCase(ip))) {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            if (ip.indexOf(' ') > 0) {
                ip = ip.substring(0, ip.indexOf(' '));
            }
            if (ip.indexOf(',') > 0) {
                ip = ip.substring(0, ip.indexOf(','));
            }
        }
        return ip;
    }


    /**
     * 根据IP地址获取地理位置，如：北京市
     */
    public static String getAddressByIP(String ip) {
        String ipAddress = ip;
        try {
            URL url = new URL("http://www.ip138.com/ips138.asp?ip=" + ip);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "GBK"));
            String line = null;
            StringBuffer result = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            ip = result.substring(result.indexOf("主数据：") + 4, result.indexOf("</li><li>参考数据"));
            char[] ipStr = ip.toCharArray();
            for (int i = 0; i < ipStr.length; i++) {
                if (i != 0 && !Character.isLetter(ipStr[i])) {    //判断是否汉字，不含中文特殊符号
                    ip = ip.substring(0, i);
                    break;
                }
            }
        } catch (StringIndexOutOfBoundsException indexOut) {
            LogUtil.error("IP地址获取失败：下标越界\t" + ipAddress);
            return ipAddress;
        } catch (IOException e) {
            return ipAddress;
        }
        return ip;
    }
}