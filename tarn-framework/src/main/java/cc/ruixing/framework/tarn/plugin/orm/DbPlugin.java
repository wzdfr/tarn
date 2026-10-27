package cc.ruixing.framework.tarn.plugin.orm;

import cc.ruixing.framework.tarn.plugin.IDataSourceProvider;
import cc.ruixing.framework.tarn.plugin.IPlugin;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Created by dfr on 15-5-17.
 */
public class DbPlugin implements IPlugin {
    private static DataSource dataSource;
    private static IDataSourceProvider dataSourceProvider;
    private static boolean isStarted = false;

    public DbPlugin(IDataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
        dataSource = dataSourceProvider.getDataSource();
    }

    @Override
    public boolean start() {
        if(isStarted){
            return true;
        }



        return false;
    }

    @Override
    public boolean start(Properties properties) {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
