package com.dfrdev.tarn.aop.inject;

import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.TextKit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * JavaBean 属性元信息：字段 + setter + getter（Tarn 2.1）。
 * <p>
 * 所有注入器（{@code ServiceInject} / {@code RepositoryInject} / {@code TarnInject} /
 * {@code BeforeInject} / {@code AttrInject}）统一通过本类访问属性，避免各自重复实现
 * "反射查找 setXxx/getXxx + 异常吞掉" 的逻辑。
 * <p>
 * 查找规则（保持老框架行为）：仅使用 <b>public</b> 方法，setter 名为
 * {@code set + 首字母大写字段名} 且参数类型必须与字段类型一致。
 *
 * @author dfr
 * @since 2.1
 */
public final class BeanProperty {

    private final Field field;
    private final Method setter;
    private final Method getter;

    BeanProperty(Field field, Method setter, Method getter) {
        this.field = field;
        this.setter = setter;
        this.getter = getter;
        makeAccessible(field);
        makeAccessible(setter);
        makeAccessible(getter);
    }

    private static void makeAccessible(java.lang.reflect.AccessibleObject object) {
        if (object != null) {
            try {
                object.setAccessible(true);
            } catch (SecurityException ignore) {
                // 安全管理器禁止时保持原样，后续调用会给出明确异常
            }
        }
    }

    public String getName() {
        return field.getName();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return field;
    }

    /**
     * 是否存在可写入口（setter 或非 final 字段）
     */
    public boolean canWrite() {
        return setter != null || !java.lang.reflect.Modifier.isFinal(field.getModifiers());
    }

    /**
     * 是否存在可读入口（getter）
     */
    public boolean canRead() {
        return getter != null;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return field.getAnnotation(annotationType);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return field.isAnnotationPresent(annotationType);
    }

    /**
     * 写入属性值：优先调用 setter，其次直接写字段。
     *
     * @return true 写入成功；false 无可用写入口或写入异常（已记录日志）
     */
    public boolean set(Object target, Object value) {
        if (!canWrite()) {
            return false;
        }
        try {
            if (setter != null) {
                setter.invoke(target, value);
            } else {
                field.set(target, value);
            }
            return true;
        } catch (Exception e) {
            LogUtil.error("[Tarn] 属性注入失败 " + target.getClass().getName() + "." + getName()
                    + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 读取属性值（无 getter 时返回 null）
     */
    public Object get(Object target) {
        if (getter == null) {
            return null;
        }
        try {
            return getter.invoke(target);
        } catch (Exception e) {
            LogUtil.error("[Tarn] 属性读取失败 " + target.getClass().getName() + "." + getName()
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 为级联属性创建子对象：优先 getter 返回值，其次反射实例化字段类型。
     *
     * @return 子对象；类型无法实例化时返回 null
     */
    public Object newChildValue(Object owner) {
        Object child = get(owner);
        if (child != null) {
            return child;
        }
        try {
            return getType().newInstance();
        } catch (Exception e) {
            LogUtil.error("[Tarn] 级联属性实例化失败 " + getType().getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * setter 名称（诊断用）
     */
    public String getSetterName() {
        return setter == null ? null : setter.getName();
    }

    String describe() {
        return getType().getSimpleName() + " " + getName();
    }

    @Override
    public String toString() {
        return "BeanProperty{" + describe() + ", setter=" + getSetterName() + "}";
    }

    /**
     * 首字母大写（与字段名拼接 setter/getter）
     */
    static String capitalize(String name) {
        return TextKit.firstLetterToUpper(name);
    }
}
