package com.dfrdev.tarn.core.boot;


import com.dfrdev.tarn.collection.RunMode;
import com.dfrdev.tarn.plugin.IDataSourceProvider;
import com.dfrdev.tarn.plugin.IPlugin;
import com.dfrdev.tarn.plugin.c3p0.C3p0Plugin;
import com.dfrdev.tarn.plugin.ehcache.EhCachePlugin;
import com.dfrdev.tarn.util.LogUtil;

import javax.servlet.FilterConfig;
import java.util.List;

/**
 * ehcache 插件配置
 */
public class PluginInit {

    /**
     * initial
     */
    protected static void init(FilterConfig filterConfig) {
        String openp = filterConfig.getInitParameter("OpenEhcache");
        if (openp == null) return;
        if (openp.toLowerCase().equals("true")) {
            String file = filterConfig.getInitParameter("EhcacheConfigFile");
            EhCachePlugin ehCachePlugin = null;
            if (file == null)
                ehCachePlugin = new EhCachePlugin();
            else {
                if(RunMode.getInstance().isJarStatus()){
                    ehCachePlugin = new EhCachePlugin(PluginInit.class.getClassLoader().getResourceAsStream(file));
                }else{
                    ehCachePlugin = new EhCachePlugin(PluginInit.class.getResource(file));

                }
            }

            ehCachePlugin.start();
            Config.getPlugins().add(ehCachePlugin);
            LogUtil.info("ehcache缓存插件已开启...");
        }

    }


    /**
     * 启动所有插件
     *
     * @param pluginList
     * @param filterConfig
     */
    protected static void startPlugins(List<IPlugin> pluginList) {
        if (pluginList == null)
            return;

        for (IPlugin plugin : pluginList) {
            try {
                if (plugin instanceof EhCachePlugin || plugin instanceof IDataSourceProvider) {
                    continue;
                }

                if (plugin.start() == false) {
                    String message = "Plugin start error: " + plugin.getClass().getName();
                    LogUtil.error(message);
                    throw new RuntimeException(message);
                }
            } catch (Exception e) {
                String message = "Plugin start error: " + plugin.getClass().getName() + ". \n" + e.getMessage();
                LogUtil.error(message);
                throw new RuntimeException(message, e);
            }
        }
    }

    /**
     * 停止所有插件
     *
     * @param plugins
     * @param filterConfig
     */
    public static void stopPlugins(List<IPlugin> plugins) {
        if (plugins != null) {
            for (int i = plugins.size() - 1; i >= 0; i--) {        // stop plugins
                boolean success = false;
                try {
                    success = plugins.get(i).stop();
                } catch (Exception e) {
                    success = false;
                    LogUtil.error("操作失败: " + e.getMessage(), e);
                }
                if (!success) {
                    System.err.println("Plugin stop error: " + plugins.get(i).getClass().getName());
                }
            }
        }
    }
}
