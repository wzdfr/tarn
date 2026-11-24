package com.dfrdev.tarn.core.web;


import com.dfrdev.tarn.aop.interceptor.RequestFactory;
import com.dfrdev.tarn.aop.interceptor.RequestInterceptor;
import com.dfrdev.tarn.collection.RunMode;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import com.dfrdev.tarn.core.connection.ConnectionInit;
import com.dfrdev.tarn.core.connection.ConnectionKit;
import com.dfrdev.tarn.route.RouteDispatcher;
import com.dfrdev.tarn.route.RouteEntry;
import com.dfrdev.tarn.route.Router;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.text.TextKit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.core.boot.PluginInit;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.boot.TarnConfig;
/**
 * tarn
 */
public class TarnDispatcherFilter implements Filter {
    private static String controllerType = "";
    private static RequestInterceptor REQUEST_ = new RequestFactory();
    //扩展的Filter
    private static FilterInterface filterInterface = null;
    private TarnConfig tarnConfig;
    private static boolean isExtendFilter = false;
    private static String[] interceptSuffixs = null;
    /**
     * 是否输出安全响应头（配置 securityHeaders=false 可关闭）
     */
    private static boolean securityHeadersEnabled = true;
    /**
     * 自定义 Content-Security-Policy，未配置时不输出
     */
    private static String contentSecurityPolicy = null;

    /**
     * Initial
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        String runMode = ConnectionKit.getDevConf("runMode");
        if (StringUtil.isEmpty(runMode) || !runMode.equalsIgnoreCase("jar")) {
            RunMode.getInstance().updateJarStatus(false);
        } else if (runMode.equalsIgnoreCase("jar")) {
            RunMode.getInstance().updateJarStatus(true);
        }
        createTarnConfig(filterConfig.getInitParameter("configClass"));

        controllerType = normalizeControllerType(ConnectionKit.getDevConf("controller-type"));
        isExtendFilter = Boolean.parseBoolean(ConnectionKit.getDevConf("ExtendFilter"));
        String suffixConf = ConnectionKit.getDevConf("InterceptSuffix");
        // 缺省（如 Spring Boot 场景未提供 dev.properties）时按“不限制扩展名”处理
        interceptSuffixs = StringUtil.isEmpty(suffixConf) ? new String[0] : suffixConf.split("\\|");
        String securityHeaders = ConnectionKit.getDevConf("securityHeaders");
        securityHeadersEnabled = StringUtil.isEmpty(securityHeaders) || Boolean.parseBoolean(securityHeaders);
        contentSecurityPolicy = ConnectionKit.getDevConf("contentSecurityPolicy");
        LogUtil.info("ruixing framework.Tarn Successfully Start jdk1.7");
        Config.configTarn(tarnConfig, filterConfig);
        LogUtil.info("jar run mode "+RunMode.getInstance().isJarStatus());
    }

    /**
     * 归一化 controller-type。
     * <p>{@code param}（{@code ?m=} 反射调用）与 {@code method}（方法名直调）已废弃，
     * 存在任意 public 方法被调用的安全风险，统一降级为 {@code auto}。
     */
    static String normalizeControllerType(String type) {
        if (StringUtil.isEmpty(type)) {
            return "";
        }
        String normalized = type.trim();
        if ("param".equalsIgnoreCase(normalized) || "method".equalsIgnoreCase(normalized)) {
            LogUtil.warn("[Tarn] controller-type=" + normalized
                    + " 已废弃（?m= 反射调用存在安全风险），已降级为 auto");
            return "auto";
        }
        return normalized;
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        try {
            req.setCharacterEncoding(Config.getGlobalCharset());
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("[Tarn] 不支持的字符编码: " + Config.getGlobalCharset(), e);
        }
        res.setCharacterEncoding(Config.getGlobalCharset());
        HttpServletRequest request = (HttpServletRequest) req;
        applySecurityHeaders(request, res);
        Map<String, Object> mp = new HashMap<String, Object>();
        mp.put(PageContext.HTTP_REQUEST, request);
        mp.put(PageContext.HTTP_RESPONSE, res);
        PageContext pc = new PageContext(mp);
        Tarn.setPageContext(pc);

        boolean next = ExpandFilterFactory.doFilter(request, res, filterChain);
        if (!next) {
            return;
        }
        String vpath = request.getRequestURI().substring(request.getContextPath().length());
        if (!shouldIntercept(vpath)) {
            filterChain.doFilter(request, res);
            return;
        }
        // Tarn 2.0+：静态路由表 O(1) 优先；未命中（动态路径/路径变量/regex）回退老拦截器链
        RouteEntry route = Router.get().lookup(vpath, request.getMethod());
        try {
            if (route != null) {
                RouteDispatcher.dispatch(route, request, res, filterChain);
            } else {
                REQUEST_.doFilter(request, res, filterChain, controllerType, request, vpath);
            }
        } catch (CastorNotFoundException e) {
            LogUtil.error("[Tarn] 请求分发失败 vpath=" + vpath, e);
            throw new ServletException(e);
        } finally {
            closeRequestResources();
            // 清理 ThreadLocal 绑定的请求上下文，防止内存泄漏
            Tarn.clearPageContext();
            Controller.clearTarnHolder();
        }
    }

    /**
     * 输出基础安全响应头（S10）：防 MIME 嗅探、点击劫持、Referrer 泄露，HTTPS 下启用 HSTS。
     * 通过 securityHeaders=false 关闭，contentSecurityPolicy 配置自定义 CSP。
     */
    static void applySecurityHeaders(HttpServletRequest request, ServletResponse res) {
        if (!securityHeadersEnabled || !(res instanceof HttpServletResponse)) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (contentSecurityPolicy != null && contentSecurityPolicy.trim().length() > 0) {
            response.setHeader("Content-Security-Policy", contentSecurityPolicy.trim());
        }
        if (request != null && request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    /**
     * 释放请求级资源：当前线程绑定的 MyBatis 会话。
     * 未引入 mybatis 时静默跳过（框架其余功能不受影响）。
     */
    private static void closeRequestResources() {
        try {
            com.dfrdev.tarn.plugin.mybatis.MybatisKit.close();
        } catch (Throwable ignore) {
            // mybatis 未在 classpath，忽略
        }
    }

    /**
     * 请求路径是否应进入 MVC 调度（无扩展名 或 扩展名在 InterceptSuffix 白名单内）
     */
    private boolean shouldIntercept(String vpath) {
        int dotIndex = vpath.lastIndexOf(".");
        if (dotIndex == -1) {
            return true;
        }
        String ext = vpath.substring(dotIndex + 1).toLowerCase();
        return TextKit.containsIgnoreStr(interceptSuffixs, ext);
    }

    private void createTarnConfig(String configClass) {
        if (configClass == null) {
            LogUtil.info("Please set configClass parameter of TarnDispatcherFilter in web.xml");
        }
        if (StringUtil.isNotEmpty(configClass)) {
            Object temp = null;
            try {
                temp = Class.forName(configClass).newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Can not create instance of class: " + configClass, e);
            }

            if (temp instanceof TarnConfig)
                tarnConfig = (TarnConfig) temp;
            else
                throw new RuntimeException("Can not create instance of class: " + configClass + ". Please check the config in web.xml");
        }

    }


    public void destroy() {
        ConnectionInit.destroy();
        LogUtil.info("ruixing framework.Tarn Successfully destroy");
        PluginInit.stopPlugins(Config.getPlugins().getPluginList());
        ExpandFilterFactory.destroy();
        if (isExtendFilter) {
            if (filterInterface != null) {
                filterInterface.destroy();
            }
        }
    }


}
