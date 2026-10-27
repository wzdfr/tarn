package com.dfrdev.tarn.core;


import com.dfrdev.tarn.aop.BeforeInject;
import com.dfrdev.tarn.aop.ControllerInfo;
import com.dfrdev.tarn.render.RenderContentType;
import com.dfrdev.tarn.render.RenderFactory;
import com.dfrdev.tarn.upload.MultipartRequest;
import com.dfrdev.tarn.upload.UploadFile;
import com.dfrdev.tarn.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * 控制器抽象类，content上下文等常用操作
 */
public abstract class Controller {
    public HttpServletRequest request;
    public HttpServletResponse response;
    /**
     * Tarn 页面上下文对象
     */
    public PageContext context = null;

    public static Tarn tarn = null;
    // render below ---
    private static RenderFactory renderFactory = null;
    private String viewRootPath;
    private ControllerInfo controllerInfo;

    public boolean INVALID_ERROR_STATUS = false;
    public Map invalidErrorkeys;

    public Map getInvalidErrorkeys() {
        return invalidErrorkeys;
    }

    public void setInvalidErrorkeys(Map invalidErrorkeys) {
        this.invalidErrorkeys = invalidErrorkeys;
    }

    /**
     * 获取参数值，转换Int
     *
     * @param key
     */
    public Integer getInt(String key) {
        return toInt(get(key), null);
    }

    /**
     * 转换int类型
     *
     * @param value
     * @param defaultValue 默认值
     */
    private Integer toInt(String value, Integer defaultValue) {
        if (value == null || "".equals(value.trim()))
            return defaultValue;
        if (value.startsWith("N") || value.startsWith("n"))
            return -Integer.parseInt(value.substring(1));
        return Integer.parseInt(value);
    }

    /**
     * 获取参数值，转换Long
     */
    public Long getParaToLong(String name) {
        return toLong(get(name), null);
    }

    private Long toLong(String value, Long defaultValue) {
        if (value == null || "".equals(value.trim()))
            return defaultValue;
        if (value.startsWith("N") || value.startsWith("n"))
            return -Long.parseLong(value.substring(1));
        return Long.parseLong(value);
    }

    /**
     * 获取参数值，转换boolean
     */
    public boolean getBoolean(String name) {
        return Boolean.parseBoolean(get(name));
    }

    /**
     * 获取参数值
     *
     * @param key key名
     */
    public String get(String key) {
        if (StringUtil.isEmpty(key)) return "";
        initResponsep();
        return context.get(key);
    }

    /**
     * 获取参数值
     *
     * @param key    key名
     * @param encode 编码
     */
    public String get(String key, String encode) {
        if (StringUtil.isEmpty(key)) return "";
        initResponsep();
        return context.get(key, encode);
    }

    /**
     * 获取Cookie值
     *
     * @param name Cookie
     */
    public String getCookie(String name) {
        if (StringUtil.isEmpty(name)) return null;
        initResponsep();
        return context.cookie(name);
    }

    /**
     * 设置 Cookie值
     *
     * @param name  Cookie
     * @param value Cookie value
     */
    public boolean setCookie(String name, String value) {
        initResponsep();
        context.cookie(name, value);
        return true;
    }

    /**
     * 获取参数Enumeration集合
     */
    public Enumeration<String> getParaNames() {
        initRequest();
        return request.getParameterNames();
    }


    /**
     * 获取参数Map集合
     */
    public Map<String, String[]> getParaMap() {
        initRequest();
        return request.getParameterMap();
    }

    /**
     * 从request删除属性
     */
    public Controller removeAttr(String name) {
        initRequest();
        request.removeAttribute(name);
        return this;
    }


    /**
     * 获取会话值
     *
     * @param name 会话名称
     */
    public String getSession(String name) {
        initResponsep();
        return context.ksr(name);
    }

    /**
     * 设置会话，key，value
     */
    public boolean setSession(String name, Object value) {
        initResponsep();
        context.ksr(name, value.toString());
        return true;
    }

    /**
     * 设置request Attribute属性
     *
     * @param key
     * @param value
     */
    public Controller attr(String key, Object value) {
        initRequest();
        request.setAttribute(key, value);
        return this;
    }


    /**
     * 试图渲染
     * ========================================
     * */

    /**
     * FreeMarker 视图渲染
     *
     * @param view 页面路径
     */
    public void renderFreeMarker(String view) {
        initRequest();
        initResponsep();
        initRenderfactory();
        try {
            BeforeInject.handle(this, request);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        renderFactory.getFreeMarkerRender(getView(view)).setContext(request, response).render();
    }

    /**
     * JSP 视图渲染
     *
     * @param view 页面路径
     */
    public void renderJSP(String view) {
        initRequest();
        initResponsep();
        initRenderfactory();
        try {
            BeforeInject.handle(this, request);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        renderFactory.getJspRender(getView(view)).setContext(request, response).render();
    }


    /**
     * Velocity 视图渲染
     *
     * @param view 页面路径
     */
    public void renderVelocity(String view) {
        initRequest();
        initResponsep();
        initRenderfactory();
        try {
            BeforeInject.handle(this, request);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        renderFactory.getVelocityRender(getView(view)).setContext(request, response).render();
    }

    /**
     * Direct
     *
     * @param view 页面路径
     */
    public void renderDirect(String view) {
        initRequest();
        initResponsep();
        initRenderfactory();
        renderFactory.getRedirectRender(view).setContext(request, response).render();
    }

    /**
     * Text
     *
     * @param text 内容
     */
    public void renderText(String text) {
        initRequest();
        initResponsep();
        initRenderfactory();
        renderFactory.getTextRender(text).setContext(request, response).render();
    }

    /**
     * Text
     *
     * @param text        内容
     * @param contentType contentTypel
     */
    public void renderText(String text, RenderContentType contentType) {
        initRequest();
        initResponsep();
        initRenderfactory();
        renderFactory.getTextRender(text, contentType).setContext(request, response).render();
    }

    /**
     * FileRender
     *
     * @param filePath 文件路径
     */
    public void renderFile(String filePath) {
        initRequest();
        initResponsep();
        initRenderfactory();
        renderFactory.getFileRender(filePath).setContext(request, response).render();
    }

    /**
     * FileRender
     *
     * @param file 文件
     */
    public void renderFile(File file) {
        initRequest();
        initResponsep();
        initRenderfactory();
        renderFactory.getFileRender(file).setContext(request, response).render();
    }

    /**
     * 获取HttpServletRequest
     */
    public HttpServletRequest getRequest() {
        initRequest();
        return request;
    }

    /**
     * 获取PageContext
     */
    public PageContext getContext() {
        initResponsep();
        return context;
    }

    /**
     * 获取PageContext  保留old
     */
    public PageContext getcontext() {
        initResponsep();
        return context;
    }

    /**
     * 获取HttpServletResponse
     */
    public HttpServletResponse getResponse() {
        if (response == null) {
            initResponsep();
            response = context.getResponse();
        }
        return response;
    }

    /**
     * Create a token.
     *
     * @param tokenName        the token name used in view
     * @param secondsOfTimeOut the seconds of time out, secondsOfTimeOut >= Const.MIN_SECONDS_OF_TOKEN_TIME_OUT
     */
    public void createToken(String tokenName, int secondsOfTimeOut) {
        com.dfrdev.tarn.token.TokenManager.createToken(this, tokenName, secondsOfTimeOut);
    }

    /**
     * Create a token with default token name and with default seconds of time out.
     */
    public void createToken() {
        createToken(com.dfrdev.tarn.token.TokenManager.DEFAULT_TOKEN_NAME, com.dfrdev.tarn.token.TokenManager.DEFAULT_SECONDS_OF_TOKEN_TIME_OUT);
    }

    /**
     * Create a token with default seconds of time out.
     *
     * @param tokenName the token name used in view
     */
    public void createToken(String tokenName) {
        createToken(tokenName, com.dfrdev.tarn.token.TokenManager.DEFAULT_SECONDS_OF_TOKEN_TIME_OUT);
    }

    /**
     * Check token to prevent resubmit.
     *
     * @param tokenName the token name used in view's form
     * @return true if token is correct
     */
    public boolean validateToken(String tokenName) {
        return com.dfrdev.tarn.token.TokenManager.validateToken(this, tokenName);
    }

    /**
     * Check token to prevent resubmit  with default token key ---> "JFINAL_TOKEN_KEY"
     *
     * @return true if token is correct
     */
    public boolean validateToken() {
        return validateToken(com.dfrdev.tarn.token.TokenManager.DEFAULT_TOKEN_NAME);
    }


    /**
     * Get upload file from multipart request.
     */
    public List<UploadFile> getFiles(String saveDirectory, Integer maxPostSize, String encoding) {
        initRequest();
        if (request instanceof MultipartRequest == false)
            request = new MultipartRequest(request, saveDirectory, maxPostSize, encoding, this);
        return ((MultipartRequest) request).getFiles();
    }

    public UploadFile getFile(String parameterName, String saveDirectory, Integer maxPostSize, String encoding) {
        getFiles(saveDirectory, maxPostSize, encoding);
        return getFile(parameterName);
    }

    public List<UploadFile> getFiles(String saveDirectory, int maxPostSize) {
        initRequest();
        if (request instanceof MultipartRequest == false)
            request = new MultipartRequest(request, saveDirectory, maxPostSize, this);
        return ((MultipartRequest) request).getFiles();
    }

    public UploadFile getFile(String parameterName, String saveDirectory, int maxPostSize) {
        getFiles(saveDirectory, maxPostSize);
        return getFile(parameterName);
    }

    public List<UploadFile> getFiles(String saveDirectory) {
        initRequest();
        if (request instanceof MultipartRequest == false)
            request = new MultipartRequest(request, saveDirectory, this);
        return ((MultipartRequest) request).getFiles();
    }

    public UploadFile getFile(String parameterName, String saveDirectory) {
        getFiles(saveDirectory);
        return getFile(parameterName);
    }

    public List<UploadFile> getFiles() {
        initRequest();
        if (request instanceof MultipartRequest == false)
            request = new MultipartRequest(request, this);
        return ((MultipartRequest) request).getFiles();
    }

    public UploadFile getFile() {
        List<UploadFile> uploadFiles = getFiles();
        return uploadFiles.size() > 0 ? uploadFiles.get(0) : null;
    }

    public UploadFile getFile(String parameterName) {
        List<UploadFile> uploadFiles = getFiles();
        for (UploadFile uploadFile : uploadFiles) {
            if (uploadFile.getParameterName().equals(parameterName)) {
                return uploadFile;
            }
        }
        return null;
    }

    /**
     * 获取View
     */
    public String getView(String view) {
        view = view.trim();
        if (StringUtil.isNotEmpty(viewRootPath)) {
            viewRootPath = viewRootPath.trim();
            if (!viewRootPath.substring(viewRootPath.length() - 1).equals("/")) viewRootPath += "/";
            if (view.substring(0, 1).equals("/")) view = view.substring(1, view.length());
            view = viewRootPath + view;
        }
        return view;
    }

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public void setControllerInfo(ControllerInfo controllerInfo) {
        this.controllerInfo = controllerInfo;
    }

    public String getViewRootPath() {
        return viewRootPath;
    }

    public void setViewRootPath(String viewRootPath) {
        this.viewRootPath = viewRootPath;
    }

    /**
     * 获取Tarn
     */
    public Tarn getTarn() {
        initTarn();
        return tarn;
    }

    /**
     * 初始化构造HttpServletRequest
     */
    public void initRequest() {
        if (request == null) {
            initResponsep();
            request = context.getRequest();
        }
    }

    /**
     * 初始化构造HttpServletResponse
     */
    public void initResponsep() {
        if (response == null) {
            initPageContext();
            response = context.getResponse();
        }
    }

    /**
     * 初始化构造PageContext
     */
    public void initPageContext() {
        if (context == null) {
            context = Tarn.getPageContext();
        }
    }

    /**
     * 初始化构造Tarn
     */
    public void initTarn() {
        if (tarn == null) {
            tarn = new Tarn();
        }
    }

    private void initRenderfactory() {
        if (renderFactory == null) {
            renderFactory = RenderFactory.getInstance();
        }
    }
}
