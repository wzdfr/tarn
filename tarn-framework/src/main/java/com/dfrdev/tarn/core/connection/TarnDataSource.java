package com.dfrdev.tarn.core.connection;


import com.dfrdev.tarn.plugin.IDataSourceProvider;
import com.dfrdev.tarn.plugin.IPlugin;
import com.dfrdev.tarn.util.LogUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by dfr on 14-4-18.
 */
public class TarnDataSource extends ConnectionConfig implements IPlugin,IDataSourceProvider {

    public final static String PROP_DRIVERCLASSNAME = "driverClassName"; // 数据库驱动
    public final static String PROP_MAXACTIVE = "maxActive";//连接池的最大空闲连接数 .（最大的可用连接数）
    public final static String PROP_MAXIDLE = "maxIdle";//连接池的最大连接数 （包括不可用链接）
    public final static String PROP_MINIDLE = "minIdle";//连接池的最小空闲连接数
    public final static String PROP_INITIALSIZE = "initialSize";//初始连接数
    public final static String PROP_PASSWORD = "password";//数据库用户密码
    public final static String PROP_URL = "url"; //数据库 URL
    public final static String PROP_USERNAME = "username"; //数据库用户名


    private static final ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
    protected static Vector<ConnectionInfo> connectionPool = new Vector<ConnectionInfo>();
    private static final TarnDataSource jds = new TarnDataSource();

    public DataSource getDataSource() {
        LogUtil.debug("Not supported.");
        return null;
    }

    /**
     * 从连接池中得到一个链接。
     *
     * @return Connection 返回一个可用链接
     */
    public synchronized Connection getConnection() {
        Connection conn = threadLocal.get();
        if (conn != null)
            return conn;
        LogUtil.debug("pool size:" + connectionPool.size());
        try {
            for (ConnectionInfo ci : connectionPool) {
                if (ci.getStatus().equals("AVAILABLE")) {
                    if (ci.getConn() == null) {
                        connectionPool.remove(ci);
                    } else if (ci.getConn().isClosed()) {
                        ci.getConn().close();
                        connectionPool.remove(ci);
                    } else {
                        conn = ci.getConn();
                        ci.setStatus("UNAVAILABLE");
                    }
                    break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (conn == null) {
            conn = getnewConnection();
            ConnectionInfo ci = new ConnectionInfo(conn, "AVAILABLE");
            connectionPool.add(ci);
        }
        return conn;
    }

    @Override
    public boolean start(Properties properties) {
        try {
            config(this, (Map) properties);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        start();
        return false;
    }

    @SuppressWarnings("rawtypes")
    public static TarnDataSource createDataSource(Properties properties) throws Exception {
        return createDataSource((Map) properties);
    }

    @SuppressWarnings("rawtypes")
    public static TarnDataSource createDataSource(Map properties) throws Exception {
        TarnDataSource dataSource = new TarnDataSource();
        config(dataSource, properties);
        return dataSource;
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    public static void config(TarnDataSource dataSource, Map properties) throws SQLException {
        String value = null;

        value = (String) properties.get(PROP_DRIVERCLASSNAME);
        if (value != null) {
            dataSource.setDriverClassName(value);
        }

        value = (String) properties.get(PROP_MAXACTIVE);
        if (value != null) {
            dataSource.setMaxActive(Integer.parseInt(value));
        }

        value = (String) properties.get(PROP_MAXIDLE);
        if (value != null) {
            dataSource.setMaxIdle(Integer.parseInt(value));
        }

        value = (String) properties.get(PROP_MINIDLE);
        if (value != null) {
            dataSource.setMinIdle(Integer.parseInt(value));
        }

        value = (String) properties.get(PROP_INITIALSIZE);
        if (value != null) {
            dataSource.setInitialSize(Integer.parseInt(value));
        }

        value = (String) properties.get(PROP_PASSWORD);
        if (value != null) {
            dataSource.setPassword(value);
        }

        value = (String) properties.get(PROP_URL);
        if (value != null) {
            dataSource.setUrl(value);
        }

        value = (String) properties.get(PROP_USERNAME);
        if (value != null) {
            dataSource.setUsername(value);
        }


    }

    public boolean start() {
        for (int i = 0; i < initialSize; i++) {
            ConnectionInfo ci = new ConnectionInfo(getnewConnection(), "AVAILABLE");
            connectionPool.add(ci);
        }
        return true;

    }

    public void setThreadLocalConnection(Connection connection) {
        threadLocal.set(connection);
    }

    public void removeThreadLocalConnection() {
        threadLocal.remove();        // threadLocal.set(null);
    }




    public Connection getThreadLocalConnection() {
        return threadLocal.get();
    }

    public boolean stop() {
        try {
            for (ConnectionInfo ci : connectionPool) {
                ci.getConn().close();
                ci.setConn(null);
                ci.setStatus(null);
                ci=null;
            }
            connectionPool = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static TarnDataSource getInstance() {
        return jds;
    }



    /**
     * 从连接池中得到一个链接对象信息。
     *
     * @return Connection 返回一个可用链接对象
     */
    public synchronized ConnectionInfo getConnectionInfo() {
        ConnectionInfo conni = null;
        for (ConnectionInfo ci : connectionPool) {
            if (ci.getStatus().equals("AVAILABLE")) {
                conni = ci;
                ci.setStatus("UNAVAILABLE");
                break;
            }
        }
        return conni;
    }

    /**
     * 注销一个链接。
     *
     * @return boolean
     */
    public synchronized boolean freeConnection(Connection con) {
        boolean b = false;
        for (ConnectionInfo ci : connectionPool) {
            if (ci.getConn().equals(con) || ci.getConn() == con) {
                ci.setStatus("AVAILABLE");
                b = true;
                break;
            }
        }
        return b;
    }


    /**
     * 关闭
     */
    public void close(Connection conn, ResultSet rs, Statement st) {
        if (rs != null) {try {rs.close();} catch (SQLException e) {}}
        if (st != null) {try {st.close();} catch (SQLException e) {}}
        if (conn != null) {
            for (ConnectionInfo ci : connectionPool) {
                if (ci.getConn().equals(conn) || ci.getConn() == conn) {
                    ci.setStatus("AVAILABLE");
                    break;
                }
            }
        }


    }


    public void close(Connection connection) {
        try {
            for (ConnectionInfo ci : connectionPool) {
                if (ci.getConn().equals(connection) || ci.getConn() == connection) {
                    ci.getConn().close();
                    connectionPool.remove(ci);
                }
                break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



    public synchronized int getPoolSize() {
        return connectionPool.size();
    }

    public synchronized int getPoolSizeAna() {
        int i = 0;
        for (ConnectionInfo ci : connectionPool) {
            if (ci.getStatus().equals("AVAILABLE")) i++;
        }
        return i;
    }

    public synchronized int getPoolSizeUnana() {
        int i = 0;
        for (ConnectionInfo ci : connectionPool) {
            if (ci.getStatus().equals("UNAVAILABLE")) i++;
        }
        return i;
    }

    private synchronized Connection getnewConnection() {
        return newConnection();
    }

    public synchronized Connection newConnection() {
        Connection conn = null;
        try {
            Class.forName(driverClassName);
            conn = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}
