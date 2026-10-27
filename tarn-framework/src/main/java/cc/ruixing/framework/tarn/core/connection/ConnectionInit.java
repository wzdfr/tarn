package cc.ruixing.framework.tarn.core.connection;


import cc.ruixing.framework.tarn.collection.RunMode;
import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.core.Tarn;
import cc.ruixing.framework.tarn.plugin.IDataSourceProvider;
import cc.ruixing.framework.tarn.plugin.IPlugin;
import cc.ruixing.framework.tarn.plugin.c3p0.C3p0Plugin;
import cc.ruixing.framework.tarn.plugin.druid.DruidPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 数据库连接初始化
 */
public class ConnectionInit {
    private static IPlugin iPlugin = null;

    public static IDataSourceProvider init(ConnectionType connectionType) {

        Properties props = loadConfigFile();
        // Spring Boot 风格的直连配置优先于连接池配置文件（tarn.datasource.url/username/password/driver-class-name）
        applyDirectConfig(props);

        if (connectionType == ConnectionType.TARN_SYSTEM) {
            TarnDataSource tarnDataSource = new TarnDataSource();
            if (props != null) tarnDataSource.start(props);
            //连接池监控
            new ConnectionManager().start();
            //连接池链接超时监控
            new ConnectionManagerTimeout().start();
            iPlugin = tarnDataSource;
            iPlugin.start();
            Config.getPlugins().add(iPlugin);
            return tarnDataSource;
        }
        if (connectionType == ConnectionType.C3p0) {
            // Tarn 2.0+: c3p0 已淘汰。保留分支仅为老项目二进制兼容，
            // 当 c3p0 jar 不在 classpath 时自动回退 Druid，请尽快迁移（connectionType=druid）
            cc.ruixing.framework.tarn.util.LogUtil.warn(
                    "[Tarn] connectionType=c3p0 已废弃，c3p0 存在已知 CVE 且停止维护，请迁移到 druid");
            try {
                C3p0Plugin c3p0Plugin = null;
                if (props != null) c3p0Plugin = new C3p0Plugin(props);
                iPlugin = c3p0Plugin;
                iPlugin.start();
                Config.getPlugins().add(iPlugin);
                return c3p0Plugin;
            } catch (NoClassDefFoundError e) {
                cc.ruixing.framework.tarn.util.LogUtil.warn(
                        "[Tarn] c3p0 jar 未在 classpath 中，自动回退到 Druid 连接池");
                return init(ConnectionType.Druid);
            }
        }
        if (connectionType == ConnectionType.Druid) {
            DruidPlugin druidPlugin = new DruidPlugin();
            if (props != null) druidPlugin.start(props);
            else iPlugin.start();
            iPlugin = druidPlugin;
            Config.getPlugins().add(druidPlugin);
            return druidPlugin;
        }
        return null;
    }

    /**
     * 加载连接池配置文件（connection-config-file，默认 druid.properties）。
     * 未配置或文件缺失时返回空 Properties，后续可被 YAML 直连配置覆盖。
     */
    private static Properties loadConfigFile() {
        Properties props = new Properties();
        String configFile = ConnectionKit.getDevConf("connection-config-file");
        if (configFile == null || configFile.trim().isEmpty()) {
            return props;
        }
        try {
            InputStream in;
            if (RunMode.getInstance().isJarStatus()) {
                in = ConnectionInit.class.getClassLoader().getResourceAsStream(configFile);
            } else {
                java.net.URL url = ConnectionInit.class.getClassLoader().getResource(configFile);
                if (url == null) {
                    return props;
                }
                File file = new File(url.getPath());
                in = file.exists() ? new FileInputStream(file) : null;
            }
            if (in != null) {
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            }
        } catch (IOException e) {
            cc.ruixing.framework.tarn.util.LogUtil.warn("[Tarn] 连接池配置文件加载失败: " + configFile
                    + " -> " + e.getMessage());
        }
        return props;
    }

    /**
     * 用 application.yml 的直连配置覆盖连接池配置文件中的同名项。
     * 未在 YAML 中出现的 key 保持配置文件原值；配置为空值时覆盖为空值（例如空密码）。
     */
    private static void applyDirectConfig(Properties props) {
        overlay(props, "url", "tarn.datasource.url");
        overlay(props, "username", "tarn.datasource.username");
        overlay(props, "password", "tarn.datasource.password");
        overlay(props, "driverClassName", "tarn.datasource.driver-class-name");
        // 2.1：密码加密开关（tarn.datasource.decrypt），传达 DruidPlugin 由 ConfigTools 解密
        overlay(props, "decrypt", "tarn.datasource.decrypt");
        // 解密公钥（tarn.datasource.public-key，留空则用 Druid 内置默认公钥）
        overlay(props, "public-key", "tarn.datasource.public-key");
    }

    private static void overlay(Properties props, String propKey, String yamlKey) {
        if (cc.ruixing.framework.tarn.config.YamlConfigKit.containsKey(yamlKey)) {
            String value = cc.ruixing.framework.tarn.config.YamlConfigKit.get(yamlKey);
            props.setProperty(propKey, value == null ? "" : value.trim());
        }
    }

    public static void destroy() {
        if (iPlugin != null) iPlugin.stop();
    }
}
