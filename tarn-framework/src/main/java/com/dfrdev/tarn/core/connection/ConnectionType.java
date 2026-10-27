package com.dfrdev.tarn.core.connection;

/**
 * Created by dfr on 14-4-18.
 * ConnectionType
 */
public enum ConnectionType {
    /**
     * Tarn 系统自带实现的简单连接池
     */
    TARN_SYSTEM,
    /**
     * c3p0
     *
     * @deprecated Tarn 2.0 起淘汰 c3p0（已知 CVE、停止维护），请使用 {@link #Druid}。
     * 配置 connectionType=c3p0 时若 c3p0 jar 缺失将自动回退 Druid。
     */
    @Deprecated
    C3p0,
    /**
     * Druid
     * */
    Druid
}
