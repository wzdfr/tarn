package com.dfrdev.tarn.container;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.text.StringUtil;

/**
 * Bean 定义：名称 + 实现类 + 作用域。
 *
 * @author dfr
 * @since 2.2
 */
public final class BeanDefinition {

    /**
     * Bean 作用域（对标 Spring 的 singleton / prototype）
     */
    public enum Scope {
        /** 容器内唯一实例，首次使用时创建并缓存 */
        SINGLETON,
        /** 每次获取都创建新实例 */
        PROTOTYPE
    }

    private final String name;
    private final String className;
    private final Scope scope;
    private volatile Class<?> beanClass;

    public BeanDefinition(String name, String className, Scope scope) {
        this.name = name;
        this.className = className;
        this.scope = scope == null ? Scope.SINGLETON : scope;
    }

    public BeanDefinition(String name, Class<?> beanClass, Scope scope) {
        this(name, beanClass == null ? null : beanClass.getName(), scope);
        this.beanClass = beanClass;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public Scope getScope() {
        return scope;
    }

    public boolean isSingleton() {
        return scope == Scope.SINGLETON;
    }

    public boolean isPrototype() {
        return scope == Scope.PROTOTYPE;
    }

    /**
     * 解析实现类（结果缓存）
     */
    public Class<?> resolveClass() {
        Class<?> resolved = beanClass;
        if (resolved != null) {
            return resolved;
        }
        if (StringUtil.isEmpty(className)) {
            throw new TarnException("Bean '" + name + "' 未声明实现类");
        }
        try {
            resolved = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                resolved = Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new TarnException("Bean '" + name + "' 实现类不存在: " + className, ex);
            }
        }
        beanClass = resolved;
        return resolved;
    }

    @Override
    public String toString() {
        return "BeanDefinition{name='" + name + "', class='" + className + "', scope=" + scope + '}';
    }
}
