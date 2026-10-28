package examples.standalone;

import com.dfrdev.tarn.core.web.Filters;
import com.dfrdev.tarn.core.boot.TarnConfig;
import com.dfrdev.tarn.core.boot.Plugins;
import com.dfrdev.tarn.util.LogUtil;

/**
 * 独立运行的 TarnConfig 实现（等同 web.xml 的 configClass）。
 *
 * @author dfr
 * @since 2.0
 */
public class StandaloneAppConfig extends TarnConfig {

    @Override
    public void configFilter(Filters filters) {
        // 需要额外 Filter 时在这里注册，例如：
        // filters.add(new XxxFilter());
        LogUtil.info("[StandaloneDemo] configFilter 完成");
    }

    @Override
    public void configPlugin(Plugins me) {
        // 需要额外插件时在这里 add，例如：
        // me.add(new DbPlugin(me.findDataSource()));
        LogUtil.info("[StandaloneDemo] 插件数量: " + me.getPluginList().size());
    }
}
