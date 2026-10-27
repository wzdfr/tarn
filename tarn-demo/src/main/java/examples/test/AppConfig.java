package examples.test;

import com.dfrdev.tarn.core.Filters;
import com.dfrdev.tarn.core.TarnConfig;
import com.dfrdev.tarn.core.Plugins;
import com.dfrdev.tarn.plugin.IPlugin;
import com.dfrdev.tarn.plugin.sqlScript.ScriptsPlugin;
import com.alibaba.fastjson.JSON;

/**
 * Created by dfr on 15-5-13.
 */
public class AppConfig extends TarnConfig {
    /**
     * 配置Filters
     *
     * @param filters
     */
    @Override
    public void configFilter(Filters filters) {

    }

    /**
     * 配置插件
     *
     * @param me
     */
    @Override
    public void configPlugin(Plugins me) {
        System.out.println("===== 自定义配置插件 =======");
        System.out.println("====plugin size: ======="+me.getPluginList().size());

        ScriptsPlugin scriptPlugin=new ScriptsPlugin(me.findDataSource());
        scriptPlugin.setTestRunInitScriptSql("SELECT * FROM bus_cccc");
//        scriptPlugin.setTestRunInitScriptSql();
        //添加sql 脚本执行插件
        me.add(scriptPlugin);
//        System.out.println("====plugin size2: ======="+me.getPluginList().size());
//        System.out.println("data source:"+me.findDataSource());
//        System.out.println("echchae:"+me.findEhcache());


    }
}
