package com.dfrdev.tarn.container;

/**
 * 容器关闭时执行销毁逻辑（对标 Spring 的 {@code DisposableBean}）。
 *
 * @author dfr
 * @since 2.2
 */
public interface DisposableBean {

    void destroy() throws Exception;
}
