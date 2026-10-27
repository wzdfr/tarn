package cc.ruixing.framework.tarn.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dfr on 14-11-18.
 */
public class AppendParmeterSQLUtils {
    /**
     * 将Map<String, Object>中的参数添加到SQL语句中
     *
     * @param sql   SQL语句
     * @param pstmt 需要执行的 <code>PreparedStatement</code>
     * @param map   参数集合
     * @throws java.sql.SQLException 当发生数据库操作异常时
     */
    public static String setParameterValue(String sql, PreparedStatement pstmt,
                                           Map<String, Object> map) {
        int count = parameterCount(sql);
        for (int i = 1; i <= count; i++) {
            String pName = parameterName(i, sql);
            boolean like = sql.substring(sql.indexOf("#" + pName) - 5, sql.indexOf("#" + pName)).equalsIgnoreCase("like ");
            sql = sql.replace(("#" + pName), "#?");
            try {
                Object o = map.get(pName);
                if (o != null)
                    if (like)
                        pstmt.setObject(i, "%" + o.toString() + "%");
                    else
                        pstmt.setObject(i, map.get(pName));
                else
                    throw new TarnException(pName + " 没有被设置！");
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new TarnException(pName + " 没有被设置！");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        sql = sql.replaceAll("#", "");
        return sql;
    }

    public static  String setParameterValueUpdate(String sql, PreparedStatement pstmt,
                                                  Map<String, Object> map){
        int count = parameterCount(sql);
        for (int i = 1; i <= count; i++) {
            String pName = parameterName(i, sql);
//            boolean like = sql.substring(sql.indexOf("#" + pName) - 5,sql.indexOf("#" + pName)).equalsIgnoreCase("like ");
            sql = sql.replace(("#" + pName), "#?");
            try {
                Object o = map.get(pName);
                if (o != null)
                        pstmt.setObject(i,o);
                else
                    throw new TarnException(pName + " 没有被设置！");
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new TarnException(pName + " 没有被设置！");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        sql = sql.replaceAll("#", "");
        return sql;
    }

    /**
     * 将Map<String, Object>中的参数添加到SQL语句中
     *
     * @param sql   SQL语句
     * @param pstmt 需要执行的 <code>PreparedStatement</code>
     * @param map   参数集合
     * @throws java.sql.SQLException 当发生数据库操作异常时
     */
    public static String setParameterValued(String sql, PreparedStatement pstmt,
                                            Map<String, Object> map, int count) throws SQLException {
        for (int i = 1; i <= count; i++) {
            String pName = parameterName(i, sql);
            sql = sql.replaceFirst(("#" + pName), "#?");
            try {
                Object o = map.get(pName);
                if (o != null)
                    //  pstmt.getResultSetType();
                    pstmt.setObject(i, map.get(pName));
                else
                    pstmt.setObject(i, null);
                //     throw new TarnException(pName + " 没有被设置！");
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw new TarnException(pName + " 没有被设置！");
            }
        }
        sql = sql.replaceAll("#", "");

        return sql;
    }

    /**
     * 替换SQL中的参数为一个正确的SQL。
     * <p/>
     * 不改变原有SQL，返回一个新的替换后的SQL。
     *
     * @param sql 需要替换的SQL
     * @return 替换后的SQL
     */
    public static String replaceSql(String sql) {
        String temp = sql;
        int count = parameterCount(temp);
        for (int i = 1; i <= count; i++) {
            String pName = parameterName(i, temp);
            temp = temp.replace(("#" + pName), "#?");
        }
        temp = temp.replaceAll("#", "");
        return temp;
    }

    public static String replaceSql(String sql, int count) {
        String temp = sql;
        for (int i = 1; i <= count; i++) {
            String pName = parameterName(i, temp);
            temp = temp.replaceFirst(("#" + pName), "#?");
        }
        temp = temp.replaceAll("#", "");
        return temp;
    }


    /**
     * 返回SQL语句中参数的个数
     *
     * @param sql
     * @return
     */
    public static int parameterCount(String sql) {
        int count = 0;
        while (sql.indexOf("#") > 0) {
            sql = sql.replaceFirst("#", "");
            count++;
        }
        return count;
    }

    /**
     * 返回指定位置的参数名称
     *
     * @param index 指定第几个参数
     * @param sql   需要的SQL语句
     * @return 指定位置的参数名称
     */
    public static String parameterName(int index, String sql) {
        int count = 0;
        if (index == 1)
            count = sql.indexOf("#");
        else {
            for (int i = 1; i < index; i++) {
                sql = sql.replaceFirst("#", "");
                count = sql.indexOf("#");
            }
        }

        String s = sql.substring(count + 1, sql.length());
        char[] sqls = s.toCharArray();
        StringBuffer temp = new StringBuffer();
        Pattern p = Pattern.compile("[a-zA-Z_0-9-]{1}");
        for (int i = 0, n = sqls.length; i < n; i++) {
            char c = sqls[i];
            Matcher m = p.matcher("" + c);
            if (m.matches())
                temp.append(c);
            else
                break;
        }
        return temp.toString();
    }
}
