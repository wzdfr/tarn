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

        controllerType = ConnectionKit.getDevConf("controller-type");
        isExtendFilter = Boolean.parseBoolean(ConnectionKit.getDevConf("ExtendFilter"));
        String suffixConf = ConnectionKit.getDevConf("InterceptSuffix");
        // 缺省（如 Spring Boot 场景未提供 dev.properties）时按“不限制扩展名”处理
        interceptSuffixs = StringUtil.isEmpty(suffixConf) ? new String[0] : suffixConf.split("\\|");
        LogUtil.info("ruixing framework.Tarn Successfully Start jdk1.7");
        Config.configTarn(tarnConfig, filterConfig);
        LogUtil.info("jar run mode "+RunMode.getInstance().isJarStatus());
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        try {
            req.setCharacterEncoding(Config.getGlobalCharset());
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("[Tarn] 不支持的字符编码: " + Config.getGlobalCharset(), e);
        }
        res.setCharacterEncoding(Config.getGlobalCharset());
        HttpServletRequest request = (HttpServletRequest) req;
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
