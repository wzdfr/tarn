package cc.ruixing.framework.tarn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一日志门面。
 * <p>
 * Tarn 2.1 起，所有框架内部日志统一走 SLF4J + logback（工程默认 logback-classic 为最终实现）。
 * JAR 包外业务工程可通过 logback.xml / log4j2.xml 切换实现。
 * <p>
 * 本类只做一件事：把外部 30+ 处老调用收敛到一个命名 logger（名称为 {@code "Tarn"}），
 * 让用户可以按 logger 名称精细控制级别与输出位置。
 * <p>
 * 老的 {@code @{Product.Code}}（{@code Config.getAppCode()}）前缀已去掉，
 * 应用标识、TraceId、MDC 上下文等请通过 SLF4J 的 MDC 机制注入。
 */
public class LogUtil {

    /**
     * 所有框架内部日志统一落到的 logger 名称。
     * 用户可在 logback.xml 里针对 Tarn 节点独立配置级别、appender、pattern。
     */
    private static final Logger TARN_LOG = LoggerFactory.getLogger("Tarn");

    /** 业务代码调用，不指定类名时回落到这个 logger。 */
    private static final Logger ROOT_LOG = LoggerFactory.getLogger(LogUtil.class);

    // =========================================================================
    // 对外 API：保持原有签名不变，老项目零改动
    // =========================================================================

    public static void info(Object obj) {
        TARN_LOG.info(String.valueOf(obj));
    }
    public static void info(String message,Object... obj) {
        TARN_LOG.info(message,obj);
    }

    public static void debug(Object obj) {
        if (TARN_LOG.isDebugEnabled()) {
            TARN_LOG.debug(String.valueOf(obj));
        }
    }

    public static void debug(String message,Object... obj) {
        if (TARN_LOG.isDebugEnabled()) {
            TARN_LOG.debug(message,obj);
        }
    }

    public static void warn(Object obj) {
        TARN_LOG.warn(String.valueOf(obj));
    }

    public static void error(Object obj) {
        TARN_LOG.error(String.valueOf(obj));
    }
    public static void error(String message,Object... var2) {
        TARN_LOG.error(message,var2);
    }

    /**
     * 错误日志（带异常堆栈），Tarn 2.0+ 新增。
     * 保持原有签名：obj 是业务描述，t 是异常；t 为 null 时按无堆栈处理。
     */
    public static void error(Object obj, Throwable t) {
        if (t == null) {
            TARN_LOG.error(String.valueOf(obj));
        } else {
            TARN_LOG.error(String.valueOf(obj), t);
        }
    }

    /**
     * fatal 沿用老语义——slf4j 没有 fatal 级别，这里降级为 error 并在消息前加 {@code [FATAL]} 标识。
     */
    public static void fatal(Object obj) {
        TARN_LOG.error("[FATAL] " + obj);
    }

    // =========================================================================
    // 按调用方类名动态 logger（给业务层用，便于按包名分级过滤）
    // =========================================================================

    /**
     * 业务代码显式指定 logger 名称。
     *
     * <pre>{@code
     * LogUtil.with("com.myapp.order").info("订单已创建 id=" + id);
     * }</pre>
     */
    public static Logger with(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * 按调用栈自动推断 logger（跳过 LogUtil 本身）。
     * 性能比 with(name) 稍差，优先用显式名称。
     */
    public static Logger caller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // stack[0] getStackTrace, stack[1] caller, stack[2] 才是真正的业务调用点
        for (int i = 2; i < stack.length; i++) {
            String cn = stack[i].getClassName();
            if (cn.startsWith("cc.ruixing.framework.tarn.util.LogUtil")) continue;
            return LoggerFactory.getLogger(cn);
        }
        return ROOT_LOG;
    }
}
