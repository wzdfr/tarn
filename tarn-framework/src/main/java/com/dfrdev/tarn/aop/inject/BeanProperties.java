package com.dfrdev.tarn.aop.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JavaBean 属性缓存（Tarn 2.1）。
 * <p>
 * 按类缓存 {@link BeanProperty} 列表，避免每个请求重复反射扫描字段与 setter。
 * {@link #of(Class)} 只收集当前类声明的字段（旧行为）；
 * Tarn 2.2 的依赖注入使用 {@link #ofIncludingInherited(Class)}，
 * 会沿继承链收集父类字段（子类优先，同名父类字段被忽略）。
 *
 * @author dfr
 * @since 2.1
 */
public final class BeanProperties {

    private static final ConcurrentMap<Class<?>, List<BeanProperty>> CACHE =
            new ConcurrentHashMap<Class<?>, List<BeanProperty>>();
    private static final ConcurrentMap<Class<?>, List<BeanProperty>> INHERITED_CACHE =
            new ConcurrentHashMap<Class<?>, List<BeanProperty>>();

    private BeanProperties() {
    }

    /**
     * 获取类声明的可注入属性（结果只读、已缓存）
     */
    public static List<BeanProperty> of(Class<?> clazz) {
        return build(clazz, CACHE, false);
    }

    /**
     * 获取类及父类声明的可注入属性（子类优先，结果只读、已缓存）
     */
    public static List<BeanProperty> ofIncludingInherited(Class<?> clazz) {
        return build(clazz, INHERITED_CACHE, true);
    }

    private static List<BeanProperty> build(Class<?> clazz,
                                            ConcurrentMap<Class<?>, List<BeanProperty>> cache,
                                            boolean includeInherited) {
        List<BeanProperty> cached = cache.get(clazz);
        if (cached != null) {
            return cached;
        }
        Map<String, BeanProperty> byName = new LinkedHashMap<String, BeanProperty>();
        if (includeInherited) {
            List<Class<?>> hierarchy = new ArrayList<Class<?>>();
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                hierarchy.add(0, current);
                current = current.getSuperclass();
            }
            // 子类优先：从最子类向根部遍历
            for (int i = hierarchy.size() - 1; i >= 0; i--) {
                collect(clazz, hierarchy.get(i), byName);
            }
        } else {
            collect(clazz, clazz, byName);
        }
        List<BeanProperty> result = Collections.unmodifiableList(
                new ArrayList<BeanProperty>(byName.values()));
        cache.put(clazz, result);
        return result;
    }

    private static void collect(Class<?> runtimeClass, Class<?> declaringClass,
                                Map<String, BeanProperty> byName) {
        for (Field field : declaringClass.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())
                    || byName.containsKey(field.getName())) {
                continue;
            }
            String suffix = BeanProperty.capitalize(field.getName());
            Method setter = publicMethod(runtimeClass, "set" + suffix, field.getType());
            Method getter = publicMethod(runtimeClass, "get" + suffix);
            if (getter == null) {
                // boolean 字段的读取方法可能是 isXxx
                getter = publicMethod(runtimeClass, "is" + suffix);
            }
            byName.put(field.getName(), new BeanProperty(field, setter, getter));
        }
    }

    /**
     * 按名称查找属性（先精确匹配，再忽略大小写）
     */
    public static BeanProperty find(Class<?> clazz, String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        for (BeanProperty property : of(clazz)) {
            if (property.getName().equals(fieldName)) {
                return property;
            }
        }
        for (BeanProperty property : of(clazz)) {
            if (property.getName().equalsIgnoreCase(fieldName)) {
                return property;
            }
        }
        return null;
    }

    private static Method publicMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 清空缓存（配置热重载场景使用）
     */
    public static void clear() {
        CACHE.clear();
        INHERITED_CACHE.clear();
    }
}
