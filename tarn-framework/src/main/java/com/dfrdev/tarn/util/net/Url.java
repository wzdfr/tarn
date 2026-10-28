package com.dfrdev.tarn.util.net;


import com.dfrdev.tarn.core.TarnException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import com.dfrdev.tarn.util.LogUtil;
/**
 * 操作URL工具类
 */
public class Url {
    public static void getImageResourcByURL(String imagesFile)
            throws IOException {// 获取URL指定的资源
        URL url = new URL(imagesFile);
        Object obj = url.getContent();// 获得此URL的内容
        System.out.println(obj.getClass().getName());// 显示名称
    }

    public static void getHtmlResourceByURL(String htmlFile) throws IOException {// 获取URL指定的资源
        URL url = new URL(htmlFile);
        URLConnection uc = url.openConnection();// 创建远程对象连接对象
        InputStream in = uc.getInputStream();// 打开的连接读取的输入流
        int c;
        while ((c = in.read()) != -1) {// 循环读取资源信息
            System.out.print((char) c);
        }
        System.out.println();
        in.close();
    }

    public static void getHTMLResource(String htmlFile) throws IOException {// 读取URL指定的网页内容
        URL url = new URL(htmlFile);// 创建URL对象
        Reader reader = new InputStreamReader(new BufferedInputStream(
                url.openStream()));// 打开URL连接创建一个读对象
        int c;
        while ((c = reader.read()) != -1) {// 循环读取资源信息
            System.out.print((char) c);
        }
        System.out.println();
        reader.close();
    }

    public static void getResourceOfHTML(String htmlFile) throws IOException {// 读取URL指定的网页内容
        URL url = new URL(htmlFile);
        InputStream in = url.openStream();// 打开URL连接创建输入流
        int c;
        while ((c = in.read()) != -1) {// 循环读取资源信息
            System.out.print((char) c);
        }
        System.out.println();
        in.close();
    }

    /**
     * 返回当前Url对象所表示的URL地址字符串.
     *
     * @return String URL地址字符串.
     */
    @Override
    public String toString() {
        return url + bookmark;
    }

    public static void supportURLType(String host, String file) {// Java所支持的URL类型
        String[] schemes = {"http", "https", "ftp", "mailto", "telnet",
                "file", "ldap", "gopher", "jdbc", "rmi", "jndi", "jar", "doc",
                "netdoc", "nfs", "verbatim", "finger", "daytime",
                "systemresource"};// 创建URL类型数组
        for (int i = 0; i < schemes.length; i++) {// 循环遍历数组判断是否是java支持的URL类型
            try {
                URL u = new URL(schemes[i], host, file);
                LogUtil.debug(schemes[i] + "是java所支持的URL类型\r\n");
            } catch (Exception ex) {
                LogUtil.error(schemes[i] + "不是java所支持的URL类型\r\n");
            }
        }
    }

    /**
     * 以默认的utf-8编码的形式构建一个Url类的实例.
     *
     * @param url 要处理的URL资源地址。
     */
    public Url(String url) {
        this(url, "utf-8");
    }

    public Url() {
    }

    /**
     * 向Url对象所指向的URL资源发送请求(POST或GET),并获取返回的HTML文本数据.
     * <br/>说明：如果在调用此方法之前已经调用过{@link #post(String, String)}方法,那么系统将以POST方式发送请求,
     * 否则将使用默认的GET方式发送请求.
     *
     * @return String 返回所请求URL资源的HTML内容.
     * 如果返回的内容出现乱码,请检查构造此Url对象时所使用的charset参数值是否与所请求的URL资源所使用的编码一致.
     */
    public String getHtml() throws IOException {
        if (hm == null) {
            return sendGet();
        } else {
            return sendPost();
        }
    }

    public Url(String url, String charset) {
        if (url.indexOf("#") == -1) {
            this.url = url;
        } else {
            bookmark = url.substring(url.indexOf("#"));
            this.url = url.substring(0, url.indexOf("#"));
        }
        this.charset = charset;
    }

    public String get(String name) throws TarnException {
        String rtn;
        String s = name + "=";
        String b;
        if (url.indexOf("?" + s) != -1) {
            b = "?" + s;
        } else if (url.indexOf("&" + s) != -1) {
            b = "&" + s;
        } else {
            return "";
        }
        int iEnd = url.indexOf("&", url.indexOf(b) + b.length());
        if (iEnd == -1) {
            rtn = url.substring(url.indexOf(b) + b.length());
        } else {
            rtn = url.substring(url.indexOf(b) + b.length(), iEnd) + "";
        }
        try {
            return charset == null ? rtn : java.net.URLDecoder.decode(rtn, charset);
        } catch (UnsupportedEncodingException e) {
            throw new TarnException("未知的编码类型" + charset);
        }
    }

    public Url del(String names) {
        String nameA[] = names.replaceAll(" ", "").split(",");
        for (String n : nameA) {
            if (n == null || n.equals("")) {
                continue;
            }
            String s = n + "=";
            if (url.contains("?" + s)) {
                int iEnd = url.indexOf("&", url.indexOf("?" + s));
                if (iEnd == -1) {
                    url = url.substring(0, url.indexOf("?"));
                } else {
                    url = url.substring(0, url.indexOf("?") + 1) + url.substring(iEnd + 1);
                }
            } else if (url.indexOf("&" + s) != -1) {
                int iEnd = url.indexOf("&", url.indexOf("&" + s) + s.length() + 1);
                if (iEnd == -1) {
                    url = url.substring(0, url.indexOf("&" + s));
                } else {
                    url = url.substring(0, url.indexOf("&" + s)) + url.substring(iEnd);
                }
            }
        }
        return this;
    }

    /**
     * 以POST的方式向该URL地址发送数据. <br />
     *
     * @param name  要发送数据的变量名称.
     * @param value 要发送数据的字符串值. 该值将以当前Url对象指定的charset进行编码并发送.
     * @return Url 当前的Url对象
     */
    public Url post(String name, String value) throws TarnException {
        if (hm == null) {
            hm = new java.util.HashMap<String, String>();
        }
        try {
            hm.put(name, charset == null ? value : java.net.URLEncoder.encode(value, charset));
        } catch (UnsupportedEncodingException e) {
            throw new TarnException("未知的编码类型" + charset);
        }
        return this;
    }

    public Url set(String name, int value) throws TarnException {
        return set(name, String.valueOf(value));
    }


    public Url set(String name, String value) throws TarnException {
        try {
            if (charset != null) {
                value = java.net.URLEncoder.encode(value, charset);
            }
        } catch (UnsupportedEncodingException e) {
            throw new TarnException("未知的编码类型" + charset);
        }
        String s = name + "=";
        String b = null;
        if (url.indexOf("?" + s) != -1) {
            b = "?" + s;
        } else if (url.indexOf("&" + s) != -1) {
            b = "&" + s;
        }
        if (b == null) {
            url += (url.indexOf("?") == -1 ? "?" : "&") + s + value;
        } else {
            int iEnd = url.indexOf("&", url.indexOf(b) + b.length());
            if (iEnd == -1) {
                url = url.substring(0, url.indexOf(b) + b.length()) + value;
            } else {
                url = url.substring(0, url.indexOf(b) + b.length()) + value + url.substring(iEnd);
            }
        }
        return this;
    }

    /**
     * 返回将要(或者已经)向URL地址POST过去的数据内容.
     *
     * @return 形如：name1=value1&name2=value2 ... 格式组成的字符串.
     * 其中的value值已经根据构造方法所指定的charset进行了编码处理.
     * @see #post(String, String)
     */
    public String getPost() {
        if (hm == null) {
            return "";
        }
        String s = hm.toString().replaceAll(", ", "&");
        return s.substring(1, s.length() - 1);
    }

    private String sendGet() throws IOException {
        URL urlX = new URL(this.toString());
        InputStream in = urlX.openStream();
        String content = pipe(in, charset);
        in.close();
        return content;
    }

    private String sendPost() throws IOException {
        String tCharset = charset == null ? "utf-8" : charset;
        String data = getPost();
        URL urlX = new URL(url);
        URLConnection conn = urlX.openConnection();
        conn.setUseCaches(false);//禁缓存
            /*conn.setRequestProperty("Pragma:", "no-cache");
        conn.setRequestProperty("Cache-Control", "no-cache");*/
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), tCharset);
        out.write(data);
        out.flush();
        out.close();
        InputStream in = conn.getInputStream();
        String content = pipe(in, tCharset);
        in.close();
        return content;
    }

    private static String pipe(InputStream in, String charset) throws IOException {
        StringBuffer s = new StringBuffer();
        if (charset == null || "".equals(charset)) {
            charset = "utf-8";
        }
        String rLine;
        BufferedReader bReader = new BufferedReader(new InputStreamReader(in, charset));
        while ((rLine = bReader.readLine()) != null) {
            int str_len = rLine.length();
            if (str_len > 0) {
                s.append(rLine);
            }
        }
        in.close();
        return s.toString();
    }

    private String url;
    private String charset = "utf-8";
    private String bookmark = "";
    private java.util.Map<String, String> hm = null;

    public static void main(String[] args) throws IOException {

        String imageFile = "http://www.baidu.com/001.jpg";
        String htmlFile = "http://www.baidu.com/";
        String host = "http://www.baidu.com";
        String file = "";
        System.out.println("1.//获取URL指定的图像资源信息");
        getImageResourcByURL(imageFile);
        System.out.println("2.获取URL指定的HTML网页资源信息");
        getHtmlResourceByURL(htmlFile);
        System.out.println("3.根据URL创建读对象读取网页内容");
        getHTMLResource(htmlFile);
        System.out.println("4.根据URL创建输入流读取网页内容");
        getResourceOfHTML(htmlFile);
        System.out.println("5.判断Java所支持的URL类型 ");
        supportURLType(host, file);
    }
}