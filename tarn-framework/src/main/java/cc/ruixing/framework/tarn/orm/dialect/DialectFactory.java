package cc.ruixing.framework.tarn.orm.dialect;

import cc.ruixing.framework.tarn.core.connection.ConnectionKit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 方言工厂（Tarn 2.0+）。
 * <p>
 * 解析顺序：
 * <ol>
 *     <li>配置 {@code tarn.datasource.type} / 老 key {@code DBType}（MYSQL/ORACLE/MSSQL/POSTGRESQL/DM/KINGBASE/GAUSSDB/VASTBASE/ORACLE12C）</li>
 *     <li>未配置时尝试按 JDBC URL 自动识别（jdbc:mysql://... 等）</li>
 *     <li>兜底 MySQL 方言</li>
 * </ol>
 *
 * @author dfr
 * @since 2.0
 */
public final class DialectFactory {

    private static final Map<String, Dialect> DIALECTS = new HashMap<String, Dialect>();

    static {
        register(new MySQLDialect());
        register(new PostgreSQLDialect());
        register(new OracleDialect());
        register(new Oracle12cDialect());
        register(new SqlServerDialect());
        register(new DMDialect());
        register(new KingbaseDialect());
        register(new GaussDBDialect());
        register(new VastbaseDialect());
    }

    private DialectFactory() {
    }

    private static void register(Dialect dialect) {
        DIALECTS.put(dialect.name().toUpperCase(Locale.ROOT), dialect);
    }

    /**
     * 注册自定义方言（扩展点）
     */
    public static void registerDialect(Dialect dialect) {
        if (dialect != null) {
            register(dialect);
        }
    }

    /**
     * 按名称取方言（大小写不敏感），未知名称回退自动识别
     */
    public static Dialect of(String name) {
        if (name != null) {
            Dialect dialect = DIALECTS.get(name.trim().toUpperCase(Locale.ROOT));
            if (dialect != null) {
                return dialect;
            }
        }
        return detect();
    }

    /**
     * 当前配置对应的方言（读 tarn.datasource.type / 老 key DBType）
     */
    public static Dialect current() {
        String type = ConnectionKit.getDevConf("DBType");
        if (type != null && !type.trim().isEmpty()) {
            Dialect dialect = DIALECTS.get(type.trim().toUpperCase(Locale.ROOT));
            if (dialect != null) {
                return dialect;
            }
        }
        return detect();
    }

    /**
     * 按 JDBC URL 自动识别数据库类型
     */
    public static Dialect detect() {
        String url = ConnectionKit.getDevConf("jdbcUrl");
        if (url == null) {
            url = ConnectionKit.getDevConf("url");
        }
        if (url != null) {
            String u = url.toLowerCase(Locale.ROOT);
            if (u.startsWith("jdbc:mysql") || u.startsWith("jdbc:mariadb")) return DIALECTS.get("MYSQL");
            if (u.startsWith("jdbc:postgresql")) return DIALECTS.get("POSTGRESQL");
            if (u.startsWith("jdbc:oracle")) return DIALECTS.get("ORACLE");
            if (u.startsWith("jdbc:sqlserver") || u.startsWith("jdbc:jtds")) return DIALECTS.get("MSSQL");
            if (u.startsWith("jdbc:dm")) return DIALECTS.get("DM");
            if (u.startsWith("jdbc:kingbase")) return DIALECTS.get("KINGBASE");
            if (u.startsWith("jdbc:opengauss") || u.startsWith("jdbc:gaussdb")) return DIALECTS.get("GAUSSDB");
            if (u.startsWith("jdbc:vastbase")) return DIALECTS.get("VASTBASE");
        }
        return DIALECTS.get("MYSQL");
    }

    /**
     * 分页便捷入口：对 sql 应用当前方言分页
     */
    public static String paged(String sql, int offset, int limit) {
        return current().getPagedSql(sql, offset, limit);
    }
}
