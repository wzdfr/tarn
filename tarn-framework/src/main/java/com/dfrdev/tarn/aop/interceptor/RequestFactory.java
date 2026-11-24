package com.dfrdev.tarn.aop.interceptor;


import com.dfrdev.tarn.core.castor.CastorNotFoundException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 简单工厂类，完成控制器的实例化
 */
public class RequestFactory implements RequestInterceptor {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain, String impl, HttpServletRequest req,String vpath) throws ServletException, IOException, CastorNotFoundException {
        if ("auto".equals(impl)) {
            new AutoInterceptor(request, response, filterChain, req, vpath);
        } else {
            // requestMapping / mapping 及未知值统一走基于映射元数据的调度
            new RequestMappingInterceptor(request, response, filterChain, req, vpath);
        }
    }
}
