package cc.ruixing.framework.tarn.core;

import cc.ruixing.framework.tarn.plugin.DbFieldMapping;
import cc.ruixing.framework.tarn.plugin.IDataSourceProvider;
import cc.ruixing.framework.tarn.plugin.IPlugin;
import cc.ruixing.framework.tarn.plugin.ehcache.EhCachePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plugins.
 */
final public class Plugins {

    private final List<IPlugin> pluginList = new ArrayList<IPlugin>();

    public Plugins add(IPlugin plugin) {
        if (plugin != null)
            this.pluginList.add(plugin);
        return this;
    }

    public List<IPlugin> getPluginList() {
        return pluginList;
    }


    /**
     * 根据给定的类型查找服务实例
     *
     * @param clazz 类型
     * @return
     */
    public IPlugin find(Class clazz) {
        for (IPlugin plugin : pluginList) {
            if (clazz.isAssignableFrom(plugin.getClass())) {
                return plugin;
            }
        }
        return null;
    }

    public IDataSourceProvider findDataSource() {
        return (IDataSourceProvider) find(IDataSourceProvider.class);
    }

    public DbFieldMapping findDbFieldMapping() {
        return (DbFieldMapping) find(DbFieldMapping.class);
    }

    public EhCachePlugin findEhcache() {
        return (EhCachePlugin) find(EhCachePlugin.class);
    }
}
