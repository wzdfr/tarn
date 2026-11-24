package com.dfrdev.tarn.container;

/**
 * 需要获知自身 Bean 名称的组件可实现此接口。
 *
 * @author dfr
 * @since 2.2
 */
public interface BeanNameAware {

    void setBeanName(String name);
}
