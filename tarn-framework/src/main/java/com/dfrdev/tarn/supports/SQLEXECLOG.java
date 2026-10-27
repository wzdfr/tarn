package com.dfrdev.tarn.supports;




import com.dfrdev.tarn.core.DaoException;
import com.dfrdev.tarn.core.Tarn;
import com.dfrdev.tarn.supports.vo.SQLEXECVo;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.TextKit;
import com.dfrdev.tarn.util.VisitIpAddr;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SQLEXECLOG extends Thread {
    private static SQLEXECVo running = null; //正在执行的任务，为null表示没有任务在执行
    private static List<Thread> threadList = new Vector<Thread>();
    private static int threadLimit = 5;//定义允许的最大线程数
    private static  List<SQLEXECVo> sqlList = new ArrayList<SQLEXECVo>();
    private static Tarn tarn = new Tarn();
    @Override
    public void run() {
        while (true) {
            SQLEXECVo p = getSQLExecVo();
            if (p != null) {
                try {
                    saveSQLExecLog(p);
                } catch (Exception e) {
                    LogUtil.error("保存 SQL 执行日志失败: " + e.getMessage(), e);
                } catch (DaoException e) {
                    LogUtil.error("保存 SQL 执行日志失败: " + e.getMessage(), e);
                }
                try {
                    this.sleep(1000);
                } catch (InterruptedException e) {
                    LogUtil.error("SQL 日志线程中断: " + e.getMessage(), e);
                }
            } else {
                remove(this);
                break;
            }
        }
    }


    private void saveSQLExecLog(SQLEXECVo p) throws SQLException, DaoException {

        HashMap map = new HashMap();
        map.put("precSql", p.getPrecSql());
        map.put("realSql", p.getRealSql());
        String ip= VisitIpAddr.getIpAddr(p.getRequest());
        map.put("ip",ip);
        map.put("addr", VisitIpAddr.getAddressByIP(ip));
        map.put("time", TextKit.parseDate(null, null, true));
        tarn.saveEXEC(generateInsertSql("SQLEXECLOG"), map);

    }

    /**
     * 执行的SQL的相关信息加人队列中
     *
     * @param precSql 预编译SQL
     * @param realSql 实际SQL
     * @param request HttpServletRequest 请求对象
     */
    public static synchronized void addSQLExecvo(String precSql, String realSql, HttpServletRequest request) {
        SQLEXECVo s = new SQLEXECVo(precSql, realSql,request);
        sqlList.add(s);
        if (threadList.size() < threadLimit) {
            Thread t = new SQLEXECLOG();
            threadList.add(t);
            t.start();
        }
    }

    private String generateInsertSql(String tableName) {
        //===生成sql
        String sql = "INSERT INTO ";
        sql += tableName + " ";
        sql += "(";
        sql += "precSql,realSql,ip,addr,time";
        sql += ")";
        sql += " VALUES (";
        sql += "?,?,?,?,?";
        sql += ") ";
        return sql;
    }

    private static synchronized SQLEXECVo getSQLExecVo() {
        SQLEXECVo t;
        if (sqlList.size() > 0) {
            t = sqlList.get(0);
            sqlList.remove(0);
            running = t;
            return t;
        } else {
            return null;
        }
    }

    private static synchronized void remove(Thread t) {
        threadList.remove(t);
        if (threadList.size() <= 0) {
            running = null;
        }
    }

}
