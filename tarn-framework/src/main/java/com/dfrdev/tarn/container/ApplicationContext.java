package com.dfrdev.tarn.container;

import java.util.Map;

/**
 * Tarn 应用上下文（对标 Spring 的 {@code ApplicationContext}）。
 * <p>
 * 负责 Bean 的注册、实例化、依赖注入、生命周期回调与销毁。
 * 容器管理的 Bean（{@code @EService} / {@code @ERepository} / {@code @EComponent}）
 * 会自动完成注入与生命周期处理，调用方无需再手动调用注入方法。
 *
 * @author dfr
 * @since 2.2
 */
public interface ApplicationContext {

    /**
     * 按 Bean 名称获取实例
     *
     * @throws com.dfrdev.tarn.core.TarnException Bean 未注册时抛出
     */
    Object getBean(String name);

    /**
     * 按类型获取唯一实例
     *
     * @throws com.dfrdev.tarn.core.TarnException 没有或存在多个候选时抛出
     */
    <T> T getBean(Class<T> type);

    /**
     * 按名称获取并校验类型
     */
    <T> T getBean(String name, Class<T> type);

    /**
     * 是否注册了该名称的 Bean
     */
    boolean containsBean(String name);

    /**
     * 该 Bean 是否为单例
     */
    boolean isSingleton(String name);

    /**
     * 所有 Bean 定义名称
     */
    String[] getBeanDefinitionNames();

    /**
     * 指定类型的所有 Bean 名称
     */
    String[] getBeanNamesForType(Class<?> type);

    /**
     * 指定类型的所有 Bean 实例（名称 → 实例）
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);

    /**
     * 已注册 Bean 定义数量
     */
    int getBeanDefinitionCount();

    /**
     * 注册 Bean 后置处理器（需在任何单例创建前注册）
     */
    void addBeanPostProcessor(BeanPostProcessor postProcessor);

    /**
     * 关闭容器：按创建顺序逆序执行销毁回调
     */
    void close();
}
