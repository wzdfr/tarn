package cc.ruixing.framework.tarn.plugin;

/**
 * 新版插件接口（Tarn 2.0+）。
 * <p>
 * 相比旧版 {@link IPlugin}：
 * <ul>
 *     <li>支持插件名称与启动优先级排序（order 越小越先启动）</li>
 *     <li>stop 支持异常传递，避免启动失败被静默吞掉</li>
 *     <li>旧插件无需改造，由 {@link PluginAdapter} 自动适配接入</li>
 * </ul>
 * 新插件示例：
 * <pre>
 *     public class XxxPlugin implements Plugin {
 *         public String name() { return "xxx"; }
 *         public void onStart() { ... }
 *         public void onStop() { ... }
 *     }
 *     // 注册（TarnConfig.configPlugin 中）：
 *     PluginManager.me().register(new XxxPlugin());
 * </pre>
 *
 * @author dfr
 * @see PluginManager
 * @see PluginAdapter
 * @since 2.0
 */
public interface Plugin {

    /**
     * 插件名称，用于日志与查找
     */
    String name();

    /**
     * 启动顺序，越小越先启动，默认 0。
     * 数据源类插件建议返回 -100 以保证最先启动。
     */
    default int order() {
        return 0;
    }

    /**
     * 插件启动
     *
     * @throws Exception 启动失败时抛出，框架会中断应用启动并记录日志
     */
    void onStart() throws Exception;

    /**
     * 插件停止（应用关闭 / 插件卸载时回调）
     */
    default void onStop() {
    }
}
