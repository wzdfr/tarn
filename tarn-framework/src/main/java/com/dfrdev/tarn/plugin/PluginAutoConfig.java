package com.dfrdev.tarn.plugin;

import com.dfrdev.tarn.config.YamlConfigKit;
import com.dfrdev.tarn.plugin.mybatis.MybatisPlugin;
import com.dfrdev.tarn.plugin.redis.RedisPlugin;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;

import java.util.List;

/**
 * 声明式插件装配（Tarn 2.0+）。
 * <p>
 * 在 {@code Config.configTarn} 中、{@link PluginManager#startAll()} 之前调用，
 * 依据 application.yml 自动注册内置插件与自定义插件：
 * <ul>
 *     <li>{@code tarn.redis.enabled=true} → 注册 {@link RedisPlugin}</li>
 *     <li>{@code tarn.mybatis.enabled=true} → 注册 {@link MybatisPlugin}</li>
 *     <li>{@code tarn.plugins: [类全名...]} → 反射实例化并注册（兼容 Plugin 与老 IPlugin）</li>
 * </ul>
 * 未启用 YAML（无 application.yml）时不做任何事，老项目行为不变。
 *
 * @author dfr
 * @since 2.0
 */
public final class PluginAutoConfig {

    private static volatile boolean configured = false;

    private PluginAutoConfig() {
    }

    /**
     * 读取 YAML 配置并注册插件（幂等，重复调用安全）
     */
    public static synchronized void configure() {
        if (configured || !YamlConfigKit.isYamlEnabled()) {
            return;
        }
        configured = true;

        // 内置插件：Redis
        if (YamlConfigKit.getBool(RedisPlugin.CONF_PREFIX + ".enabled", false)) {
            PluginManager.me().register(new RedisPlugin());
            LogUtil.info("[PluginAutoConfig] 装配 RedisPlugin (tarn.redis.enabled=true)");
        }

        // 内置插件：MyBatis（老 IPlugin 接口，自动适配）
        if (YamlConfigKit.getBool("tarn.mybatis.enabled", false)) {
            String configLocation = YamlConfigKit.get("tarn.mybatis.config-location", "");
            if (StringUtil.isNotEmpty(configLocation) && configLocation.startsWith("classpath:")) {
                configLocation = configLocation.substring("classpath:".length());
            }
            PluginManager.me().register(new MybatisPlugin(
                    StringUtil.isEmpty(configLocation) ? null : configLocation));
            LogUtil.info("[PluginAutoConfig] 装配 MybatisPlugin (tarn.mybatis.enabled=true)");
        }

        // 自定义插件列表（未配置 tarn.plugins 时为 null）
        List<String> pluginClasses = YamlConfigKit.getList("tarn.plugins");
        if (pluginClasses != null) {
            for (String className : pluginClasses) {
                registerByClassName(className);
            }
        }
    }

    private static void registerByClassName(String className) {
        if (StringUtil.isEmpty(className)) {
            return;
        }
        try {
            Object instance = Class.forName(className.trim()).newInstance();
            if (instance instanceof Plugin) {
                PluginManager.me().register((Plugin) instance);
                LogUtil.info("[PluginAutoConfig] 装配插件 " + className);
            } else if (instance instanceof IPlugin) {
                PluginManager.me().register((IPlugin) instance);
                LogUtil.info("[PluginAutoConfig] 装配老插件(IPlugin) " + className);
            } else {
                LogUtil.error("[PluginAutoConfig] 类未实现 Plugin/IPlugin 接口，已跳过: " + className);
            }
        } catch (Exception e) {
            LogUtil.error("[PluginAutoConfig] 插件实例化失败: " + className, e);
        }
    }
}
