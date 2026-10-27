package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.annotation.ECookieValue;
import com.dfrdev.tarn.aop.annotation.EPathVariable;
import com.dfrdev.tarn.aop.annotation.ERequestBody;
import com.dfrdev.tarn.aop.annotation.ERequestHeader;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.interceptor.PathRuleUtils;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import com.dfrdev.tarn.core.castor.CastorService;
import com.dfrdev.tarn.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 控制器方法参数解析器（Tarn 2.1 从 {@code ExecuteInject} 拆分）。
 * <p>
 * 职责单一：按参数注解（{@code @ERequestParam} / {@code @EPathVariable} / {@code @ERequestBody} /
 * {@code @ERequestHeader} / {@code @ECookieValue}）把请求数据转换为方法实参；
 * 以及读取请求体文本。{@code ExecuteInject} 只负责编排执行与渲染。
 *
 * @author dfr
 * @since 2.1
 */
public final class MethodParamResolver {

    private MethodParamResolver() {
    }

    /**
     * 解析方法实参
     *
     * @param types      方法参数类型
     * @param methodInfo 方法元信息（参数注解、参数类型）
     * @param mapping    映射信息（路径模板 + 实际路径，路径变量解析依赖）
     * @param request    当前请求
     */
    public static Object[] resolve(Class<?>[] types, ControllerMethodInfo methodInfo,
                                   ControllerMappingInfo mapping, HttpServletRequest request)
            throws CastorNotFoundException, IOException {
        Object values[] = new Object[types.length];
        Object methodParam[] = methodInfo.getMethodParam();
        Map pathVars = null;
        String mappingName = mapping == null ? null : mapping.getMappingName();
        int pathVarCount = countPathVars(mappingName);
        if (pathVarCount > 0) {
            // 变量个数 = { 出现次数；老实现用 split 长度会多算一次
            pathVars = new PathRuleUtils(mappingName, mapping.getPath()).conver(pathVarCount);
        }
        for (int i = 0; i < types.length; i++) {
            if (methodParam == null || i >= methodParam.length || methodParam[i] == null) {
                values[i] = null;
                continue;
            }
            MethodParam mp = (MethodParam) methodParam[i];
            Annotation annotation = mp.getAnnotation();
            if (annotation == null) {
                values[i] = null;
                continue;
            }
            if (ERequestParam.class == annotation.annotationType()) {
                ERequestParam param = (ERequestParam) annotation;
                values[i] = CastorService.toType(request.getParameter(param.value()), types[i]);
            } else if (EPathVariable.class == annotation.annotationType()) {
                if (pathVars != null) {
                    EPathVariable variable = (EPathVariable) annotation;
                    values[i] = CastorService.toType(pathVars.get(variable.value()), types[i]);
                }
            } else if (ERequestBody.class == annotation.annotationType()) {
                values[i] = CastorService.toType(request, types[i]);
            } else if (ERequestHeader.class == annotation.annotationType()) {
                ERequestHeader header = (ERequestHeader) annotation;
                values[i] = CastorService.toType(request.getHeader(header.value()), types[i]);
            } else if (ECookieValue.class == annotation.annotationType()) {
                ECookieValue cookie = (ECookieValue) annotation;
                values[i] = CastorService.toType(request.getSession().getAttribute(cookie.value()), types[i]);
            } else {
                values[i] = null;
            }
        }
        return values;
    }

    /**
     * 读取请求体文本。
     * <p>
     * 注意：不关闭 reader —— 关闭后同请求内再次 getReader() 在部分 Tomcat 版本会抛
     * {@code Stream closed}，老实现已因该问题移除 close()。
     */
    public static String readData(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader();
            String line = reader.readLine();
            if (line == null) {
                return "";
            }
            StringBuilder data = new StringBuilder(line);
            while ((line = reader.readLine()) != null) {
                data.append('\n').append(line);
            }
            return data.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 统计路径模板中 {var} 占位符个数（兼容 ${mapping} 写法）
     */
    private static int countPathVars(String mappingName) {
        if (StringUtil.isEmpty(mappingName)) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < mappingName.length(); i++) {
            if (mappingName.charAt(i) == '{') {
                count++;
            }
        }
        return count;
    }
}
