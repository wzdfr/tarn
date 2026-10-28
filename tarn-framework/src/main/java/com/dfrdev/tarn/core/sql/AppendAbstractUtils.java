package com.dfrdev.tarn.core.sql;


import com.dfrdev.tarn.core.connection.ConnectionConfig;
import com.dfrdev.tarn.core.connection.ConnectionKit;
import com.dfrdev.tarn.util.text.StringUtil;

import java.io.Reader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.db.DAO;
import com.dfrdev.tarn.core.web.Model;
public class AppendAbstractUtils {
    private static final String STR_ORDERBY = " order by ";

    /**
     * 数据设置映射
     */
    public static void setValue(String[] cols, int columnType, int j, ResultSet rs, DAO d) throws SQLException {
        switch (columnType) {
            case Types.DATE:

            case Types.TIMESTAMP:
                Timestamp n = rs.getTimestamp(j);
                if (n != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(n.getTime());
                    d.set(cols[j - 1], sdf.format(c.getTime()));
                }
                break;
            case Types.BIGINT:
                d.set(cols[j - 1], rs.getLong(j));
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
                d.set(cols[j - 1], rs.getDouble(j));
                break;
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIT:
                d.set(cols[j - 1], rs.getInt(j));
                break;
            case Types.CLOB:
                d.set(cols[j - 1], clobToString(rs.getClob(j)));
                break;
            default:
                d.set(cols[j - 1], String.valueOf(rs.getObject(j)));
                break;
        }
    }

    /**
     * 处理Oracle Clob数据类型
     */
    public static String clobToString(Clob clob) {
        if (clob == null) return null;
        try {
            Reader inStreamDoc = clob.getCharacterStream();
            char[] tempDoc = new char[(int) clob.length()];
            inStreamDoc.read(tempDoc);
            inStreamDoc.close();
            return new String(tempDoc);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 根据指定SQL语句生成带分页的SQl。
     *
     * @param sql      SQl语句
     * @param pageSize 分页大小
     * @return String 生成后的SQL
     * @absPage 当前页码
     */
    public static String generatePageFragmentSql(String sql, int pageSize, int absPage) {
        String dbType = ConnectionKit.getDevConf("DBType");
        String dbVersion = ConnectionConfig.getDbTypeVersion();
        // Tarn 2.0+: 新增数据库类型（POSTGRESQL/DM/KINGBASE/GAUSSDB/VASTBASE/ORACLE12C）走方言体系；
        // MYSQL/MSSQL/ORACLE 保持老逻辑路径，行为完全不变
        if (dbType != null && !dbType.equalsIgnoreCase("MYSQL")
                && !dbType.equalsIgnoreCase("MSSQL") && !dbType.equalsIgnoreCase("ORACLE")) {
            int absPageForNew = absPage == 0 ? 1 : absPage;
            return com.dfrdev.tarn.orm.dialect.DialectFactory
                    .paged(sql, (absPageForNew - 1) * pageSize, pageSize);
        }
        String tempSql = "";
        int absPageT = absPage == 0 ? 1 : absPage;
        int pageStart = (absPageT - 1) * pageSize;//本页起始记录位置
        if (dbType.equalsIgnoreCase("MYSQL")) {
            tempSql = sql;
            if (pageSize > 0) {
                tempSql += " LIMIT " + pageStart + ", " + pageSize;
            }

        } else if (dbType.equalsIgnoreCase("MSSQL")) {
            if (dbVersion.equals("2000")) {
                String tem = sql.replaceFirst("(?i)select", "SELECT top " + pageSize + " ");
                Matcher matcher = Pattern.compile("(?i)where").matcher(tem);
                if (!matcher.find()) {
                    tem += " WHERE 1=1 ";
                }
                Matcher tableMatcher1 = Pattern.compile("(?i)from").matcher(tem);
                Matcher tableMatcher2 = Pattern.compile("(?i)where").matcher(tem);
                if (tableMatcher1.find()) {
                    tableMatcher2.find();
                    int sss = tableMatcher1.end();
                    int eee = tableMatcher2.start();
                    int whereE = tableMatcher2.end();
                    String table = tem.substring(sss, eee).trim();
                    String tb = table.indexOf(",") == -1 ? table : table.split(",")[0];
                    StringBuffer sb = new StringBuffer(tem);
                    String fff = " (" + tb + ".id NOT IN (SELECT TOP " + pageStart + " " + tb + ".id FROM " + table + " WHERE " + tem.substring((whereE), tem.length()) + ")) AND ";
                    sb.insert((whereE), fff);
                    tempSql = sb.toString();
                    sb = null;
                }
            } else {
                int orderIdx = sql.toLowerCase().lastIndexOf(STR_ORDERBY);
                String orderStr = null;
                if (orderIdx != -1) {
                    orderStr = sql.substring(orderIdx + 10);
                    sql = sql.substring(0, orderIdx);
                }
                StringBuffer pageSql = new StringBuffer();
                pageSql.append("select top ");
                pageSql.append(pageSize);
                pageSql.append(" * from (select row_number() over (");
                String orderBy = getOrderBy(sql, orderStr);
                pageSql.append(orderBy);
                pageSql.append(") row_number, * from (");
                pageSql.append(sql);
                pageSql.append(") aa ) a where row_number > ");
                pageSql.append(pageStart);
                pageSql.append(" order by row_number");

                tempSql = pageSql.toString();

            }
        } else if (dbType.equalsIgnoreCase("ORACLE")) {
            tempSql = "SELECT * FROM (" +
                    " SELECT A.*, ROWNUM ROWNUM_FOR_ORACLE " +
                    "   FROM (" + sql + ") A" +
                    " WHERE ROWNUM <=" + (pageStart + pageSize) +
                    ") WHERE ROWNUM_FOR_ORACLE >" + pageStart;
        } else {
            tempSql = sql;
        }
        return tempSql;
    }

    public static void main(String agr[]) {
        // System.out.println(AppendAbstractUtils.generatePageFragmentSql("SELECT idf,cff,gds,dgd from tables_tg,tafd  where idf=234 and fd='cdf' and ccc LIKE'%dd%'", 10, 2, "MSSQL"));
    }

    public static String getOrderBy(String sql, String orderBy) {
        if (StringUtil.isEmpty(orderBy)) {
            return STR_ORDERBY + " id desc ";
        }
        StringBuffer orderBuffer = new StringBuffer(30);
        String[] orderByArray = StringUtil.splitEx(orderBy, ",");
        for (int i = 0; i < orderByArray.length; i++) {
            String orderByItem = orderByArray[i].trim();
            String orderByName = null;
            String orderByDirect = "";
            if (orderByItem.indexOf(" ") == -1) {
                orderByName = orderByItem;
            } else {
                orderByName = orderByItem.substring(0, orderByItem.indexOf(" "));
                orderByDirect = orderByItem.substring(orderByItem.indexOf(" ") + 1);
            }
            if (orderByName.indexOf(".") > -1) {
                orderByName = orderByName.substring(orderByName.indexOf(".") + 1);
            }
            String columnAlias = orderByName + " as ";
            int columnIndex = sql.indexOf(columnAlias);
            if (columnIndex == -1) {
                orderBuffer.append(orderByName).append(" ").append(orderByDirect).append(" ,");
            } else {
                String after = sql.substring(columnIndex + columnAlias.length());
                String aliasName = null;
                if (after.indexOf(",") != -1 && after.indexOf(" from") > after.indexOf(",")) {
                    aliasName = after.substring(0, after.indexOf(","));
                } else {
                    aliasName = after.substring(0, after.indexOf(" "));
                }
                orderBuffer.append(aliasName).append(" ").append(orderByDirect).append(" ,");
            }
        }
        orderBuffer.deleteCharAt(orderBuffer.length() - 1);
        return STR_ORDERBY + orderBuffer.toString();
    }

    /**
     * 根据Model对象生成分页查询SQL语句<br/>
     *
     * @param tmb 数据库查询语句组合类
     * @return 生成的SQL语句
     */
    public static String generateSelectSql(Model tmb) {
        String sql;
        String dbType = ConnectionKit.getDevConf("DBType");
        int absPage = tmb.absPage == 0 ? 1 : tmb.absPage;
        int pageSize = tmb.pageSize;
        String table = tmb.table;
        String rf = (tmb.filter == null || tmb.filter.equals("")) ? "1=1" : tmb.filter;
//          sql codeselect top 10 * from table1
//          where id not in(select top 开始的位置 id from table1)
        int pageStart = (absPage - 1) * pageSize;//本页起始记录位置
        if (dbType.equalsIgnoreCase("MYSQL")) {
            sql = "SELECT " + tmb.fields + " FROM " + table + " WHERE " + rf;
            if (pageSize > 0) {
                sql += " LIMIT " + pageStart + ", " + pageSize;
            }
        } else if (dbType.equalsIgnoreCase("MSSQL")) {
            sql = "SELECT " + (pageSize > 0 ? "TOP " + pageSize + " " : "") + tmb.fields + " FROM " + table + " WHERE ";
            if (pageSize > 0 && absPage - 1 > 0) {
                String tb = table.indexOf(",") == -1 ? table : table.split(",")[0];
                if (tb.indexOf(" ") != -1) {
                    String[] tbsp = tb.split(" ");
                    tb = tbsp[tbsp.length - 1];
                }
                sql += "(" + tb + ".id NOT IN (SELECT TOP " + pageStart + " " + tb + ".id FROM " + table + " WHERE " + rf + ")) AND ";
            }
            sql += rf;
        } else if (dbType.equalsIgnoreCase("ORACLE")) {
            if (pageSize > 0) {
                /**
                 * SELECT * FROM
                 * (SELECT "NAVICAT_TABLE".*, ROWNUM "NAVICAT_ROWNUM"
                 * FROM ( SELECT "CROOT"."MAIN_COUNCIL_PER".*, ROWID "NAVICAT_ROWID" FROM "CROOT"."MAIN_COUNCIL_PER") "NAVICAT_TABLE"
                 * WHERE ROWNUM <= 1000) WHERE "NAVICAT_ROWNUM" > 0
                 * */
                sql = "SELECT * FROM (" +
                        " SELECT A.*, ROWNUM ROWNUM_FOR_ORACLE " +
                        "   FROM (SELECT " + tmb.fields + " FROM " + table + " WHERE " + rf + ") A" +
                        " WHERE ROWNUM <=" + (pageStart + pageSize) +
                        ") WHERE ROWNUM_FOR_ORACLE >" + pageStart;
            } else {
                sql = "SELECT " + tmb.fields + " FROM " + table + " WHERE " + rf;
            }
        } else {
            throw new TarnException("查询SQL生成错误,请检查配置文件设置是否正确.");
        }
        return sql;
    }

    /**
     * 根据Model对象生成分页查询带有分页的SQL语句<br/>
     *
     * @param model 数据库查询语句组合类
     * @return 生成的SQL语句
     */

    public static String generateSelectPageSql(Model model) {
        return generatePageFragmentSql(generateSelectSql(model), model.pageSize, model.absPage);
    }


}
