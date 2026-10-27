package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.inject.BeanProperties;
import com.dfrdev.tarn.aop.inject.BeanProperty;
import com.dfrdev.tarn.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * 请求参数 → 目标对象属性注入（Tarn 2.1 重构）。
 * <p>
 * 支持两种形式：
 * <ul>
 *   <li>普通参数：{@code title=xxx} → {@code target.setTitle("xxx")}</li>
 *   <li>级联参数：{@code user.role.id=1} → 逐级创建子对象并注入</li>
 * </ul>
 * 重构点：级联路径由原先「递归 + 字符串 substring 定位」改为<b>按下标遍历路径段</b>，
 * 不再依赖 {@code fieldRule.indexOf(field)}（同名段重复时老实现会截错位置）；
 * 属性访问统一走 {@link BeanProperty}。
 *
 * @author dfr
 */
public class AttrInject {

    /**
     * 完成类字段的 setAttribute
     *
     * @param request 当前请求参数
     * @param target  需要注入的 Object 对象
     */
    public static void handle(HttpServletRequest request, Object target) throws IllegalAccessException,
            InstantiationException, NoSuchFieldException, NoSuchMethodException,
            java.lang.reflect.InvocationTargetException, java.io.IOException {
        if (request == null || target == null) {
            return;
        }
        HashMap<String, Object> createdCache = new HashMap<String, Object>();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String fieldName = (String) paramNames.nextElement();
            String parameterValue = request.getParameter(fieldName);
            if (fieldName.indexOf(".") != -1) {
                invokeRecursive(target, fieldName, parameterValue, createdCache);
            } else if (StringUtil.isNotEmpty(parameterValue)) {
                ActionInvocation.invokeSet(target, fieldName, parameterValue);
            }
        }
    }

    /**
     * 递归设置级联对象属性。如：{@code user.role.id} 会设置用户对象中角色对象的 id。
     *
     * @param target            目标对象
     * @param fieldRule         级联路径（点号分隔）
     * @param value             参数值
     * @param aopRecursiveField 本次请求内已创建的子对象缓存（键为字段名，与老实现一致）
     * @return 目标对象
     */
    public static Object invokeRecursive(Object target, String fieldRule, Object value,
                                         HashMap<String, Object> aopRecursiveField) {
        if (target == null || value == null || StringUtil.isEmpty(fieldRule)) {
            return target;
        }
        if (aopRecursiveField == null) {
            aopRecursiveField = new HashMap<String, Object>();
        }
        String[] segments = fieldRule.split("\\.");
        Object current = target;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (StringUtil.isEmpty(segment)) {
                continue;
            }
            if (isLastSegment(segments, i)) {
                ActionInvocation.invokeSet(current, segment, value);
                break;
            }
            BeanProperty property = BeanProperties.find(current.getClass(), segment);
            if (property == null || !property.canWrite()) {
                // 路径不存在时静默放弃（与老实现一致），避免一次无效参数引发请求失败
                return target;
            }
            Object child = aopRecursiveField.get(segment);
            if (child == null) {
                child = property.newChildValue(current);
                if (child == null) {
                    return target;
                }
                property.set(current, child);
                aopRecursiveField.put(segment, child);
            }
            current = child;
        }
        return target;
    }

    /**
     * 当前段之后是否还存在有效路径段
     */
    private static boolean isLastSegment(String[] segments, int index) {
        for (int i = index + 1; i < segments.length; i++) {
            if (StringUtil.isNotEmpty(segments[i].trim())) {
                return false;
            }
        }
        return true;
    }
}
