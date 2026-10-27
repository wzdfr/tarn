package com.dfrdev.tarn.aop.interceptor;

import com.dfrdev.tarn.core.castor.CastorNotFoundException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求控制器接口
 * */
public interface RequestInterceptor {
    public  void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain, String impl, HttpServletRequest req, String vpath) throws ServletException, IOException, CastorNotFoundException;

}
