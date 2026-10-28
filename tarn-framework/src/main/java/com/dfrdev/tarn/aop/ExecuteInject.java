package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.annotation.Before;
import com.dfrdev.tarn.aop.annotation.EResponseType;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.core.web.Controller;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import com.dfrdev.tarn.json.JsonResult;
import com.dfrdev.tarn.render.JsonRender;
import com.dfrdev.tarn.render.RenderFactory;
import com.dfrdev.tarn.util.text.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * 控制器方法执行与视图/JSON 渲染（Tarn 2.1 重构）。
 * <p>
 * 重构点：
 * <ul>
 *   <li>参数解析、请求体读取拆到 {@link MethodParamResolver}，本类只做编排</li>
 *   <li>{@code executeMethodHandle} / {@code handle} 的控制器预处理去重（{@link #prepare}）</li>
 *   <li>{@code @Before} 拦截器改由 {@link BeforeInject} 的静态缓存获取</li>
 * </ul>
 *
 * @author dfr
 */
public class ExecuteInject {
    // render below ---
    private static final RenderFactory renderFactory = RenderFactory.getInstance();

    /**
     * 执行控制器方法（含 {@code @Before} 拦截器），不支持返回 void 之外的特殊处理
     */
    public static void executeMethodHandle(Method execute, HttpServletRequest request, ServletResponse response,
                                           Object target, FilterChain filterChain,
                                           ControllerMappingInfo controllerMapping,
                                           ControllerMethodInfo controllerMethodInfo,
                                           ControllerInfo propertiesInfo) throws IllegalAccessException,
            InvocationTargetException, CastorNotFoundException, IOException, ServletException {
        if (target != null) {
            doHandle(execute, request, response, prepare(execute, target, propertiesInfo), filterChain,
                    controllerMapping, controllerMethodInfo, propertiesInfo);
        }
    }

    /**
     * 读取请求体文本（兼容老 API，实现已迁移到 {@link MethodParamResolver}）
     */
    public static String readData(HttpServletRequest request) {
        return MethodParamResolver.readData(request);
    }

    /**
     * 执行控制器方法：先跑 {@code @Before} 拦截器，再执行方法并按返回类型渲染
     */
    public static void handle(Method execute, HttpServletRequest request, ServletResponse response, Object target,
                              FilterChain filterChain, ControllerMappingInfo controllerMapping,
                              ControllerMethodInfo controllerMethodInfo, ControllerInfo propertiesInfo)
            throws IllegalAccessException, InvocationTargetException, CastorNotFoundException,
            IOException, ServletException {
        if (execute == null) {
            return;
        }
        target = prepare(execute, target, propertiesInfo);
        if (execute.isAnnotationPresent(Before.class)) {
            ActionInfo actionInfo = new ActionInfo((Controller) target, propertiesInfo,
                    controllerMapping, controllerMethodInfo);
            for (Interceptor interceptor : BeforeInject.buildMethodInterceptors(execute)) {
                interceptor.intercept(actionInfo);
            }
            if (target instanceof Controller) {
                Controller controller = (Controller) target;
                controller.attr("INVALID_ERROR_STATUS", controller.INVALID_ERROR_STATUS);
                if (controller.INVALID_ERROR_STATUS) {
                    return;
                }
            }
        }
        doHandle(execute, request, response, target, filterChain, controllerMapping, controllerMethodInfo, propertiesInfo);
    }

    /**
     * 控制器前置处理：回填视图根路径与控制器元信息
     */
    private static Object prepare(Method execute, Object target, ControllerInfo propertiesInfo) {
        if (execute == null || !(target instanceof Controller) || propertiesInfo == null) {
            return target;
        }
        Controller controller = (Controller) target;
        controller.setViewRootPath(propertiesInfo.getViewRootPath());
        controller.setControllerInfo(propertiesInfo);
        return controller;
    }

    /**
     * 执行方法并按返回类型选择输出方式
     */
    private static void doHandle(Method execute, HttpServletRequest request, ServletResponse response,
                                 Object target, FilterChain filterChain, ControllerMappingInfo controllerMapping,
                                 ControllerMethodInfo controllerMethodInfo, ControllerInfo propertiesInfo)
            throws IllegalAccessException, InvocationTargetException, CastorNotFoundException,
            IOException, ServletException {
        if (execute == null) {
            return;
        }
        // 无返回值：只执行
        if (isMethodReturnVoid(execute)) {
            executeMethod(execute, target, controllerMethodInfo, controllerMapping, request);
            return;
        }
        // Tarn 2.0+: @Json 注解或返回 JsonResult 时，自动 JSON 输出（前后端分离支持）
        if (isJsonResponse(execute)) {
            Object result = executeMethod(execute, target, controllerMethodInfo, controllerMapping, request);
            new JsonRender(result).setContext(request, (HttpServletResponse) response).render();
            return;
        }
        // 返回字符串直接写响应体
        if (isAnnotationResp(execute)) {
            response.getWriter().print(executeMethod(execute, target, controllerMethodInfo, controllerMapping, request));
            return;
        }
        // 否则调转指定页面地址
        String view = (String) executeMethod(execute, target, controllerMethodInfo, controllerMapping, request);
        BeforeInject.handle(target, request);
        if (view == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (propertiesInfo != null && StringUtil.isNotEmpty(propertiesInfo.getViewRootPath())) {
            renderFactory.getRender(view, execute, propertiesInfo.getViewRootPath())
                    .setContext(request, (HttpServletResponse) response).render();
            return;
        }
        renderFactory.getRender(view, execute).setContext(request, (HttpServletResponse) response).render();
    }

    private static boolean isMethodReturnVoid(Method m) {
        return m.getGenericReturnType().toString().equals("void");
    }

    private static boolean isAnnotationResp(Method m) {
        return (m.isAnnotationPresent(EResponseType.class) && !m.getAnnotation(EResponseType.class).value());
    }

    /**
     * Tarn 2.0+: 判断是否为 JSON 响应（@Json 注解或返回 JsonResult 类型）
     */
    private static boolean isJsonResponse(Method m) {
        if (m.isAnnotationPresent(Json.class)) {
            return m.getAnnotation(Json.class).value();
        }
        return JsonResult.class.isAssignableFrom(m.getReturnType());
    }

    private static Object executeMethod(Method execute, Object target, ControllerMethodInfo controllerMethodInfo,
                                        ControllerMappingInfo controllerMapping, HttpServletRequest request)
            throws CastorNotFoundException, InvocationTargetException, IllegalAccessException, IOException {
        if (controllerMethodInfo == null) {
            return execute.invoke(target, (Object[]) null);
        }
        Class<?> types[] = controllerMethodInfo.getParamTypes();
        Object args[] = null;
        if (types.length > 0) {
            args = MethodParamResolver.resolve(types, controllerMethodInfo, controllerMapping, request);
        }
        return execute.invoke(target, args);
    }
}
