package com.dfrdev.tarn.plugin.c3p0;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.core.connection.ConnectionConfig;
import com.dfrdev.tarn.plugin.IDataSourceProvider;
import com.dfrdev.tarn.plugin.IPlugin;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * The c3p0 datasource plugin.
 *
 * @deprecated Tarn 2.0 起淘汰 c3p0（存在已知 CVE、社区停止维护），
 * 请迁移至 {@link com.dfrdev.tarn.plugin.druid.DruidPlugin}。
 * 本类仅为老项目二进制兼容保留，新代码请勿使用。
 */
@Deprecated
public class C3p0Plugin extends ConnectionConfig implements IPlugin, IDataSourceProvider {

    private String jdbcUrl = "";
    private String user = "";
    private String password = "";
    private String driverClass = "";
    //连接池中保留的最大连接数。默认值: 64
    private int maxPoolSize = 64;
    //连接池中保留的最小连接数  默认为2
    private int minPoolSize = 2;
    //初始化连接池中的连接数，取值应在minPoolSize与maxPoolSize之间，默认为2
    private int initialPoolSize = 2;
    //最大空闲时间，多少秒内未使用则连接被丢弃。若为0则永不丢弃。默认值: 60
    private int maxIdleTime = 60;
    //当连接池中的连接耗尽的时候c3p0一次同时获取的连接数。默认值: 2
    private int acquireIncrement = 2;
    //当连接池连接耗尽时，客户端调用getConnection()后等待获取新连接的时间，超时后将抛出SQLException，如设为0则无限期等待。单位毫秒。默认: 20000
    private int checkoutTimeout = 20000;

    private ComboPooledDataSource dataSource;
    private static final ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();

    /**
     * 字符串为 null 或者为  "" 时返回 true
     */
    public static boolean isBlank(String str) {
        return str == null || "".equals(str.trim()) ? true : false;
    }

    public C3p0Plugin setDriverClass(String driverClass) {
        if (isBlank(driverClass))
            throw new IllegalArgumentException("driverClass can not be blank.");
        this.driverClass = driverClass;
        return this;
    }

    public C3p0Plugin setMaxPoolSize(int maxPoolSize) {
        if (maxPoolSize < 1)
            throw new IllegalArgumentException("maxPoolSize must more than 0.");
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public C3p0Plugin setMinPoolSize(int minPoolSize) {
        if (minPoolSize < 1)
            throw new IllegalArgumentException("minPoolSize must more than 0.");
        this.minPoolSize = minPoolSize;
        return this;
    }

    public C3p0Plugin setInitialPoolSize(int initialPoolSize) {
        if (initialPoolSize < 1)
            throw new IllegalArgumentException("initialPoolSize must more than 0.");
        this.initialPoolSize = initialPoolSize;
        return this;
    }

    public C3p0Plugin setMaxIdleTime(int maxIdleTime) {
        if (maxIdleTime < 1)
            throw new IllegalArgumentException("maxIdleTime must more than 0.");
        this.maxIdleTime = maxIdleTime;
        return this;
    }

    public C3p0Plugin setAcquireIncrement(int acquireIncrement) {
        if (acquireIncrement < 1)
            throw new IllegalArgumentException("acquireIncrement must more than 0.");
        this.acquireIncrement = acquireIncrement;
        return this;
    }

    public void setCheckoutTimeout(int checkoutTimeout) {
        this.checkoutTimeout = checkoutTimeout;
    }

    public C3p0Plugin() {
    }

    public C3p0Plugin(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    public C3p0Plugin(String jdbcUrl, String user, String password, String driverClass) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.driverClass = driverClass != null ? driverClass : this.driverClass;
    }

    public C3p0Plugin(String jdbcUrl, String user, String password, String driverClass, Integer maxPoolSize, Integer minPoolSize, Integer initialPoolSize, Integer maxIdleTime, Integer acquireIncrement) {
        initC3p0Properties(jdbcUrl, user, password, driverClass, maxPoolSize, minPoolSize, initialPoolSize, maxIdleTime, acquireIncrement);
    }

    private void initC3p0Properties(String jdbcUrl, String user, String password, String driverClass, Integer maxPoolSize, Integer minPoolSize, Integer initialPoolSize, Integer maxIdleTime, Integer acquireIncrement) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.driverClass = driverClass != null ? driverClass : this.driverClass;
        this.maxPoolSize = maxPoolSize != null ? maxPoolSize : this.maxPoolSize;
        this.minPoolSize = minPoolSize != null ? minPoolSize : this.minPoolSize;
        this.initialPoolSize = initialPoolSize != null ? initialPoolSize : this.initialPoolSize;
        this.maxIdleTime = maxIdleTime != null ? maxIdleTime : this.maxIdleTime;
        this.acquireIncrement = acquireIncrement != null ? acquireIncrement : this.acquireIncrement;
    }

    public C3p0Plugin(File propertyfile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propertyfile);
            Properties ps = new Properties();
            ps.load(fis);

            initC3p0Properties(ps.getProperty("jdbcUrl"), ps.getProperty("user"), ps.getProperty("password"), ps.getProperty("driverClass"),
                    toInt(ps.getProperty("maxPoolSize")), toInt(ps.getProperty("minPoolSize")), toInt(ps.getProperty("initialPoolSize")),
                    toInt(ps.getProperty("maxIdleTime")), toInt(ps.getProperty("acquireIncrement")));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public C3p0Plugin(Properties properties) {
        Properties ps = properties;
        this.jdbcUrl = properties.getProperty("jdbcUrl");
        this.user = properties.getProperty("user");
        this.password = properties.getProperty("password");
        this.driverClass = ps.getProperty("driverClass");
        if (ps.getProperty("maxPoolSize") != null && !ps.getProperty("maxPoolSize").equals(""))
            this.maxPoolSize = toInt(ps.getProperty("maxPoolSize"));
        if (ps.getProperty("initialPoolSize") != null && !ps.getProperty("initialPoolSize").equals(""))
            this.initialPoolSize = toInt(ps.getProperty("initialPoolSize"));
        if (ps.getProperty("minPoolSize") != null && !ps.getProperty("minPoolSize").equals(""))
            this.minPoolSize = toInt(ps.getProperty("minPoolSize"));
        if (ps.getProperty("maxIdleTime") != null && !ps.getProperty("maxIdleTime").equals(""))
            this.maxIdleTime = toInt(ps.getProperty("maxIdleTime"));
        if (ps.getProperty("acquireIncrement") != null && !ps.getProperty("acquireIncrement").equals(""))
            this.acquireIncrement = toInt(ps.getProperty("acquireIncrement"));
        if (ps.getProperty("checkoutTimeout") != null && !ps.getProperty("checkoutTimeout").equals(""))
            this.checkoutTimeout = toInt(ps.getProperty("checkoutTimeout"));


    }

    @Override
    public boolean start(Properties properties) {
        dataSource = new ComboPooledDataSource();
        dataSource.setProperties(properties);
        initC3p0Properties(properties.getProperty("jdbcUrl"), properties.getProperty("user"), properties.getProperty("password"), properties.getProperty("driverClass"),
                toInt(properties.getProperty("maxPoolSize")), toInt(properties.getProperty("minPoolSize")), toInt(properties.getProperty("initialPoolSize")),
                toInt(properties.getProperty("maxIdleTime")), toInt(properties.getProperty("acquireIncrement")));
        if (toInt(properties.getProperty("checkoutTimeout")) != null)
            this.checkoutTimeout = toInt(properties.getProperty("checkoutTimeout"));
        return true;
    }

    public boolean start() {
        dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        try {
            dataSource.setDriverClass(driverClass);
        } catch (PropertyVetoException e) {
            dataSource = null;
            System.err.println("C3p0Plugin start error");
            throw new RuntimeException(e);
        }
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMaxIdleTime(maxIdleTime);
        dataSource.setAcquireIncrement(acquireIncrement);
        dataSource.setCheckoutTimeout(checkoutTimeout);
        return true;
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

    private Integer toInt(String str) {
        return Integer.parseInt(str);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Connection getConnection() {
        Connection conn = threadLocal.get();
        if (conn == null) {
            try {
                conn = dataSource.getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }

    @Override
    public void close(Connection conn, ResultSet rs, Statement st) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (st != null) {
            try {
                st.close();
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
        return databaseType;
    }

    public boolean stop() {
        if (dataSource != null)
            dataSource.close();
        return true;
    }
}

