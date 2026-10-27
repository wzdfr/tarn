package cc.ruixing.framework.tarn.core.connection;


import cc.ruixing.framework.tarn.util.LogUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

/**
 * 一个控制连接超时监控类
 */
public class ConnectionManagerTimeout extends Thread {
  TarnDataSource jds = TarnDataSource.getInstance();
    protected final long timeDiff = ConnectionConfig.getConnectionTimeout();//1小时60分钟，1分钟60秒，1秒1000毫秒
    protected static int tsnum = 0;

    public synchronized void run() {
        while (true) {
            Vector<ConnectionInfo> pool = jds.connectionPool;
            if (tsnum > 0) {
                if (pool.size() > 0) {
                    Vector<ConnectionInfo> delVetor = new Vector<ConnectionInfo>();
                    for (ConnectionInfo ci : pool) {
                        if (ci.getStatus().equals("AVAILABLE")) {
                            try {
                                if (!execTest(ci.getConn(), "SELECT * FROM CONNECTIONPOOLTEST  where id=1")) {
                                    LogUtil.debug("进入超时连接检测.[无异常]....");
                                }
                            } catch (SQLException e) {
                                LogUtil.error("连接池中当前连接出现异常，原因可能是一个超时连接...");
                                delVetor.add(ci);
                                continue;
                            }
                        }
                    }
                    pool.removeAll(delVetor);
                }
            }
            try {
                tsnum++;
                sleep(timeDiff);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 该方法是专用于测试连接池的连接是否正常<br/>
     */
    protected boolean execTest(Connection conn, String sql) throws SQLException {
        Statement pstmt = null;
        boolean cf = false;
        try {
            pstmt = conn.createStatement();
            // 执行操作
            pstmt.executeQuery(sql);
            cf = true;
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return cf;
    }
}
