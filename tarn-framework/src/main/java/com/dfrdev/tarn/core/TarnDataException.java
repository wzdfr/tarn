package com.dfrdev.tarn.core;

/**
 * Tarn 数据访问运行时异常（Tarn 2.1+）。
 * <p>
 * 设计原因：老 API {@code save/update/delete} 的签名是 {@code void}，无法追加 checked 异常，
 * 而原实现把 {@link java.sql.SQLException} 吞掉只打日志，导致“保存/更新失败但调用方无感知”。
 * 该类作为非受检异常统一向上抛出，老调用方即使不 catch 也不会编译失败，但不会再有静默失败。
 *
 * @author dfr
 * @since 2.1
 */
public class TarnDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TarnDataException(String message) {
        super(message);
    }

    public TarnDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
