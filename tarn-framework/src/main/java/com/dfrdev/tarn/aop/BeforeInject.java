package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.annotation.Before;
import com.dfrdev.tarn.aop.inject.BeanProperties;
import com.dfrdev.tarn.aop.inject.BeanProperty;
import com.dfrdev.tarn.core.web.Controller;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.LogUtil;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@code @Before} 拦截器装配 + 控制器字段回填 request attribute（Tarn 2.1 重构）。
 * <p>
 * 重构点：
 * <ul>
 *   <li>拦截器缓存由实例字段改为<b>静态并发缓存</b>——原实现每次请求 {@code new BeforeInject()}，
 *       缓存随实例创建而失效，等于每次请求都重新反射实例化拦截器</li>
 *   <li>字段回填统一走 {@link BeanProperties}，无 getter 的字段直接跳过（不再逐字段打印异常）</li>
 * </ul>
 *
 * @author dfr
 */
public class BeforeInject {

    private static final Interceptor[] NULL_INTERCEPTOR_ARRAY = new Interceptor[0];

    /**
     * 拦截器单例缓存（按拦截器类型）
     */
    private static final ConcurrentMap<Class<? extends Interceptor>, Interceptor> INTERCEPTORS =
            new ConcurrentHashMap<Class<? extends Interceptor>, Interceptor>();

    /**
     * Build interceptors of Controller
     */
    static Interceptor[] buildControllerInterceptors(Class<? extends Controller> controllerClass) {
        Before before = controllerClass.getAnnotation(Before.class);
        return before != null ? createInterceptors(before) : NULL_INTERCEPTOR_ARRAY;
    }

    /**
     * Build interceptors of Method
     */
    static Interceptor[] buildMethodInterceptors(Method method) {
        Before before = method.getAnnotation(Before.class);
        return before != null ? createInterceptors(before) : NULL_INTERCEPTOR_ARRAY;
    }

    /**
     * Create interceptors with Annotation of Before. Singleton version.
     */
    private static Interceptor[] createInterceptors(Before beforeAnnotation) {
        Class<? extends Interceptor>[] interceptorClasses = beforeAnnotation.value();
        if (interceptorClasses == null || interceptorClasses.length == 0) {
            return NULL_INTERCEPTOR_ARRAY;
        }
        Interceptor[] result = new Interceptor[interceptorClasses.length];
        for (int i = 0; i < interceptorClasses.length; i++) {
            result[i] = interceptorsOf(interceptorClasses[i]);
        }
        return result;
    }

    private static Interceptor interceptorsOf(Class<? extends Interceptor> interceptorClass) {
        Interceptor cached = INTERCEPTORS.get(interceptorClass);
        if (cached != null) {
            return cached;
        }
        try {
            Interceptor created = interceptorClass.newInstance();
            Interceptor exists = INTERCEPTORS.putIfAbsent(interceptorClass, created);
            return exists != null ? exists : created;
        } catch (Exception e) {
            throw new TarnException("[Tarn] 拦截器实例化失败: " + interceptorClass.getName(), e);
        }
    }

    /**
     * 把控制器字段值回填到 request attribute（视图渲染使用）。
     *
     * @param o       需要回填的对象
     * @param request 当前请求
     * @throws java.io.IOException    保留老 API 的异常声明（实现不会抛出，兼容既有调用方的 catch）
     * @throws javax.servlet.ServletException 同上
     */
    public static void handle(Object o, HttpServletRequest request)
            throws java.io.IOException, javax.servlet.ServletException {
        if (o == null) {
            return;
        }
        for (BeanProperty property : BeanProperties.of(o.getClass())) {
            if (!property.canRead()) {
                continue;
            }
            Object value = property.get(o);
            if (value != null) {
                request.setAttribute(property.getName(), value);
            }
        }
    }

    /**
     * 已缓存的拦截器数量（诊断用）
     */
    public static int cachedInterceptorCount() {
        return INTERCEPTORS.size();
    }

    /**
     * 清空拦截器缓存（配置热重载场景使用）
     */
    public static void clearCache() {
        INTERCEPTORS.clear();
        LogUtil.info("[Tarn] @Before 拦截器缓存已清空");
    }
}
