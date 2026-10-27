package com.dfrdev.tarn.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 配置加载器（Tarn 2.0+，Spring Boot 风格）。
 * <p>
 * 加载 classpath 下的 {@code application.yml}，将嵌套结构扁平化为点号 key
 * （如 {@code tarn.datasource.type}），并自动映射到老框架的 key
 * （如 {@code DBType}、{@code controller-type}）——老代码 {@code ConnectionKit.getDevConf()}
 * 无需修改即可读取 YAML 值。
 * <p>
 * 取值优先级：application.yml &gt; dev.properties。
 *
 * @author dfr
 * @since 2.0
 */
public final class YamlConfigKit {

    private static final String DEFAULT_CONFIG = "application.yml";

    /**
     * 扁平化后的 YAML 配置（点号 key），未加载到配置文件时为空 Map
     */
    private static final Map<String, Object> FLAT = new HashMap<String, Object>();

    /**
     * YAML 新 key → 老框架 key 的映射表
     */
    private static final Map<String, String> LEGACY_KEY_MAP = new HashMap<String, String>();

    static {
        LEGACY_KEY_MAP.put("tarn.datasource.type", "DBType");
        LEGACY_KEY_MAP.put("tarn.datasource.pool-type", "connectionType");
        LEGACY_KEY_MAP.put("tarn.datasource.config-file", "connection-config-file");
        LEGACY_KEY_MAP.put("tarn.datasource.db-version", "dbTypeVersion");
        LEGACY_KEY_MAP.put("tarn.datasource.url", "jdbcUrl");
        LEGACY_KEY_MAP.put("tarn.datasource.username", "jdbcUser");
        LEGACY_KEY_MAP.put("tarn.datasource.password", "jdbcPassword");
        LEGACY_KEY_MAP.put("tarn.datasource.driver-class-name", "jdbcDriverClass");
        LEGACY_KEY_MAP.put("tarn.mvc.controller-type", "controller-type");
        LEGACY_KEY_MAP.put("tarn.mvc.intercept-suffix", "InterceptSuffix");
        LEGACY_KEY_MAP.put("tarn.mvc.template-base-directory", "templateBaseDirectory");
        LEGACY_KEY_MAP.put("tarn.mvc.default-view-type", "defaultViewType");
        LEGACY_KEY_MAP.put("tarn.mvc.dev-mode", "devMode");
        // FreeMarker 相关（2.1 补齐：老项目迁移 yml 后缺失导致启动失败/缓存不可配）
        LEGACY_KEY_MAP.put("tarn.mvc.freemarker.template-update-delay", "DEFAULT_FREEMARKER_TEMPLATE_UPDATE_DELAY");
        LEGACY_KEY_MAP.put("tarn.mvc.freemarker.string-template-cache", "freemarker_stringLoadTemplate_cache");
        LEGACY_KEY_MAP.put("tarn.mvc.freemarker.multi-dir", "isFreeMarkerMultiDir");
        // 扩展 Filter（2.1 补齐）
        LEGACY_KEY_MAP.put("tarn.mvc.extend-filter.enabled", "ExtendFilter");
        LEGACY_KEY_MAP.put("tarn.mvc.extend-filter.impl", "ExtendFilterImpl");
        // 注意：真实读取 key 为 isDebug / isSaveSQLLOGDB（DB.java 静态字段），
        // 旧映射值 isBug / isSavaSQLexecLog 与实际不符，导致 yml 的 debug 配置不生效，2.1 修正
        LEGACY_KEY_MAP.put("tarn.system.debug", "isDebug");
        LEGACY_KEY_MAP.put("tarn.system.save-sql-log", "isSaveSQLLOGDB");
        LEGACY_KEY_MAP.put("tarn.system.encoding", "encoding");
        LEGACY_KEY_MAP.put("tarn.system.run-mode", "runMode");
        LEGACY_KEY_MAP.put("tarn.session.store-type", "ksrType");
        LEGACY_KEY_MAP.put("tarn.session.expires", "varExpires");
        LEGACY_KEY_MAP.put("tarn.ehcache.enabled", "OpenEhcache");
        loadDefault();
    }

    private YamlConfigKit() {
    }

    private static synchronized void loadDefault() {
        if (!FLAT.isEmpty()) {
            return;
        }
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_CONFIG);
        if (in == null) {
            in = YamlConfigKit.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG);
        }
        if (in == null) {
            return; // 无 YAML 配置，纯老模式运行
        }
        try {
            Yaml yaml = new Yaml();
            Object root = yaml.load(in);
            if (root instanceof Map) {
                flatten("", (Map<String, Object>) root, FLAT);
            }
        } catch (Exception e) {
            System.err.println("[Tarn] application.yml 解析失败: " + e.getMessage());
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> map, Map<String, Object> out) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey())
                    : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value, out);
            } else {
                out.put(key, value);
            }
        }
    }

    /**
     * 配置项是否存在（用于区分“未配置”与“配置为空值”）
     */
    public static boolean containsKey(String key) {
        return FLAT.containsKey(key);
    }

    /**
     * 按新 YAML key 取值，如 {@code get("tarn.redis.host")}
     */
    public static String get(String key) {
        Object value = FLAT.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /**
     * 取列表配置（如 tarn.plugins）
     */
    @SuppressWarnings("unchecked")
    public static List<String> getList(String key) {
        Object value = FLAT.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }

    /**
     * 按老框架 key 取值（自动从新 YAML key 映射）。
     * 例如老代码 {@code ConnectionKit.getDevConf("DBType")} 的 YAML 等价查询。
     *
     * @return YAML 中对应值；无则返回 null（调用方应回退 properties）
     */
    public static String getByLegacyKey(String legacyKey) {
        for (Map.Entry<String, String> entry : LEGACY_KEY_MAP.entrySet()) {
            if (entry.getValue().equals(legacyKey)) {
                return get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * 配置是否存在 YAML 文件且非空
     */
    public static boolean isYamlEnabled() {
        return !FLAT.isEmpty();
    }

    /**
     * 全部扁平化配置（只读视图，供诊断）
     */
    public static Map<String, Object> all() {
        return new HashMap<String, Object>(FLAT);
    }
}
