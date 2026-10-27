package cc.ruixing.framework.tarn.core.connection;


/**
 * Created by dfr on 14-4-18.
 */
public class ConnectionConfig {
    public static int initialSize = 2;   //初始连接数
    public static int minIdle = 2; //连接池的最小空闲连接数
    public static int maxActive = 10; //连接池的最大空闲连接数 .（最大的可用连接数）
    public static int maxIdle = 64; //连接池的最大连接数 （包括不可用链接）


    public static String driverClassName = ""; // 数据库驱动
    public static String url = ""; // 数据 URL
    public static String username = ""; // 数据库用户名
    public static String password = ""; // 数据库用户密码
    public static String databaseType = ""; //数据库类型
    public static Integer conCount = 0; //这个是静态的。当前连接数

    public static Integer ConnectionTimeout=28860000;
    public static Integer ConnectionCheck=60000;
    public static String dbTypeVersion="2008";


    public static int getInitialSize() {
        return initialSize;
    }

    public static void setInitialSize(int initialSize) {
        ConnectionConfig.initialSize = initialSize;
    }

    public static int getMinIdle() {
        return minIdle;
    }

    public static void setMinIdle(int minIdle) {
        ConnectionConfig.minIdle = minIdle;
    }

    public static int getMaxActive() {
        return maxActive;
    }

    public static void setMaxActive(int maxActive) {
        ConnectionConfig.maxActive = maxActive;
    }

    public static int getMaxIdle() {
        return maxIdle;
    }

    public static void setMaxIdle(int maxIdle) {
        ConnectionConfig.maxIdle = maxIdle;
    }

    public static String getDriverClassName() {
        return driverClassName;
    }

    public static void setDriverClassName(String driverClassName) {
        ConnectionConfig.driverClassName = driverClassName;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        ConnectionConfig.url = url;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        ConnectionConfig.username = username;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        ConnectionConfig.password = password;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public static void setDatabaseType(String databaseType) {
        ConnectionConfig.databaseType = databaseType;
    }

    public static Integer getConCount() {
        return conCount;
    }

    public static void setConCount(Integer conCount) {
        ConnectionConfig.conCount = conCount;
    }

    public static Integer getConnectionTimeout() {
        return ConnectionTimeout;
    }

    public static Integer getConnectionCheck() {
        return ConnectionCheck;
    }

    public static String getDbTypeVersion() {
        return dbTypeVersion;
    }
}
