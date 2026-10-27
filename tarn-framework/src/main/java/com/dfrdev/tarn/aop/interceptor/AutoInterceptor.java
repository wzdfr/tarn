package com.dfrdev.tarn.aop.interceptor;

import com.dfrdev.tarn.aop.*;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import com.dfrdev.tarn.util.StringUtil;
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
 * 自动识别调用
 */
public class AutoInterceptor implements Serializable {
    private static final long serialVersionUID = 6392376146163510046L;
    private static Logger logger = Logger.getLogger(AutoInterceptor.class);
    private static List<ControllerInfo> list = Inject.getControllers();

    AutoInterceptor() {

    }

    AutoInterceptor(ServletRequest request, ServletResponse response, FilterChain filterChain, HttpServletRequest req, String vpath) throws ServletException, IOException, CastorNotFoundException {
        boolean ninter = false;
        if (list != null) {
            String methodType = req.getMethod();

            for (ControllerInfo pi : list) {
                ControllerMappingInfo cm = RequestMappingUtils.installMethod(pi, vpath, methodType);

                String methodName = cm.getMethodName();
                String k = cm.getRequestMapping();

                if (StringUtil.isNotEmpty(methodName) || vpath.equals(k)) {
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
                            ExecuteInject.executeMethodHandle(execute, req, response, o, filterChain, cm, null, pi);
                        }

                        Method m = null;
                        ControllerMethodInfo invokeMethod = null;
                        if (vpath.equals(k)) methodName = req.getParameter("m");
                        if (methodName != null && !methodName.equals("")) {
                            try {
                                List<ControllerMethodInfo> methodInfos = pi.getMethodInfoList();
                                for (ControllerMethodInfo methodInfo : methodInfos) {
                                    if (methodInfo.getName().equals(methodName)) {
                                        if (methodInfo.isParameters()) {
                                            invokeMethod = methodInfo;
                                            m = o.getClass().getMethod(methodName, methodInfo.getParamTypes());
                                            break;
                                        } else {
                                            m = o.getClass().getMethod(methodName);
                                            break;
                                        }
                                    }
                                }
                            } catch (NoSuchMethodException e) {
                                m = null;
                            }
                            if (m != null) {
                                if (m.isAnnotationPresent(ERequestMapping.class)) {
                                    if (m.getAnnotation(ERequestMapping.class).method() != RequestMethod.AUTO
                                            && !methodType.equals(m.getAnnotation(ERequestMapping.class).method().toString())) {
                                        ninter = false;
                                        break;
                                    }
                                }
                                ExecuteInject.handle(m, req, response, o, filterChain, cm, invokeMethod,pi);
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
        if (!ninter) {
            filterChain.doFilter(request, response);
        }
    }


}
