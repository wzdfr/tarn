
package com.dfrdev.tarn.plugin.druid;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.plugin.IDataSourceProvider;
import com.dfrdev.tarn.plugin.IPlugin;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;
import com.dfrdev.tarn.util.TextKit;
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.config.ConfigTools;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * DruidPlugin.
 */
public class DruidPlugin implements IPlugin, IDataSourceProvider {

    // 基本属性 url、user、password
    private String url;
    private String username;
    private String password;
    private String driverClass = "com.mysql.jdbc.Driver";

    // 配置初始化大小、最小、最大
    private int initialSize = 10;
    private int minIdle = 10;
    private int maxActive = 100;

    // 配置获取连接等待超时的时间
    private long maxWait = 60000;

    // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
    private long timeBetweenEvictionRunsMillis = 60000;

    // 配置一个连接在池中最小生存的时间，单位是毫秒
    private long minEvictableIdleTimeMillis = 300000;

    /**
     * hsqldb - "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS"
     * Oracle - "select 1 from dual"
     * DB2 - "select 1 from sysibm.sysdummy1"
     * mysql - "select 1"
     */
    private String validationQuery = "select 1";
    private boolean testWhileIdle = true;
    private boolean testOnBorrow = false;
    private boolean testOnReturn = false;

    // 打开PSCache，并且指定每个连接上PSCache的大小
    private boolean poolPreparedStatements = false;    // mysql 建议为 false, oracle 建议为 true;
    private int maxPoolPreparedStatementPerConnectionSize = 20;

    // 配置监控统计拦截的filters
    private String filters;    // 监控统计："stat"    防SQL注入："wall"     组合使用： "stat,wall"
    private List<Filter> filterList;
    // 密码是否加密（对应 druid.properties / tarn.datasource.decrypt）
    private boolean decrypt = false;
    // 解密公钥（Druid ConfigTools 生成的 publicKey，可空=使用 Druid 内置默认公钥）
    private String publicKey;
    private DruidDataSource ds;
    private static final ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();

    /** 密码加密开关属性名（与 druid.properties 保持一致） */
    private static final String PROP_DECRYPT = "decrypt";
    /** 解密公钥属性名 */
    private static final String PROP_PUBLIC_KEY = "public-key";

    public DruidPlugin() {
    }

    public DruidPlugin(Properties properties) {
        start(properties);
    }

    public DruidPlugin(String url, String username, String password,boolean decrypt) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.decrypt = decrypt;
    }

    public DruidPlugin(String url, String username, String password, String driverClass,boolean decrypt) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClass = driverClass;
        this.decrypt = decrypt;
    }

    public DruidPlugin(String url, String username, String password, String driverClass, String filters,boolean decrypt) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClass = driverClass;
        this.filters = filters;
        this.decrypt = decrypt;
    }

    public static Properties loadPropertyFile(String fullFile) {
        String webRootPath = null;
        if (null == fullFile || fullFile.equals(""))
            throw new IllegalArgumentException(
                    "Properties file path can not be null : " + fullFile);
        webRootPath = DruidPlugin.class.getClassLoader().getResource("")
                .getPath();
        webRootPath = new File(webRootPath).getParent();
        InputStream inputStream = null;
        Properties p = null;
        try {
            inputStream = new FileInputStream(new File(webRootPath
                    + File.separator + fullFile));
            p = new Properties();
            p.load(inputStream);
        } catch (FileNotFoundException e) {
            LogUtil.error("Properties file not found: "
                    + fullFile);
            throw new IllegalArgumentException("Properties file not found: "
                    + fullFile);
        } catch (IOException e) {
            LogUtil.error("Properties file can not be loading: " + fullFile);
            throw new IllegalArgumentException(
                    "Properties file can not be loading: " + fullFile);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                LogUtil.error("关闭 Druid 配置文件输入流失败: " + e.getMessage(), e);
            }
        }
        return p;
    }

    /**
     * 设置过滤器，如果要开启监控统计需要使用此方法或在构造方法中进行设置
     * <p>
     * 监控统计："stat"
     * 防SQL注入："wall"
     * 组合使用： "stat,wall"
     * </p>
     */
    public DruidPlugin setFilters(String filters) {
        this.filters = filters;
        return this;
    }

    public synchronized DruidPlugin addFilter(Filter filter) {
        if (filterList == null)
            filterList = new ArrayList<Filter>();
        filterList.add(filter);
        return this;
    }

    public boolean start(Properties properties) {
        // 密码解密必须在建池之前完成：解密失败属于配置错误，直接抛出（避免用密文去连库）
        resolvePassword(properties);
        try {
            ds = (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            LogUtil.error("[Tarn] Druid 连接池创建失败: " + e.getMessage());
        }
        // 创建失败时 ds 可能为 null，避免启动阶段 NPE（数据库不可用不应阻断 MVC 启动）
        if (ds != null) {
            addFilterList(ds);
        }
        return true;
    }

    /**
     * 处理 {@code decrypt} / {@code public-key} 两个属性：
     * <ul>
     *     <li>{@code decrypt=true} 时用 Druid 的 {@link ConfigTools} 还原明文密码</li>
     *     <li>解密后移除这两个属性，避免被 DruidDataSourceFactory 当作未知项</li>
     * </ul>
     * 兼容 druid.properties 的老用法（decrypt 直接写在配置文件里），
     * 也支持 application.yml 的 {@code tarn.datasource.decrypt} 经 ConnectionInit 传入。
     */
    private void resolvePassword(Properties properties) {
        boolean needDecrypt = Boolean.parseBoolean(String.valueOf(properties.getProperty(PROP_DECRYPT, "false")).trim());
        String key = properties.getProperty(PROP_PUBLIC_KEY);
        properties.remove(PROP_DECRYPT);
        properties.remove(PROP_PUBLIC_KEY);
        if (!needDecrypt) {
            return;
        }
        String cipherText = properties.getProperty("password");
        if (StringUtil.isEmpty(cipherText)) {
            return;
        }
        try {
            String plainText = StringUtil.isEmpty(key)
                    ? ConfigTools.decrypt(cipherText) : ConfigTools.decrypt(key.trim(), cipherText);
            properties.setProperty("password", plainText);
        } catch (Exception e) {
            throw new TarnException("[Tarn] 数据源密码解密失败，请检查 decrypt / public-key 与密文是否成对: "
                    + e.getMessage(), e);
        }
    }

    public boolean start() {
        if (decrypt && StringUtil.isNotEmpty(password)) {
            try {
                password = StringUtil.isEmpty(publicKey)
                        ? ConfigTools.decrypt(password) : ConfigTools.decrypt(publicKey.trim(), password);
            } catch (Exception e) {
                throw new TarnException("[Tarn] 数据源密码解密失败，请检查 decrypt / publicKey 与密文是否成对: "
                        + e.getMessage(), e);
            }
        }
        ds = new DruidDataSource();

        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClass);
        ds.setInitialSize(initialSize);
        ds.setMinIdle(minIdle);
        ds.setMaxActive(maxActive);
        ds.setMaxWait(maxWait);
        ds.setTimeBetweenConnectErrorMillis(timeBetweenEvictionRunsMillis);
        ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);

        ds.setValidationQuery(validationQuery);
        ds.setTestWhileIdle(testWhileIdle);
        ds.setTestOnBorrow(testOnBorrow);
        ds.setTestOnReturn(testOnReturn);
        ds.setPoolPreparedStatements(poolPreparedStatements);
        if (poolPreparedStatements == true)
            ds.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);

        if (TextKit.notBlank(filters))
            try {
                ds.setFilters(filters);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        addFilterList(ds);
        return true;
    }

    /**
     * 设置密码解密公钥（Druid ConfigTools 生成），配合 decrypt=true 使用
     */
    public DruidPlugin setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public DruidPlugin setDecrypt(boolean decrypt) {
        this.decrypt = decrypt;
        return this;
    }

    @Override
    public Connection getConnection() {
        Connection conn = threadLocal.get();
        if (conn == null) {
            try {
                conn = ds.getConnection();
            } catch (SQLException e) {
                LogUtil.error("Druid 获取数据库连接失败: " + e.getMessage(), e);
            }
        }
        return conn;
    }

    @Override
    public void close(Connection conn, ResultSet rs, Statement statement) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
            }
        }
        if (threadLocal.get() == null) {    // in transaction if conn in threadlocal
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new TarnException(e);
                }
            }
        }
    }

    @Override
    public String getDatabaseType() {
        return "druid";
    }

    @Override
    public void setThreadLocalConnection(Connection connection) {
        threadLocal.set(connection);
    }

    @Override
    public void removeThreadLocalConnection() {
        threadLocal.remove();        // threadLocal.set(null);
    }

    @Override
    public Connection getThreadLocalConnection() {
        return threadLocal.get();
    }

    private void addFilterList(DruidDataSource ds) {
        if (filterList != null) {
            List<Filter> targetList = ds.getProxyFilters();
            for (Filter add : filterList) {
                boolean found = false;
                for (Filter target : targetList) {
                    if (add.getClass().equals(target.getClass())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    targetList.add(add);
            }
        }
    }

    public boolean stop() {
        if (ds != null)
            ds.close();
        return true;
    }

    public DataSource getDataSource() {
        return ds;
    }

    public DruidPlugin set(int initialSize, int minIdle, int maxActive) {
        this.initialSize = initialSize;
        this.minIdle = minIdle;
        this.maxActive = maxActive;
        return this;
    }

    public DruidPlugin setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        return this;
    }

    public DruidPlugin setInitialSize(int initialSize) {
        this.initialSize = initialSize;
        return this;
    }

    public DruidPlugin setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    public DruidPlugin setMaxActive(int maxActive) {
        this.maxActive = maxActive;
        return this;
    }

    public DruidPlugin setMaxWait(long maxWait) {
        this.maxWait = maxWait;
        return this;
    }

    public DruidPlugin setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        return this;
    }

    public DruidPlugin setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        return this;
    }

    /**
     * hsqldb - "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS"
     * Oracle - "select 1 from dual"
     * DB2 - "select 1 from sysibm.sysdummy1"
     * mysql - "select 1"
     */
    public DruidPlugin setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    public DruidPlugin setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        return this;
    }

    public DruidPlugin setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        return this;
    }

    public DruidPlugin setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
        return this;
    }

    public DruidPlugin setPoolPreparedStatements(boolean poolPreparedStatements) {
        this.poolPreparedStatements = poolPreparedStatements;
        return this;
    }

    public DruidPlugin setMaxPoolPreparedStatementPerConnectionSize(int maxPoolPreparedStatementPerConnectionSize) {
        this.maxPoolPreparedStatementPerConnectionSize = maxPoolPreparedStatementPerConnectionSize;
        return this;
    }
}
