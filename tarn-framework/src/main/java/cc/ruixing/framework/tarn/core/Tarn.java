package cc.ruixing.framework.tarn.core;


import cc.ruixing.framework.tarn.core.connection.ConnectionKit;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Tarn 框架总入口（配置 / 调试 / 请求上下文门面）。
 * <p>
 * <b>职责说明（Tarn 2.1 重构）：</b>老版本中 {@code Tarn extends Q extends DB extends DAOAbstract
 * extends ConnectionFactory}，一个类继承链背下了全部 ORM 与 JDBC 能力。
 * 2.1 起真实 ORM 实现已收敛到组合式引擎 {@link cc.ruixing.framework.tarn.orm.OrmTemplate}，
 * 本类保留 {@code extends Q} 仅为老代码二进制兼容（{@code new Tarn().save(...)} 等调用不受影响），
 * 推荐新代码改用：
 * <ul>
 *     <li>ORM 数据操作：{@code Orm.template()}（组合 + 工厂，零继承）</li>
 *     <li>配置读取：{@link #sysDevConf(String)} / {@link #getConfByFile(String, String)}</li>
 *     <li>请求上下文：{@link #getPageContext()} / {@link #setPageContext(PageContext)}</li>
 *     <li>运行开关：{@link #isDebug()} / {@link #changeDebug(boolean)} / {@link #isSaveSqlLogger()}</li>
 * </ul>
 *
 * @author dfr
 * @see cc.ruixing.framework.tarn.orm.Orm ORM 工厂入口（推荐）
 * @see Q 老 ORM 门面（兼容保留）
 * @since 2.0
 */
public class Tarn extends Q {

    /**
     * 框架版本号，与 Maven 坐标版本保持一致
     */
    public static final String TARN_VERSION = "2.0.0";

    /**
     * 是否处于 jar 运行模式（独立运行 / 可执行 jar）
     */
    public static volatile boolean jarRunMode = false;

    private static final ThreadLocal<PageContext> pageContext = new ThreadLocal<PageContext>();

    /**
     * 当前整个应用级别的数据库执行 SQL 日志是否保存到数据库（SQLEXECLOG 表）。
     *
     * @return true 是，false 不是
     */
    public static boolean isSaveSqlLogger() {
        return isSaveSQLLOGDB;
    }

    /**
     * 改变应用级别数据库执行 SQL 日志是否保存数据库状态。
     * 注意：该开关影响性能，生产环境建议保持关闭。
     *
     * @param status true 开启
     */
    public static void changeSaveSqlLogger(boolean status) {
        isSaveSQLLOGDB = status;
    }

    /**
     * 当前应用是否处于 debug 状态（输出 SQL 与反射信息）
     *
     * @return true 是，false 不是
     */
    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * 改变当前应用 debug 状态
     *
     * @param status true 开启
     */
    public static void changeDebug(boolean status) {
        isDebug = status;
    }

    public static boolean isJarRunMode() {
        return jarRunMode;
    }

    public static void updateRunMode(boolean status) {
        jarRunMode = status;
    }

    /**
     * 框架版本号
     */
    public static String version() {
        return TARN_VERSION;
    }

    // ==================== 配置读取 ====================

    /**
     * 读取主配置属性值（application.yml 优先，回退 dev.properties）。
     *
     * @param key 属性名称
     * @return 属性值；不存在时返回 null
     */
    public static String sysDevConf(String key) {
        return ConnectionKit.getDevConf(key);
    }

    /**
     * 读取主配置属性值，带默认值。
     *
     * @param key          属性名称
     * @param defaultValue 未配置时返回的默认值
     */
    public static String sysDevConf(String key, String defaultValue) {
        String value = ConnectionKit.getDevConf(key);
        return StringUtil.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 获取系统的任意 .properties 文件的内容。该文件必须是在 classpath 根目录。
     * <p>
     * 第一次获取某个文件时会加载并缓存到内存，后续从缓存读取不再访问文件系统。
     *
     * @param key      属性名称
     * @param fileName 文件名称（不需要带后缀名，如 {@code "app"} 代表 app.properties）
     * @return 属性值；文件不存在或 key 不存在时返回 null（老版本文件缺失返回 ""，调用方应以 isEmpty 判空）
     */
    public static String getConfByFile(String key, String fileName) {
        if (key == null || fileName == null) {
            return null;
        }
        Properties props = ConnectionKit.getSysConfs().get(fileName);
        if (props == null) {
            props = loadProperties(fileName);
            if (props == null) {
                LogUtil.warn("[Tarn] 配置文件不存在: " + fileName + ".properties");
                return null;
            }
            ConnectionKit.getSysConfs().put(fileName, props);
        }
        return props.getProperty(key);
    }

    /**
     * {@link #getConfByFile(String, String)} 的带默认值版本
     */
    public static String getConfByFile(String key, String fileName, String defaultValue) {
        String value = getConfByFile(key, fileName);
        return StringUtil.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 读取 int 型配置，解析失败或未配置时返回默认值
     */
    public static int getConfInt(String key, String fileName, int defaultValue) {
        String value = getConfByFile(key, fileName);
        if (StringUtil.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LogUtil.warn("[Tarn] 配置项非 int 值: " + fileName + ".properties#" + key + " = " + value);
            return defaultValue;
        }
    }

    /**
     * 读取 boolean 型配置，解析失败或未配置时返回默认值
     */
    public static boolean getConfBool(String key, String fileName, boolean defaultValue) {
        String value = getConfByFile(key, fileName);
        return StringUtil.isEmpty(value) ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /**
     * {@link #getConfByFile(String, String)} 的简写，自动补 .properties 后缀
     */
    public static String getConf(String key, String fileName) {
        return getConfByFile(key, fileName + ".properties");
    }

    /**
     * 从 classpath 加载 properties 文件；文件不存在返回 null。
     * 使用 classpath 流读取，兼容 IDE 目录与 Spring Boot 可执行 jar 两种运行形态。
     */
    private static Properties loadProperties(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = Tarn.class.getClassLoader();
        }
        InputStream in = loader.getResourceAsStream(fileName);
        if (in == null) {
            return null;
        }
        Properties props = new Properties();
        try {
            try {
                props.load(in);
            } finally {
                in.close();
            }
            return props;
        } catch (IOException e) {
            LogUtil.error("[Tarn] 配置文件加载失败: " + fileName + " -> " + e.getMessage(), e);
            return null;
        }
    }

    // ==================== 请求上下文 ====================

    /**
     * 获取当前线程绑定的请求上下文（框架在每次请求进入时设置）
     */
    public static PageContext getPageContext() {
        return pageContext.get();
    }

    /**
     * 绑定当前线程的请求上下文（框架内部使用；过滤器 finally 中应清理防止线程复用串数据）
     */
    public static void setPageContext(PageContext pc) {
        pageContext.set(pc);
    }

    /**
     * 清理当前线程绑定的请求上下文（线程池复用场景防内存泄漏）
     */
    public static void clearPageContext() {
        pageContext.remove();
    }

    /**
     * 当前请求的完整 URL（含查询串）；不在 Web 请求线程内时返回 null
     */
    protected String pageUrl() {
        PageContext context = getPageContext();
        if (context == null) {
            return null;
        }
        HttpServletRequest req = context.getRequest();
        if (req == null) {
            return null;
        }
        return req.getRequestURI()
                + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
    }
}
