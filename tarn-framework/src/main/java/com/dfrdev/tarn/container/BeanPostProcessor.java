package com.dfrdev.tarn.container;

/**
 * Bean 后置处理器（对标 Spring 的 {@code BeanPostProcessor}）。
 * <p>
 * 实现类注册到容器后，会在每个 Bean 的初始化方法前后被调用。
 * 返回 {@code null} 表示不替换原实例。
 *
 * @author dfr
 * @since 2.2
 */
public interface BeanPostProcessor {

    /**
     * 初始化方法（{@code @EPostConstruct} / {@code afterPropertiesSet}）执行前
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 初始化方法执行后
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
