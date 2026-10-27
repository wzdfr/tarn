package cc.ruixing.framework.tarn.aop.interceptor;


import cc.ruixing.framework.tarn.core.castor.CastorNotFoundException;

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
        if (impl.equals("method")) {
            new MethodInterceptor(request, response, filterChain, req,vpath);
        } else if (impl.equals("auto")) {
            new AutoInterceptor(request, response, filterChain, req,vpath);
        }  else if (impl.equals("requestMapping")) {
            new RequestMappingInterceptor(request, response, filterChain, req,vpath);
        } else {
            new ParamInterceptor(request, response, filterChain, req,vpath);
        }
    }
}
