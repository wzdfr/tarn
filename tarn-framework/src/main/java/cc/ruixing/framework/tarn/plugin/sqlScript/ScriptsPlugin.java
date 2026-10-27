package cc.ruixing.framework.tarn.plugin.sqlScript;

import cc.ruixing.framework.tarn.plugin.IDataSourceProvider;
import cc.ruixing.framework.tarn.plugin.IPlugin;
import cc.ruixing.framework.tarn.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 脚本执行类
 *
 * @author dfr
 * @since 0.1
 */
public class ScriptsPlugin implements IPlugin {
    private static final Logger log = LoggerFactory.getLogger(ScriptsPlugin.class);
    //数据源
    private static boolean isStarted = false;
    private static DataSource dataSource;
    private static IDataSourceProvider dataSourceProvider;
    public String testRunInitScriptSql = "select * from sys_users";

    public String schemaRunInitScriptSqlFile = "db/schema-business.sql";
    public String initDataRunInitScriptSqlFile = "db/init-data.sql";


    public String[] scriptFilePaths;


    /**
     * 根据DataSource构造插件
     *
     * @param dataSource 数据源
     */
    public ScriptsPlugin(DataSource dataSource) {
        ScriptsPlugin.dataSource = dataSource;
    }

    public String getTestRunInitScriptSql() {
        return testRunInitScriptSql;
    }

    public void setTestRunInitScriptSql(String testRunInitScriptSql) {
        this.testRunInitScriptSql = testRunInitScriptSql;
    }

    public String getSchemaRunInitScriptSqlFile() {
        return schemaRunInitScriptSqlFile;
    }

    public void setSchemaRunInitScriptSqlFile(String schemaRunInitScriptSqlFile) {
        this.schemaRunInitScriptSqlFile = schemaRunInitScriptSqlFile;
    }

    public String getInitDataRunInitScriptSqlFile() {
        return initDataRunInitScriptSqlFile;
    }

    public void setInitDataRunInitScriptSqlFile(String initDataRunInitScriptSqlFile) {
        this.initDataRunInitScriptSqlFile = initDataRunInitScriptSqlFile;
    }


    /**
     * 指定脚本运行文件路径
     *
     * @param filesPath
     */
    public String[] setScriptFilePaths(String[] filesPath) {
        this.scriptFilePaths = filesPath;
        return scriptFilePaths;

    }


    /**
     * 根据数据源提供者构造插件
     *
     * @param dataSourceProvider 数据源提供接口
     */
    public ScriptsPlugin(IDataSourceProvider dataSourceProvider) {
        ScriptsPlugin.dataSourceProvider = dataSourceProvider;
    }

    @Override
    public boolean start() {
        if (isStarted)
            return true;
        if (dataSourceProvider != null)
            dataSource = dataSourceProvider.getDataSource();
        if (dataSource == null)
            throw new RuntimeException("ScriptsPlugin start error: ScriptsPlugin need DataSource");
        log.info("scripts running......");
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            if (isExec(conn)) {
                log.info("script has completed execution.skip this step");
                return true;
            }
//            String databaseType = JdbcHelper.getDatabaseType(conn);
            //"db/schema-" + databaseType + ".sql"
            ScriptRunner runner = new ScriptRunner(conn, true);
            if (scriptFilePaths != null) {
                for (String schema : scriptFilePaths) {
                    runner.runScript(schema);
                }
            } else {
                String[] schemas = new String[]{schemaRunInitScriptSqlFile, initDataRunInitScriptSqlFile};
                for (String schema : schemas) {
                    runner.runScript(schema);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSourceProvider.close(conn, null, null);
        }
        isStarted = true;
        return true;
    }

    /**
     * 判断是否已经执行过脚本[暂时根据sys_users表是否有数据]
     *
     * @param conn 数据库连接
     * @return 是否执行成功
     */
    public boolean isExec(Connection conn) {
        Statement stmt = null;
        try {
            if (StringUtil.isEmpty(testRunInitScriptSql)) testRunInitScriptSql = "SELECT * FROM test ";
            stmt = conn.createStatement();
            stmt.execute(testRunInitScriptSql);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            dataSourceProvider.close(null, null, stmt);
        }
    }

    @Override
    public boolean start(Properties properties) {
        return false;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
