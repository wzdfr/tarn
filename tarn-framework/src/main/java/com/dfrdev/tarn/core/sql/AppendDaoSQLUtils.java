package com.dfrdev.tarn.core.sql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.db.DAO;
public class AppendDaoSQLUtils {
    /**
     * 生成插入SQL语句。
     *
     * @param d
     */
    public static String generateInsertSql(DAO d) {
        List useFields = d.getUseFields();
        if (useFields.size() == 0) return null;
        String sql = "INSERT INTO ";
        sql += d.getTableName() + " ";
        sql += "(";
        sql += join(useFields, null);
        sql += ")";
        sql += " VALUES (";
        sql += join(useFields, null, "#");
        sql += ") ";
        return sql;
    }

    /**
     * 生成插入SQL语句。
     *
     * @param tableName
     * @param fields
     */
    public static String generateInsertSql(String tableName, List fields) {
        //===生成sql
        if (fields.size() == 0) return null;
        String sql = "INSERT INTO ";
        sql += tableName + " ";
        sql += "(";
        sql += join(fields, null);
        sql += ")";
        sql += " VALUES (";
        sql += join(fields, null, "#");
        sql += ") ";
        return sql;
    }

    /**
     * 生成UPDATE SQL语句。
     *
     * @param tableName
     * @param fields
     * @param uk
     */
    public static String generateUpdateSql(String tableName, List fields, String uk) {
        //===生成sql
        if (fields.size() == 0) return null;
        //     String uk = d.getPrimKey();
        String sql = "UPDATE ";
        sql += tableName + " ";
        sql += "SET ";
        sql += joinUpdate(fields, null, uk);
        sql += " WHERE (";
        if (uk == null || uk.equals("")) sql += " id=#ID ";
        else sql += uk + "=#" + uk;
        sql += " )";
        return sql;
    }

    /**
     * 生成查询SQL语句，查询Object对象
     *
     * @deprecated 存在 SQL 注入风险（map 值直接拼接），请使用 {@link #generateSelectSqlParameterized}
     */
    @Deprecated
    public static String generateSELECTSqlObj(String tableName, String fields, Map map, String orderBy) {
        String f = (fields == null || fields.equals("")) ? "*" : fields;
        String sql = "SELECT " + f;
        sql += " FROM ";
        sql += tableName + " ";
        sql += " WHERE 1=1 ";
        Set<String> key = map.keySet();
        for (Iterator it = key.iterator(); it.hasNext(); ) {
            String k = (String) it.next();
            sql += " AND " + k + "='" + map.get(k) + "' ";
        }
        if (orderBy != null && !orderBy.equals("")) {
            sql += " ORDER BY " + orderBy;
        }
        //todo
        return sql;
    }

    /**
     * Tarn 2.0+: 参数化实体查询 SQL 生成（修复 generateSELECTSqlObj 的注入风险）。
     * 列名与 orderBy 走标识符白名单校验，值全部参数绑定。
     *
     * @return {@link com.dfrdev.tarn.core.sql.ParameterizedSql}，条件为空时参数列表为空
     */
    public static com.dfrdev.tarn.core.sql.ParameterizedSql generateSelectSqlParameterized(
            String tableName, String fields, Map<String, Object> map, String orderBy) {
        com.dfrdev.tarn.core.sql.SqlBuilder builder =
                com.dfrdev.tarn.core.sql.SqlBuilder.select(
                        (fields == null || fields.equals("")) ? "*" : fields).from(tableName);
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder(builder.build().getSql()).append(" WHERE 1=1");
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                // 列名标识符校验，防 key 注入
                if (!entry.getKey().matches("^[A-Za-z_][A-Za-z0-9_.]*$")) {
                    throw new IllegalArgumentException("非法查询列名: " + entry.getKey());
                }
                sql.append(" AND ").append(entry.getKey()).append(" = ?");
                params.add(entry.getValue());
            }
        }
        if (orderBy != null && !orderBy.equals("")) {
            // orderBy 白名单校验（允许 字段/表前缀/asc/desc/逗号）
            if (orderBy.matches("^[A-Za-z0-9_.,\\s]+(?i:(asc|desc))?$|^[A-Za-z_][A-Za-z0-9_.]*(\\s+(?i:asc|desc))?(\\s*,\\s*[A-Za-z_][A-Za-z0-9_.]*(\\s+(?i:asc|desc))?)*$")) {
                sql.append(" ORDER BY ").append(orderBy);
            }
        }
        return new com.dfrdev.tarn.core.sql.ParameterizedSql(sql.toString(), params);
    }

    private static void workByKeySet(Map<String, Object> map) {
        Set<String> key = map.keySet();
        for (Iterator it = key.iterator(); it.hasNext(); ) {
            String s = (String) it.next();
        }
    }

    /**
     * 生成删除语句
     */
    public static String generateDeleteSql(String tableName, List fields, String uk) {
        //===生成sql
        if (fields.size() == 0) return null;
        //     String uk = d.getPrimKey();
        String sql = "DELETE FROM ";
        sql += tableName + " ";
        sql += " WHERE ";
        sql += joinDelete(fields, null, uk);

        return sql;
    }

    /**
     * 生成删除语句
     */
    public static String generateDeleteSql(DAO d) {
        List useFields = d.getUseFields();
        if (useFields.size() == 0) return null;
        String sql = "DELETE FROM ";
        sql += d.getTableName() + " ";
        sql += " WHERE ";
        sql += joinDelete(useFields, null, null);
        return sql;
    }

    /**
     * 生成更新语句
     */
    public static String generateUpdateSql(DAO d) {
        List useFields = d.getUseFields();
        if (useFields.size() == 0) return null;
        String uk = d.getPrimKey();
        String sql = "UPDATE ";
        sql += d.getTableName() + " ";
        sql += "SET ";
        sql += joinUpdate(useFields, null, uk);
        sql += " WHERE (";
        if (uk == null) sql += " id=#ID ";
        else sql += uk + "=#" + uk;
        sql += " )";

        return sql;
    }

    /**
     * 将List 分隔为字符串。<br/>
     *
     * @param list    需要被操作的list
     * @param joinStr 分隔符，指明null 默认为逗号
     */
    public static String join(List list, String joinStr) {
        if (null == list) return null;
        if (null == joinStr) joinStr = ",";
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = list.size(); i < n; i++) {
            if (i > 0) sb.append(joinStr);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * 根据规范的包类名，获取类名
     *
     * @param clazPackageN
     */
    public static String className(String clazPackageN) {
        clazPackageN = clazPackageN.substring(clazPackageN.lastIndexOf(".") + 1, clazPackageN.length());
        return clazPackageN;
    }

    /**
     * 根据规范的包类名，获取类名
     *
     * @param clazPackageN
     */
    public static String classNameToupp(String clazPackageN) {
        clazPackageN = clazPackageN.substring(clazPackageN.lastIndexOf(".") + 1, clazPackageN.length());
        return clazPackageN.toUpperCase();
    }

    /**
     * 将List 分隔为字符串。<br/>
     *
     * @param list    需要被操作的list
     * @param joinStr 分隔符，指明null 默认为逗号
     * @param petn    分隔后的元素前面需要加的字符
     */
    public static String join(List list, String joinStr, String petn) {
        if (null == list) return null;
        if (null == joinStr) joinStr = ",";
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = list.size(); i < n; i++) {
            if (i > 0) sb.append(joinStr);
            sb.append(petn + list.get(i));
        }
        return sb.toString();
    }

    /**
     *
     * */
    private static String joinUpdate(List list, String joinStr, String primKey) {
        if (null == list) return null;
        if (null == joinStr) joinStr = ",";
        String idkey = primKey == null ? "ID" : primKey;
        StringBuffer sb = new StringBuffer();

        for (int i = 0, n = list.size(); i < n; i++) {
            if (!list.get(i).equals(idkey)) {
                sb.append(joinStr);
                sb.append(list.get(i) + "=#" + list.get(i));
            }
        }
        return sb.toString().replaceFirst(joinStr, "");
    }

    private static String join(String str, String addStr, String joinStr) {
        if (str == null || str.equals("")) return null;
        if (null == addStr) addStr = "#";
        if (null == joinStr) joinStr = ",";
        return str.replaceAll(joinStr, joinStr + addStr);
    }

    /**
     * 将String[] 分隔为字符串。<br/>
     *
     * @param arr     需要被操作的数组
     * @param joinStr 分隔符，指明null 默认为逗号
     */
    private static String join(String[] arr, String joinStr) {
        if (null == arr) return null;
        if (null == joinStr) joinStr = ",";
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = arr.length; i < n; i++) {
            if (i > 0) sb.append(joinStr);
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static String joinDelete(List list, String joinStr, String primKey) {
        if (null == list) return null;
        if (null == joinStr) joinStr = " and ";
        String idkey = primKey == null ? "ID" : primKey;
        StringBuffer sb = new StringBuffer();

        for (int i = 0, n = list.size(); i < n; i++) {
            //  if (!list.get(i).equals(idkey)) {
            sb.append(joinStr);
            sb.append(list.get(i) + "=#" + list.get(i));
            // }

        }
        return sb.toString().replaceFirst(joinStr, "");
    }
}
