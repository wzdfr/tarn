package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.inject.BeanProperties;
import com.dfrdev.tarn.aop.inject.BeanProperty;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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
     * 可选：按类型登记的可绑定字段白名单（未登记的类型默认按内置安全规则放行）
     */
    private static final Map<Class<?>, Set<String>> BINDABLE_FIELDS =
            new ConcurrentHashMap<Class<?>, Set<String>>();

    /**
     * 框架自有属性：禁止通过请求参数覆盖（S5）
     */
    private static final Set<String> FRAMEWORK_DENIED_FIELDS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("request", "response", "context", "viewRootPath",
                    "controllerInfo", "INVALID_ERROR_STATUS", "invalidErrorkeys", "renderFactory")));

    /**
     * 设置某类型的可绑定字段白名单；登记后仅列表内字段接受请求参数注入。
     */
    public static void setBindableFields(Class<?> type, String... fieldNames) {
        if (type == null) {
            return;
        }
        if (fieldNames == null || fieldNames.length == 0) {
            BINDABLE_FIELDS.remove(type);
            return;
        }
        BINDABLE_FIELDS.put(type, Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(fieldNames))));
    }

    /**
     * 清除某类型的白名单登记。
     */
    public static void clearBindableFields(Class<?> type) {
        if (type != null) {
            BINDABLE_FIELDS.remove(type);
        }
    }

    /**
     * 判断请求参数是否允许注入目标对象的指定属性（S5）。
     * <p>
     * 规则：静态/常量字段一律拒绝；框架自有属性（如 request/response/viewRootPath/controllerInfo，
     * 以及声明在 Controller 基类上的字段）一律拒绝；登记了白名单的类型只允许白名单字段。
     */
    public static boolean isBindable(Object target, String fieldName) {
        if (target == null || StringUtil.isEmpty(fieldName)) {
            return false;
        }
        Set<String> allowed = findBindableFields(target.getClass());
        if (allowed != null && !allowed.contains(fieldName)) {
            LogUtil.debug("[参数注入] 字段不在白名单内，已跳过: " + fieldName);
            return false;
        }
        Field field;
        try {
            field = target.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return false;
        }
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }
        if (FRAMEWORK_DENIED_FIELDS.contains(fieldName)
                || "com.dfrdev.tarn.core.web.Controller".equals(field.getDeclaringClass().getName())) {
            LogUtil.warn("[参数注入] 拒绝覆盖框架属性: " + target.getClass().getName() + "." + fieldName);
            return false;
        }
        return true;
    }

    private static Set<String> findBindableFields(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Set<String> fields = BINDABLE_FIELDS.get(c);
            if (fields != null) {
                return fields;
            }
        }
        return null;
    }

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
                invokeRecursiveChecked(target, fieldName, parameterValue, createdCache);
            } else if (StringUtil.isNotEmpty(parameterValue) && isBindable(target, fieldName)) {
                ActionInvocation.invokeSet(target, fieldName, parameterValue);
            }
        }
    }

    /**
     * 递归设置级联对象属性（请求参数注入用，逐段校验可绑定属性）。
     */
    public static Object invokeRecursiveChecked(Object target, String fieldRule, Object value,
                                                HashMap<String, Object> aopRecursiveField) {
        return invokeRecursive(target, fieldRule, value, aopRecursiveField, true);
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
        return invokeRecursive(target, fieldRule, value, aopRecursiveField, false);
    }

    private static Object invokeRecursive(Object target, String fieldRule, Object value,
                                          HashMap<String, Object> aopRecursiveField, boolean checkBindable) {
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
            if (checkBindable && !isBindable(current, segment)) {
                return target;
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
