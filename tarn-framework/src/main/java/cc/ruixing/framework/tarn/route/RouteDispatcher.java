package cc.ruixing.framework.tarn.route;

import cc.ruixing.framework.tarn.aop.AttrInject;
import cc.ruixing.framework.tarn.aop.ControllerInfo;
import cc.ruixing.framework.tarn.aop.ControllerMappingInfo;
import cc.ruixing.framework.tarn.aop.ExecuteInject;
import cc.ruixing.framework.tarn.aop.ServiceInject;
import cc.ruixing.framework.tarn.core.castor.CastorNotFoundException;
import cc.ruixing.framework.tarn.util.LogUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 路由分发器（Tarn 2.0+）：执行 {@link Router} 命中的路由。
 * <p>
 * 执行流程与老 {@code RequestMappingInterceptor} 完全一致，保证行为兼容：
 * <ol>
 *     <li>实例化控制器</li>
 *     <li>{@link ServiceInject#inject(Object)} 完成 Service 依赖注入</li>
 *     <li>{@link AttrInject#handle(HttpServletRequest, Object)} 完成请求属性注入</li>
 *     <li>存在 {@code execute()} 则先执行</li>
 *     <li>路由条目带方法名则执行映射方法（携带参数元数据，支持
 *     {@code @ERequestParam} / {@code @ERequestBody} / {@code @ERequestHeader}）</li>
 *     <li>两者皆无则放行 {@code filterChain.doFilter}（与老逻辑一致）</li>
 * </ol>
 *
 * @author dfr
 * @since 2.0
 */
public final class RouteDispatcher {

    private RouteDispatcher() {
    }

    /**
     * 分发已命中的路由。本方法完成整个请求处理（含放行兜底），调用方无需再处理。
     *
     * @param entry       路由条目
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤链（兜底放行用）
     */
    public static void dispatch(RouteEntry entry, HttpServletRequest request, ServletResponse response,
                                FilterChain filterChain) throws IOException, ServletException {
        ControllerInfo ci = entry.getControllerInfo();
        try {
            Object target = Class.forName(entry.getControllerClass()).newInstance();
            ServiceInject.inject(target);
            AttrInject.handle(request, target);

            // 1. 默认 execute()
            Method execute = null;
            try {
                execute = target.getClass().getMethod("execute");
            } catch (NoSuchMethodException e) {
                execute = null;
            }
            if (execute != null) {
                ExecuteInject.handle(execute, request, response, target, filterChain, null, null, ci);
            }

            // 2. 映射方法（带参数元数据，使方法参数注入可用）
            Method mapped = null;
            String methodName = entry.getMethodName();
            if (methodName != null && !methodName.isEmpty()) {
                Class<?>[] paramTypes = entry.getMethodInfo() != null
                        ? entry.getMethodInfo().getParamTypes() : null;
                try {
                    if (paramTypes != null && paramTypes.length > 0) {
                        // 带参方法必须按参数类型精确查找，否则 NoSuchMethodException 会被误判为未命中
                        mapped = target.getClass().getMethod(methodName, paramTypes);
                    } else {
                        mapped = target.getClass().getMethod(methodName);
                    }
                } catch (NoSuchMethodException e) {
                    mapped = null;
                }
                if (mapped != null) {
                    ExecuteInject.handle(mapped, request, response, target, filterChain,
                            buildMappingInfo(entry, request), entry.getMethodInfo(), ci);
                    return;
                }
            }

            // 3. 两者皆无 → 放行（与老拦截器链一致）
            if (execute == null && mapped == null) {
                filterChain.doFilter(request, response);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | NoSuchFieldException | NoSuchMethodException e) {
            LogUtil.error("[RouteDispatcher] 控制器装载失败: " + entry, e);
            throw new ServletException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException() != null ? e.getTargetException() : e;
            LogUtil.error("[RouteDispatcher] 控制器方法执行异常: " + entry, cause);
            throw new ServletException(cause);
        } catch (CastorNotFoundException e) {
            LogUtil.error("[RouteDispatcher] 参数转换器缺失: " + entry, e);
            throw new ServletException(e);
        }
    }

    /**
     * 构造 ControllerMappingInfo：参数解析器（路径变量）依赖其 mappingName 与 path。
     * <p>mappingName 使用<b>完整路径模板</b>（如 {@code /api/demo/delete/{id}}），
     * path 使用实际请求路径，二者前缀一致，{@code PathRuleUtils} 才能正确截取变量值。
     */
    private static ControllerMappingInfo buildMappingInfo(RouteEntry entry, HttpServletRequest request) {
        ControllerMappingInfo mapping = new ControllerMappingInfo();
        mapping.setRequestMapping(entry.getControllerInfo().getKey());
        String template = entry.getPathTemplate();
        if (template == null) {
            template = entry.getMappingValue() != null
                    ? entry.getMappingValue() : entry.getControllerInfo().getKey();
        }
        mapping.setMappingName(template);
        mapping.setMethodName(entry.getMethodName());
        String uri = request.getRequestURI();
        mapping.setPath(uri.substring(request.getContextPath().length()));
        return mapping;
    }
}
