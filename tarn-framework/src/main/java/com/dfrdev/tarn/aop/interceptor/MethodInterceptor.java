package com.dfrdev.tarn.aop.interceptor;


import com.dfrdev.tarn.aop.AttrInject;
import com.dfrdev.tarn.aop.ExecuteInject;
import com.dfrdev.tarn.aop.Inject;
import com.dfrdev.tarn.aop.ServiceInject;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.aop.ControllerInfo;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直接方法的控制器
 */
public class MethodInterceptor implements Serializable {
    private static final long serialVersionUID = 6392376146163510046L;
    private static List<ControllerInfo> list = Inject.getControllers();
    private static Logger logger = Logger.getLogger(MethodInterceptor.class);

    MethodInterceptor() {

    }

    MethodInterceptor(ServletRequest request, ServletResponse response, FilterChain filterChain, HttpServletRequest req, String vpath) throws ServletException, IOException, CastorNotFoundException {
        boolean ninter = false;
        if (list != null) {
            String methodType = req.getMethod();
            for (ControllerInfo pi : list) {
                String k = pi.getKey();
                if (k.indexOf("${method}") != -1) {
                    String reg = k.replace("${method}", "[a-zA-Z0-9_]+");
                    Matcher matcher = Pattern.compile(reg).matcher(vpath);
                    if (matcher.find()) {
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
                                ExecuteInject.executeMethodHandle(execute, req, response, o, filterChain, null, null, pi);
                            }
                            Method m = null;
                            String monthod = vpath.replace(k.substring(0, k.indexOf("$")), "").replace(k.substring(k.indexOf("d}") + 2), "");
                            if (monthod != null && !monthod.equals("")) {
                                try {
                                    m = o.getClass().getMethod(monthod);
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
            }
        }
        if (!ninter) {
            filterChain.doFilter(request, response);
        }
    }
}
