package cc.ruixing.framework.tarn.plugin;

/**
 * 旧版 {@link IPlugin} 到新版 {@link Plugin} 的适配器。
 * <p>
 * 保证存量项目无需任何改动即可纳入新版插件生命周期管理（老接口保留，
 * 内部委托新机制 —— 框架统一升级迁移策略）。
 *
 * @author dfr
 * @since 2.0
 */
public class PluginAdapter implements Plugin {

    private final IPlugin legacyPlugin;
    private final String name;

    public PluginAdapter(IPlugin legacyPlugin) {
        this(legacyPlugin, legacyPlugin == null ? null : legacyPlugin.getClass().getSimpleName());
    }

    public PluginAdapter(IPlugin legacyPlugin, String name) {
        this.legacyPlugin = legacyPlugin;
        this.name = name == null ? "legacy-plugin" : name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int order() {
        // 旧插件默认顺序 100，保证在新插件（数据源等）之后启动，与旧版行为一致
        return 100;
    }

    @Override
    public void onStart() throws Exception {
        if (legacyPlugin != null && !legacyPlugin.start()) {
            throw new IllegalStateException("Plugin start failed: " + name);
        }
    }

    @Override
    public void onStop() {
        if (legacyPlugin != null) {
            legacyPlugin.stop();
        }
    }

    public IPlugin getLegacyPlugin() {
        return legacyPlugin;
    }
}
