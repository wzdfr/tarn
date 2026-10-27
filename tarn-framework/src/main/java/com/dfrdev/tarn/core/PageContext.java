package com.dfrdev.tarn.core;

import com.dfrdev.tarn.util.FileKit;
import com.dfrdev.tarn.util.StringUtil;
import com.dfrdev.tarn.util.TextKit;
import com.dfrdev.tarn.util.Url;
import com.oreilly.servlet.MultipartRequest;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;

/**
 * 页面上下文对象
 * 此对象无需实例化，请使用Tarn.getPageContext()静态方法获得.
 */
public class PageContext {
    PageContext(Map<String, Object> context) {
        this.pageContext = context;
    }

    private PageContext() {
    }

    /**
     * @return HttpServletRequest
     */
    public HttpServletRequest getRequest() {
        if (request == null) {
            request = (HttpServletRequest) pageContext.get(HTTP_REQUEST);
        }
        return request;
    }

    /**
     * @return HttpServletResponse
     */
    public HttpServletResponse getResponse() {
        if (response == null) {
            response = (HttpServletResponse) pageContext.get(HTTP_RESPONSE);
        }
        return response;
    }

    /**
     * @return MultipartRequest
     */
    public MultipartRequest getMultipartRequest() {
        if (muRequest == null) {
            muRequest = (MultipartRequest) pageContext.get(HTTP_MULTIPART_REQUEST);
        }
        return muRequest;
    }

    /**
     * 请求当中有文件上传功能，需要得到上传的文件必须要在最前面调用此方法初始化<br/>
     *
     * @param uploadMaxSizeAllowed 上传文件最大大小
     */
    public void initMultipartRequest(int uploadMaxSizeAllowed) throws IOException {
        String basePath = getRequest().getSession().getServletContext().getRealPath(File.separator + "FileDATA" + File.separator + "Temp" + File.separator);
        FileKit.newFolder(basePath);
        UploadRequestProxy uploadRequest = new UploadRequestProxy(getRequest(), new UploadSize());
        pageContext.put(HTTP_MULTIPART_REQUEST,
                new MultipartRequest(
                        uploadRequest,
                        basePath,
                        uploadMaxSizeAllowed, "utf-8",
                        new com.oreilly.servlet.multipart.DefaultFileRenamePolicy()));
    }

    /***
     * 设置mulitippartRequest
     */
    public void setMultipartRequest(MultipartRequest multipartRequest) {
        pageContext.put(HTTP_MULTIPART_REQUEST, multipartRequest);
    }


    /**
     * 取Post或Get过来的参数值,并将此值格式化为整数. 如果该值不能被格式化，将返回0. <br/>
     * 此方法等同于：get(str, 0)
     *
     * @param str 要获取的参数名称。
     * @return int 一个整数值。
     */
    public int getInt(String str) {
        return TextKit.parseInt(get(str));
    }

    public int getInt(String str, int defnum) {
        return TextKit.parseInt(get(str, String.valueOf(defnum), ""));
    }

    /**
     * 取Post或Get过来的参数值,并将此值格式化为整数. 如果该值不能被格式化，将返回0. <br/>
     * 此方法等同于：get(str, 0)
     *
     * @param str 要获取的参数名称。
     * @return String
     */
    public String get(String str) {
        return get(str, "", "");
    }

    public String get(String str, String encode) {
        return get(str, encode, "");
    }

    /**
     * 取Post或Get过来的参数值,并将此值格式化为整数. 如果该值不能被格式化，将返回0. <br/>
     * 此方法等同于：get(str, 0)
     *
     * @param str 要获取的参数名称。
     * @return String
     */
    public String get(String str, String encode, String defStr) {
        if (StringUtil.isEmpty(encode)) encode = "UTF-8";
        String prm = getMultipartRequest() == null
                ? getRequest().getParameter(str) : getMultipartRequest().getParameter(str);
        if (StringUtil.isEmpty(prm)) return defStr;
        else return prm;
    }


    public String getUrlQuery(String str, String encode) {
        if (StringUtil.isEmpty(encode)) encode = "UTF-8";
        String queryString = getRequest().getQueryString();
        String url = null;
        try {
            url = new Url(queryString, encode).get(str);
        } catch (Exception ex) {
            url = null;
        }
        return url;
    }

    public Enumeration getEnum() {
        return getRequest().getParameterNames();
    }

    private String strEncodeing(String strSource, String encode) {
        if (encode.equals("")) encode = "UTF-8";     //默认utf-8
        String newStr = strSource;
        try {
//            newStr = new String (strSource.getBytes(Charset.forName(encode)), encode);
            newStr = new String(strSource.getBytes("ISO-8859-1"), encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return newStr;
    }

    public String cookie(String name) {
        Cookie[] c = getRequest().getCookies();
        if (c != null) {
            for (Cookie aC : c) {
                if (aC.getName().equals(name)) {
                    try {
                        return aC.getValue() == null ? null : URLDecoder.decode(aC.getValue(), "utf-8");
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }


    public PageContext cookie(String name, String value) {
        return cookie(name, value, -1);
    }

    public PageContext cookie(String name, String value, int maxAge) {
        if (name == null) return this;
        if (value == null) {
            Cookie[] c = getRequest().getCookies();
            if (c == null) return this;
            for (Cookie aC : c)
                if (name.equals(aC.getName())) {
                    aC.setMaxAge(0);
                    aC.setPath(getRequest().getContextPath() + "/");
                    getResponse().addCookie(aC);
                    break;
                }
        } else {
            try {
                Cookie c = new Cookie(name, URLEncoder.encode(value, "utf-8"));
                c.setMaxAge(maxAge == 0 ? -1 : maxAge);
                c.setPath(getRequest().getContextPath() + "/");
                getResponse().addCookie(c);
            } catch (UnsupportedEncodingException ignore) {
            }
        }
        return this;
    }

    public boolean isSessionExpired(HttpServletRequest request) {
        return request.getSession() == null;
//        return (request.getRequestedSessionId() != null)
//                && !request.isRequestedSessionIdValid();
    }

    /**
     * 获取当前的应用程序会话级变量. <br />
     * (会话变量可能是session存储,也可能是cookie存储,存储方式由sys-config.properties中的ksrType属性配置)
     *
     * @param name 要获取的会话级变量的名称。
     * @return 指定的会话级变量的值, 如果该变量不存在，将返回空字符串"".
     */
    public String ksr(String name) {
        String rtn = null;
        String varKeep = Tarn.sysDevConf("ksrType").toLowerCase();
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        if (isSessionExpired(request)) {
            return "";
        }
        if (varKeep.equals("session") || varKeep.equals("both")) {
            try {
                HttpSession session = request.getSession();
                if (session != null) {
                    rtn = (String) session.getAttribute(name);
                }
            } catch (Exception e) {
                return null;
            }

        }
        if (rtn != null) {
            return rtn;
        }
        if (varKeep.equals("cookie") || varKeep.equals("both")) {
            Cookie c[] = request.getCookies();
            if (c != null) {
                for (Cookie aC : c) {
                    if (aC.getName().equals(name)) {
                        try {
                            if (aC.getValue() == null) {
                                rtn = null;
                            } else {
                                rtn = URLDecoder.decode(aC.getValue(), "utf-8");
                            }
                        } catch (UnsupportedEncodingException ignored) {
                        }
                        break;
                    }
                }
            }
        }
        return rtn == null ? "" : rtn;
    }

    /**
     * 创建或删除当前应用程序的会话级变量.  <br />
     * (会话变量可能是session存储,也可能是cookie存储,存储方式由sys-config.properties中的ksrType属性配置)<Br/>
     *
     * @param name  要创建或删除的会话级变量名称。<br />
     * @param value 设置会话变量的值,如果此值为null,将从会话中删除name参数所指定的变量.
     *              如果value的值等于null(即删除变量)时, 那么name可以是由逗号分隔的多个变量名,例如：<br />
     *              pc.ksr("vipId, vipRole, vipUid", null);　这样程序将会同时删除vipId, vipRole, vipUid三个会话变量.
     * @return 当前的PageContext对象。
     */
    public PageContext ksr(String name, String value) {
        String varKeep = Tarn.sysDevConf("ksrType").toLowerCase();
        HttpServletRequest request = getRequest();
        if (request == null) {
            return this;
        }
        if (isSessionExpired(request)) {
            return this;
        }
        if (value != null) {//create mode
            if (varKeep.equals("session") || varKeep.equals("both")) {
                request.getSession().setAttribute(name, value);
            }
            if (varKeep.equals("cookie") || varKeep.equals("both")) {
                try {
                    Cookie c = new Cookie(name, URLEncoder.encode(value, "utf-8"));
                    int expSec = TextKit.parseInt(Tarn.sysDevConf("varExpires"), -1);
                    if (expSec == 0) {
                        expSec = -1;
                    }
                    c.setMaxAge(expSec);
                    c.setPath(getRequest().getContextPath() + "/");
                    getResponse().addCookie(c);
                } catch (UnsupportedEncodingException e) {
                    //URLEncoder中的编码为utf-8肯定支持，实际不会抛出异常
                    return this;
                }
            }
        } else {
            if (varKeep.equals("session") || varKeep.equals("both")) {

                if (name.indexOf(",") == -1) {
                    request.getSession().removeAttribute(name);
                } else {
                    String na[] = name.replaceAll(" ", "").split(",");
                    for (String aNa : na) {
                        request.getSession().removeAttribute(aNa);
                    }
                }
            }
            if (varKeep.equals("cookie") || varKeep.equals("both")) {
                Cookie c[] = request.getCookies();
                if (c == null) {
                    return this;
                }
                for (Cookie aC : c) {
                    if (name.indexOf(aC.getName()) != -1) {
                        aC.setValue(null);
                        aC.setMaxAge(0);
                        aC.setPath(getRequest().getContextPath() + "/");
                        getResponse().addCookie(aC);
                    }
                }
            }
        }
        return this;
    }


    /**
     * 用于需要不返回内容时。如Freemaker视图调用场景<br/>
     * 创建或删除当前应用程序的会话级变量.  <br />
     * (会话变量可能是session存储,也可能是cookie存储,存储方式由sys-config.properties中的ksrType属性配置)<Br/>
     *
     * @param name  要创建或删除的会话级变量名称。<br />
     * @param value 设置会话变量的值,如果此值为null,将从会话中删除name参数所指定的变量.
     *              如果value的值等于null(即删除变量)时, 那么name可以是由逗号分隔的多个变量名,例如：<br />
     *              pc.ksr("vipId, vipRole, vipUid", null);　这样程序将会同时删除vipId, vipRole, vipUid三个会话变量.
     * @return 当前的PageContext对象。
     */
    public void ksrN(String name, String value) {
        String varKeep = Tarn.sysDevConf("ksrType").toLowerCase();
        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }
        if (isSessionExpired(request)) {
            return;
        }
        if (value != null) {//create mode
            if (varKeep.equals("session") || varKeep.equals("both")) {
                request.getSession().setAttribute(name, value);
            }
            if (varKeep.equals("cookie") || varKeep.equals("both")) {
                try {
                    Cookie c = new Cookie(name, URLEncoder.encode(value, "utf-8"));
                    int expSec = TextKit.parseInt(Tarn.sysDevConf("varExpires"), -1);
                    if (expSec == 0) {
                        expSec = -1;
                    }
                    c.setMaxAge(expSec);
                    c.setPath(getRequest().getContextPath() + "/");
                    getResponse().addCookie(c);
                } catch (UnsupportedEncodingException e) {
                    //URLEncoder中的编码为utf-8肯定支持，实际不会抛出异常
                    return;
                }
            }
        } else {
            if (varKeep.equals("session") || varKeep.equals("both")) {

                if (name.indexOf(",") == -1) {
                    request.getSession().removeAttribute(name);
                } else {
                    String na[] = name.replaceAll(" ", "").split(",");
                    for (String aNa : na) {
                        request.getSession().removeAttribute(aNa);
                    }
                }
            }
            if (varKeep.equals("cookie") || varKeep.equals("both")) {
                Cookie c[] = request.getCookies();
                if (c == null) {
                    return;
                }
                for (Cookie aC : c) {
                    if (name.indexOf(aC.getName()) != -1) {
                        aC.setValue(null);
                        aC.setMaxAge(0);
                        aC.setPath(getRequest().getContextPath() + "/");
                        getResponse().addCookie(aC);
                    }
                }
            }
        }
        return;
    }

    /**
     * 从会话中删除所有session信息
     */
    public PageContext removeAllKsr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return this;
        }
        if (isSessionExpired(request)) {
            return this;
        }
        Enumeration enumeration = request.getSession().getAttributeNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            ksr(key, null);
        }
        return this;
    }


    static final String HTTP_REQUEST = "HTTP_REQUEST";
    static final String HTTP_RESPONSE = "HTTP_RESPONSE";
    static final String HTTP_MULTIPART_REQUEST = "HTTP_MULTIPART_REQUEST";
    private Map<String, Object> pageContext = new java.util.HashMap<String, Object>();
    private HttpServletResponse response;
    private HttpServletRequest request;
    private MultipartRequest muRequest;

    class UploadRequestProxy extends HttpServletRequestWrapper {

        private UploadSize us;

        public UploadRequestProxy(HttpServletRequest request, UploadSize observer) {
            super(request);
            this.us = observer;
        }

        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream in = super.getInputStream();
            return new UploadStreamProxy(in, us);
        }

        class UploadStreamProxy extends ServletInputStream {
            private UploadSize us;
            private ServletInputStream in;

            public UploadStreamProxy(ServletInputStream in, UploadSize us) {
                this.in = in;
                this.us = us;
            }

            @Override
            public int read() throws IOException {
                return 0;
            }

            public int read(byte[] b, int off, int len) throws IOException {
                int r = in.read(b, off, len);
                if (r != -1) {
                    us.setUploadSize(r);
                }
                return r;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        }

    }

    class UploadSize {
        int x = 0;

        public void setUploadSize(int r) {
            x += r;
        }
    }
}
