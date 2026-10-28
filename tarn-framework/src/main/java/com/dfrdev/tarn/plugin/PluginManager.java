package com.dfrdev.tarn.plugin;

import com.dfrdev.tarn.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 统一插件管理器（Tarn 2.0+）。
 * <p>
 * 职责：
 * <ul>
 *     <li>新插件注册与按 order 排序启动/停止</li>
 *     <li>兼容旧版 {@link IPlugin}（通过 {@link PluginAdapter} 自动适配）</li>
 *     <li>按类型查找插件实例</li>
 * </ul>
 * 线程安全：注册列表使用 CopyOnWriteArrayList。
 *
 * @author dfr
 * @since 2.0
 */
public final class PluginManager {

    private static final PluginManager INSTANCE = new PluginManager();

    private final List<Plugin> plugins = new CopyOnWriteArrayList<Plugin>();

    private volatile boolean started = false;

    private PluginManager() {
    }

    public static PluginManager me() {
        return INSTANCE;
    }

    /**
     * 获取插件管理器单例（推荐命名）。
     *
     * @return 插件管理器实例
     * @since 1.1
     */
    public static PluginManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册新插件
     */
    public PluginManager register(Plugin plugin) {
        if (plugin != null) {
            plugins.add(plugin);
        }
        return this;
    }

    /**
     * 注册旧版插件（自动适配，存量项目零改动）
     */
    public PluginManager register(IPlugin legacyPlugin) {
        if (legacyPlugin != null) {
            plugins.add(new PluginAdapter(legacyPlugin));
        }
        return this;
    }

    /**
     * 按优先级启动所有插件（order 越小越先启动）
     *
     * @return 成功启动的插件数量
     */
    public synchronized int startAll() {
        if (started) {
            return plugins.size();
        }
        List<Plugin> sorted = new ArrayList<Plugin>(plugins);
        Collections.sort(sorted, new Comparator<Plugin>() {
            public int compare(Plugin a, Plugin b) {
                return Integer.compare(a.order(), b.order());
            }
        });

        int count = 0;
        for (Plugin plugin : sorted) {
            try {
                plugin.onStart();
                count++;
                LogUtil.info("Plugin started: " + plugin.name());
            } catch (Exception e) {
                String msg = "Plugin start error: " + plugin.name() + " (" + plugin.getClass().getName() + ")";
                LogUtil.error(msg + " -> " + e.getMessage());
                throw new RuntimeException(msg, e);
            }
        }
        started = true;
        return count;
    }

    /**
     * 逆序停止所有插件
     */
    public synchronized void stopAll() {
        List<Plugin> sorted = new ArrayList<Plugin>(plugins);
        Collections.sort(sorted, new Comparator<Plugin>() {
            public int compare(Plugin a, Plugin b) {
                return Integer.compare(b.order(), a.order());
            }
        });
        for (Plugin plugin : sorted) {
            try {
                plugin.onStop();
            } catch (Exception e) {
                LogUtil.error("Plugin stop error: " + plugin.name() + " -> " + e.getMessage());
            }
        }
        started = false;
    }

    /**
     * 按类型查找第一个匹配的插件
     */
    @SuppressWarnings("unchecked")
    public <T extends Plugin> T find(Class<T> type) {
        for (Plugin plugin : plugins) {
            if (type.isInstance(plugin)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public boolean isStarted() {
        return started;
    }
}
