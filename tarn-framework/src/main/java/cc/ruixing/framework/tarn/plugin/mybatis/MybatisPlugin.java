package cc.ruixing.framework.tarn.plugin.mybatis;

import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.plugin.IDataSourceProvider;
import cc.ruixing.framework.tarn.plugin.IPlugin;
import cc.ruixing.framework.tarn.util.LogUtil;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

/**
 * MyBatis 集成插件（Tarn 2.0+）。
 * <p>
 * 与框架数据源无缝集成：自动复用已注册的 {@link IDataSourceProvider}（Druid/c3p0 插件）
 * 提供的 DataSource，老项目引入本插件即可使用 MyBatis，无需重复配置数据源。
 * <p>
 * 用法一（零配置，纯编程式，推荐新模块使用）：
 * <pre>
 *     // TarnConfig.configPlugin 中注册：
 *     plugins.add(new MybatisPlugin());
 *     // 之后业务代码：
 *     UserMapper mapper = MybatisKit.getMapper(UserMapper.class);
 * </pre>
 * 用法二（传统 mybatis-config.xml，配置文件放 classpath 根目录）：
 * <pre>
 *     plugins.add(new MybatisPlugin("mybatis-config.xml"));
 * </pre>
 * 依赖：将 mybatis-3.5.x.jar 放入 WEB-INF/lib（本工程已内置 3.5.17）。
 *
 * @author dfr
 * @see MybatisKit
 * @since 2.0
 */
public class MybatisPlugin implements IPlugin {

    private static volatile SqlSessionFactory sqlSessionFactory;

    private final String configLocation;

    public MybatisPlugin() {
        this(null);
    }

    /**
     * @param configLocation classpath 下的 mybatis 主配置文件路径；为 null 时使用纯编程式构建
     */
    public MybatisPlugin(String configLocation) {
        this.configLocation = configLocation;
    }

    public boolean start() {
        if (sqlSessionFactory != null) {
            return true;
        }
        try {
            DataSource dataSource = resolveDataSource();
            if (dataSource == null) {
                LogUtil.error("[MybatisPlugin] 启动失败：未找到数据源插件（IDataSourceProvider），请先注册 DbPlugin/DruidPlugin/C3p0Plugin");
                return false;
            }

            SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            if (configLocation != null) {
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(configLocation);
                if (in == null) {
                    LogUtil.error("[MybatisPlugin] 配置文件未找到: " + configLocation);
                    return false;
                }
                try {
                    sqlSessionFactory = builder.build(in);
                } finally {
                    in.close();
                }
            } else {
                // 纯编程式构建：复用 Tarn 数据源，开启驼峰映射
                Configuration configuration = new Configuration();
                configuration.setEnvironment(new Environment("tarn", new JdbcTransactionFactory(), dataSource));
                configuration.setMapUnderscoreToCamelCase(true);
                sqlSessionFactory = builder.build(configuration);
            }
            LogUtil.info("[MybatisPlugin] MyBatis 插件启动成功, config="
                    + (configLocation == null ? "<programmatic>" : configLocation));
            return true;
        } catch (Exception e) {
            LogUtil.error("[MybatisPlugin] 启动异常: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean start(Properties properties) {
        return start();
    }

    /**
     * 数据源解析优先级：构造器注入 &gt; 插件注册表中的 IDataSourceProvider
     */
    private DataSource resolveDataSource() {
        IDataSourceProvider provider = Config.getPlugins().findDataSource();
        return provider == null ? null : provider.getDataSource();
    }

    public boolean stop() {
        sqlSessionFactory = null;
        return true;
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}
