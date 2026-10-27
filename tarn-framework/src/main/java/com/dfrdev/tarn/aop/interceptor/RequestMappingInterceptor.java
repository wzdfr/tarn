package com.dfrdev.tarn.aop.interceptor;


import com.dfrdev.tarn.aop.*;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import org.apache.log4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Mapping的方式映射的控制器<br/>
 * ERequestMapping
 */
public class RequestMappingInterceptor implements Serializable {
    private static final long serialVersionUID = 6392376146163510046L;
    private static List<ControllerInfo> list = Inject.getControllers();
    private static Logger logger = Logger.getLogger(ParamInterceptor.class);

    RequestMappingInterceptor() {

    }

    RequestMappingInterceptor(ServletRequest request, ServletResponse response, FilterChain filterChain, HttpServletRequest req, String vpath) throws ServletException, IOException, CastorNotFoundException {
        boolean ninter = false;
        if (list != null) {
            String methodType = req.getMethod();
            for (ControllerInfo pi : list) {
                ControllerMappingInfo cm = RequestMappingUtils.installMethodMapping(pi, vpath, methodType);
                String methodName = cm.getMethodName();
                if (!cm.getMethodName().equals("")) {
                    Object o = null;
                    try {
                        o = Class.forName(pi.getValue()).newInstance();
                        ninter = true;
                        ServiceInject.inject(o);
                        //完成属性注入
                        AttrInject.handle(req, o);
                        //调用名称为：execute() 的方法
                        Method execute = null;
                        try {
                            execute = o.getClass().getMethod("execute");
                        } catch (NoSuchMethodException e) {
                            execute = null;
                        }
                        if (execute != null) {
                            ExecuteInject.handle(execute, req, response, o, filterChain, null, null, pi);
                        }

                        Method m = null;
                        if (methodName != null && !methodName.equals("")) {
                            try {
                                m = o.getClass().getMethod(methodName);
                            } catch (NoSuchMethodException e) {
                                m = null;
                            }
                            if (m != null) {
                                if (m.isAnnotationPresent(ERequestMapping.class)) {
                                    if (m.getAnnotation(ERequestMapping.class).method() != RequestMethod.AUTO &&
                                            !methodType.equals(m.getAnnotation(ERequestMapping.class).method().toString())) {
                                        ninter = false;
                                        break;
                                    }
                                }
                                ExecuteInject.handle(m, req, response, o, filterChain, null, null,pi);
                                return;
                            }
                        }

                        if (execute == null && m == null) {
                            filterChain.doFilter(request, response);
                            return;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!ninter) {
                filterChain.doFilter(request, response);
            }
        }
    }
}
