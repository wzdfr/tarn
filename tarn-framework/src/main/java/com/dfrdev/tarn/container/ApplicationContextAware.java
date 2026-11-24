package com.dfrdev.tarn.container;

/**
 * 需要持有容器的组件可实现此接口（对标 Spring 的 {@code ApplicationContextAware}）。
 *
 * @author dfr
 * @since 2.2
 */
public interface ApplicationContextAware {

    void setApplicationContext(ApplicationContext applicationContext);
}
