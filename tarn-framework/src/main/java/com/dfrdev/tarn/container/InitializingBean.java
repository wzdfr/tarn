package com.dfrdev.tarn.container;

/**
 * 属性注入完成后执行初始化逻辑（对标 Spring 的 {@code InitializingBean}）。
 *
 * @author dfr
 * @since 2.2
 */
public interface InitializingBean {

    void afterPropertiesSet() throws Exception;
}
