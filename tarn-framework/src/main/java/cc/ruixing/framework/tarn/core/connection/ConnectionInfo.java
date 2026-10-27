package cc.ruixing.framework.tarn.core.connection;

import java.sql.Connection;

public class ConnectionInfo {
    private Connection conn = null;
    private String status = null;    //AVAILABLE:可用状态  UNAVAILABLE：不可用

    public ConnectionInfo(Connection conn, String status) {
        this.conn = conn;
        this.status = status;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
