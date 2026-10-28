package com.dfrdev.tarn.core.connection;


import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.plugin.IDataSourceProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * ConnectionFactory
 * @author dfr
 */
public class ConnectionFactory {
    public static IDataSourceProvider DATA_SOURCE_POOL = null;


    public static synchronized void init() {
        if (DATA_SOURCE_POOL == null) {
            String confConnectionType = Tarn.sysDevConf("connectionType");
            // 未配置时默认 Druid（c3p0 已淘汰，system 池仅老项目保留）
            if (confConnectionType == null || confConnectionType.trim().isEmpty()) {
                confConnectionType = "druid";
            }
            ConnectionType connectionType;
            if ("c3p0".equalsIgnoreCase(confConnectionType)) connectionType = ConnectionType.C3p0;
            else if ("druid".equalsIgnoreCase(confConnectionType)) connectionType = ConnectionType.Druid;
            else connectionType = ConnectionType.TARN_SYSTEM;
            DATA_SOURCE_POOL = ConnectionInit.init(connectionType);
        }
    }

    /**
     * 事务开启<br/>
     * 执行此语句后，其后的数据库操作将以事务处理的方式运行，
     */
    public void transOpen() throws TarnException, SQLException {
        if (DATA_SOURCE_POOL == null) throw new TarnException("dataBase source null.");
        transOpen(Connection.TRANSACTION_READ_COMMITTED);
    }

    /**
     * 以给定的事务隔离级别开启事务处理. <br />
     */
    public void transOpen(int level) throws TarnException, SQLException {
        if (!transExists()) {
            this.transRollback();
            throw new TarnException("事务处理已经存在.");
        }
        DATA_SOURCE_POOL.setThreadLocalConnection(DATA_SOURCE_POOL.getConnection());
        Connection connection = DATA_SOURCE_POOL.getThreadLocalConnection();
        try {
            connection.setTransactionIsolation(level);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            if (connection != null) {
                DATA_SOURCE_POOL.removeThreadLocalConnection();
                DATA_SOURCE_POOL.close(connection, null, null);
            }
            throw e;
        }
    }

    /**
     * 判断是否存在事务
     */
    public boolean transExists() {
        return DATA_SOURCE_POOL.getThreadLocalConnection() == null;
    }

    /**
     * 提交事务
     */
    public void transCommit() throws TarnException, SQLException {
        if (DATA_SOURCE_POOL == null) throw new TarnException("dataBase source null.");
        if (!transExists()) {
            Connection connection = DATA_SOURCE_POOL.getThreadLocalConnection();
            connection.commit();
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            DATA_SOURCE_POOL.removeThreadLocalConnection();
            DATA_SOURCE_POOL.close(connection, null, null);
        }
    }

    /**
     * 事务回滚<br />
     */
    public void transRollback() throws SQLException {
        transRollback(null);
    }

    /**
     * 将事务回滚至指定的事务保存点. <br />
     */
    public void transRollback(Savepoint sp) throws TarnException, SQLException {
        if (!transExists()) {
            Connection connection = null;
            if (sp == null) {
                connection = DATA_SOURCE_POOL.getThreadLocalConnection();
                connection.rollback();
                connection.setAutoCommit(true);
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                DATA_SOURCE_POOL.removeThreadLocalConnection();
                DATA_SOURCE_POOL.close(connection, null, null);
            } else {
                connection.rollback(sp);
            }
        }
    }

    /**
     * 从连接池中获取一个链接
     *
     * @return Connection
     */
    public Connection getConnection() {
        if (DATA_SOURCE_POOL == null) init();
        if (DATA_SOURCE_POOL == null) {
            // 老代码只 new 了异常没有抛出，这里修复为真正抛出
            throw new TarnException("DataSourceProvider is null.");
        }
        return DATA_SOURCE_POOL.getThreadLocalConnection() == null ? DATA_SOURCE_POOL.getConnection() : DATA_SOURCE_POOL.getThreadLocalConnection();
    }

}
