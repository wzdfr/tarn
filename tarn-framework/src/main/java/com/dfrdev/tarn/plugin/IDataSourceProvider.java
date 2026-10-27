
package com.dfrdev.tarn.plugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * IDataSourceProvider
 * 构造函数返回DataSource
 * <p/>
 */
public interface IDataSourceProvider {
    DataSource getDataSource();

    public Connection getConnection();

    public void close(Connection conn, ResultSet rs, Statement statement);

    public String getDatabaseType();

    public Connection getThreadLocalConnection();

    public void setThreadLocalConnection(Connection connection);

    public void removeThreadLocalConnection();
}


