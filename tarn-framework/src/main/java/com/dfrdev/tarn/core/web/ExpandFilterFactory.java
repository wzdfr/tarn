package com.dfrdev.tarn.core.web;

import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.collection.RunMode;
import com.dfrdev.tarn.util.xml.XMLElement;
import com.dfrdev.tarn.util.xml.XMLMultiLoader;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.ArrayList;
import java.util.List;
import com.dfrdev.tarn.util.LogUtil;

import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.core.boot.ConfigLoader;
/**
 * ExpandFilterFactory.
 */
public class ExpandFilterFactory {

    static {

        XMLMultiLoader xmlMultiLoader = new XMLMultiLoader();
        if (RunMode.getInstance().isJarStatus()) {
            xmlMultiLoader.load(ConfigLoader.class.getClassLoader().getResourceAsStream("expand-filter.xml"));
        } else {
            String path = ConfigLoader.class.getResource("/").getPath() + "expand-filter.xml";
            xmlMultiLoader.load(path);
        }

        List<XMLElement> list = xmlMultiLoader.elements("filters.expand-impl");
        for (XMLElement xml : list) {
            Mapx mapx = xml.getAttributes();
            String packageClass = mapx.getString("class");
            try {
                Config.getFilters().add((FilterInterface) Class.forName(packageClass).newInstance());
            } catch (ClassNotFoundException e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            } catch (InstantiationException e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            }
        }

    }


    /**
     * 初始化，在服务启动时值执行一次
     *
     * @param filterConfig
     */
    public static void init(FilterConfig filterConfig) {
        if (Config.getFilters() != null && Config.getFilters().getFilterList().size() > 0) {
            for (FilterInterface filter : Config.getFilters().getFilterList()) {
                filter.init(filterConfig);
            }
        }

    }

    /**
     * @param request     ServletRequest
     * @param response    ServletResponse对象
     * @param filterChain FilterChain对象
     * @return boolean 是否继续往下执行，返回true，是，false，不往下执行
     */
    public static boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        boolean next = true;
        if (Config.getFilters() != null && Config.getFilters().getFilterList().size() > 0) {
            for (FilterInterface filter : Config.getFilters().getFilterList()) {
                next = filter.doFilter(request, response, filterChain);
                if (!next) {
                    break;
                }
            }
        }
        return next;
    }

    /**
     * 销毁
     */
    public static void destroy() {
        if (Config.getFilters() != null && Config.getFilters().getFilterList().size() > 0) {
            for (FilterInterface filter : Config.getFilters().getFilterList()) {
                filter.destroy();
            }
        }
    }

}
