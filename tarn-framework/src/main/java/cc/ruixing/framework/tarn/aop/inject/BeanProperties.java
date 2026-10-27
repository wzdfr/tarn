package cc.ruixing.framework.tarn.aop.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JavaBean 属性缓存（Tarn 2.1）。
 * <p>
 * 按类缓存 {@link BeanProperty} 列表，避免每个请求重复反射扫描字段与 setter。
 * 与老实现一致，只处理<b>当前类声明的实例字段</b>（不含静态字段与继承字段）。
 *
 * @author dfr
 * @since 2.1
 */
public final class BeanProperties {

    private static final ConcurrentMap<Class<?>, List<BeanProperty>> CACHE =
            new ConcurrentHashMap<Class<?>, List<BeanProperty>>();

    private BeanProperties() {
    }

    /**
     * 获取类声明的可注入属性（结果只读、已缓存）
     */
    public static List<BeanProperty> of(Class<?> clazz) {
        List<BeanProperty> cached = CACHE.get(clazz);
        if (cached != null) {
            return cached;
        }
        List<BeanProperty> properties = new ArrayList<BeanProperty>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String suffix = BeanProperty.capitalize(field.getName());
            Method setter = publicMethod(clazz, "set" + suffix, field.getType());
            Method getter = publicMethod(clazz, "get" + suffix);
            if (getter == null) {
                // boolean 字段的读取方法可能是 isXxx
                getter = publicMethod(clazz, "is" + suffix);
            }
            properties.add(new BeanProperty(field, setter, getter));
        }
        List<BeanProperty> result = Collections.unmodifiableList(properties);
        CACHE.put(clazz, result);
        return result;
    }

    /**
     * 按字段名查找属性：精确匹配优先，其次忽略大小写匹配。
     *
     * @return 未找到返回 null
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
    }
}
