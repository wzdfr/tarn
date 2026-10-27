package com.dfrdev.tarn.plugin.orm;

import com.dfrdev.tarn.aop.annotation.Etable;
import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.core.AppendDaoSQLUtils;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dfr on 15-12-29.
 */
public class QueryBuffer implements SqlBuffer, Cloneable {

    private StringBuffer sql = new StringBuffer();
    private Mapx<String, Object> params = null;

    private SqlType sqlType = SqlType.SELECT;

    private String countField = null;

    private String countSql = null;

    public QueryBuffer() {
    }

    public QueryBuffer(String sql) {
        this.sql.append(sql);
    }

    /**
     * 添加子句
     */
    public QueryBuffer append(String s) {
        sql.append(s);
        return this;
    }

    public QueryBuffer and() {
        sql.append(" and ");
        return this;
    }

    public QueryBuffer not() {
        sql.append(" not ");
        return this;
    }

    public QueryBuffer or() {
        sql.append(" or ");
        return this;
    }

    public QueryBuffer comma() {
        this.sql.append(',');
        return this;
    }

    public QueryBuffer space() {
        sql.append(' ');
        return this;
    }

    public QueryBuffer count(String field) {
        verifyName(field);
        sql.append(" count(");
        sql.append(field);
        sql.append(')');
        return this;
    }

    public QueryBuffer sum(String field) {
        verifyName(field);
        sql.append(" sum(");
        sql.append(field);
        sql.append(')');
        return this;
    }

    public QueryBuffer avg(String field) {
        verifyName(field);
        sql.append(" avg(");
        sql.append(field);
        sql.append(')');
        return this;
    }


    public QueryBuffer from(String[] tables) {
        this.sql.append(" FROM ");
        boolean first = true;
        for (String table : tables) {
            verifyName(table);
            if (first) {
                this.sql.append(" ");
                first = false;
            } else {
                this.sql.append(",");
            }
            this.sql.append(table);
        }
        return this;
    }

    public QueryBuffer from(String table) {
        verifyName(table);
        sql.append(" FROM ");
        this.sql.append(table);
        return this;
    }

    public QueryBuffer orderby(String orderby) {
        if ((orderby == null) || ((orderby = orderby.trim()).length() == 0)) {
            return this;
        }
        int last = 0;
        boolean nameFlag = true;
        for (int i = 0; i < orderby.length(); i++) {
            char c = orderby.charAt(i);
            if (c == ' ') {
                if (!nameFlag) {
                    throw new TarnException("Maybe a SQL Injection: orderby=" + orderby);
                }
                verifyName(orderby.substring(last, i));
                last = i + 1;
                nameFlag = false;
            } else if (c == ',') {
                String str = orderby.substring(last, i).trim();
                if (nameFlag) {
                    verifyName(str);
                } else if ((str.equals("")) || (str.equalsIgnoreCase("asc")) || (str.equalsIgnoreCase("desc"))) {
                    last = i + 1;
                }

                for (; (i + 1 < orderby.length()) && (orderby.charAt(i + 1) == ' ');
                     last = i)
                    i++;
                nameFlag = true;
            }
        }
        if (nameFlag) {
            verifyName(orderby.substring(last));
        } else {
            String order = orderby.substring(last);
            if ((!order.equals("")) && (!order.equalsIgnoreCase("asc")) && (!order.equalsIgnoreCase("desc"))) {
                throw new TarnException("Maybe a SQL Injection: orderby=" + orderby);
            }
        }
        sql.append(" order by " + orderby);
        return this;
    }

    /**
     * 统计字段
     */
    public QueryBuffer countField(String field) {
        countField = field;
        return this;
    }

    public QueryBuffer countSql(String sql) {
        if (StringUtil.isNotEmpty(sql)) countSql = sql;
        return this;
    }

    /**
     * 获取统计数量SQL
     */
    @Override
    public String getCountSql() {
        String sql = getSql();
        if (StringUtil.isNotEmpty(countField)) {
            Pattern pattern = Pattern.compile("SELECT([\\s\\S]*?)FROM", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sql);
            while (matcher.find()) {
                sql = sql.replaceAll(matcher.group().replaceAll("\\*", "\\\\*"), "SELECT " + countField + " FROM ");
            }
            sql = sql.replaceAll("(?i)order by(.*)", "");
        } else if (StringUtil.isNotEmpty(countSql)) {
            sql = this.countSql;
        } else {
            //如果都没有设置count SQL.重置为null,防止使用时没有设置造成异常查询
            sql = null;
        }
        return sql;
    }

    /**
     * QueryBuffer 合并
     */
    public QueryBuffer addPart(QueryBuffer wherePart) {
        if (wherePart == null) {
            return this;
        }
        this.sql.append(wherePart.getSql());
        initParams();
        params.putAll(wherePart.getParams());
        return this;
    }

    public QueryBuffer between(String field, Object start, Object end) {
        this.sql.append(" ");
        verifyName(field);
        this.sql.append(field);
        initParams();
        sql.append(" between #" + field + "1 and #" + field + "2");
        params.put(field + "1", start);
        params.put(field + "2", end);

        return this;
    }

    /**
     * 设置参数。
     *
     * @param param
     * @param value
     * @return
     */
    public QueryBuffer setParam(String param, Object value) {
        initParams();
        verifyName(param);
        params.put(param, value);
        return this;
    }

    public QueryBuffer setParamLike(String param, String value) {
        initParams();
        verifyName(param);
        params.put(param, "%" + value + "%");
        return this;
    }


    public QueryBuffer setParamLikeLeft(String param, String value) {
        initParams();
        verifyName(param);
        params.put(param, "%" + value);
        return this;
    }

    public QueryBuffer setParamLikeRight(String param, String value) {
        initParams();
        verifyName(param);
        params.put(param, value + "%");
        return this;
    }

    public QueryBuffer where(String s) {
        initParams();
        sql.append(" WHERE ");
        if (StringUtil.isNotEmpty(s)) sql.append(s);
        else sql.append("1=1");
        return this;
    }


    public static void main(String[] args) {

        QueryBuffer qb=new QueryBuffer("select * FROM sss where aaa=23434");
        qb.countField("count(id)");
        System.out.println("countsql:" + qb.getCountSql());

    }


    public QueryBuffer where(String s, Object[]... paras) {
        sql.append(" WHERE ");
        if (StringUtil.isNotEmpty(s)) sql.append(s);
        else sql.append("1=1");
        initParams();
        if (paras != null) {

            for (int i = 0; i < paras.length; i++) {
                Object[] obj = paras[i];
                if (obj != null && obj.length > 0) {
                    String k = (String) obj[0];
                    verifyName(k);
                    params.put(k, obj[1]);
                }
            }
        }
        return this;
    }

    public QueryBuffer where(String s, String key, Object value) {
        sql.append(" WHERE ");
        if (StringUtil.isNotEmpty(s)) sql.append(s);
        else sql.append("1=1");
        initParams();
        verifyName(key);
        params.put(key, value);
        return this;
    }


    public QueryBuffer field(String f) {
        return field(f, false);
    }


    public QueryBuffer field(String f, boolean split) {
        if (StringUtil.isEmpty(sql.toString())) sql.append("SELECT ");
        if (split) sql.append(",");
        sql.append(f);
        return this;
    }


    /**
     * 根据 Entity(分表)或者Class返回更新语句的Finder
     * 等同 getUpdateFinder(User.class,"id,name")==return new QueryBuffer("SELECT id,name FROM t_User ");
     *
     * @return
     * @throws Exception
     */

    public static QueryBuffer getSelectBuffer(Class clazz, String selectField) {
        if (clazz == null) {
            return null;
        }
        if (StringUtil.isEmpty(selectField)) {
            selectField = "*";
        }
        String tableName = getTableName(clazz);
        QueryBuffer queryBuffer = new QueryBuffer("SELECT ");
        queryBuffer.append(selectField).append(" FROM ").append(tableName).append(" ");
        return queryBuffer;
    }


    public static QueryBuffer getSelectBuffer(String tableName, String selectField) {
        if (StringUtil.isEmpty(tableName)) {
            return null;
        }
        if (StringUtil.isEmpty(selectField)) {
            selectField = "*";
        }
        QueryBuffer queryBuffer = new QueryBuffer("SELECT ");
        queryBuffer.append(selectField).append(" FROM ").append(tableName).append(" ");
        return queryBuffer;
    }


    /**
     * 根据 Entity(分表)或者Class返回更新语句的Finder
     *
     * @param clazz
     * @return
     * @throws Exception
     */
    public static QueryBuffer getUpdateBuffer(Class clazz) {
        return getUpdateBuffer(clazz, null);
    }

    /**
     * 根据 Entity(分表)或者Class返回更新语句的QueryBuffer
     * <p/>
     * 等同 getUpdateFinder(User.class,"name=:name")==return new QueryBuffer("UPDATE t_User SET name=#name ");
     *
     * @param clazz,updatesql
     * @return
     * @throws Exception
     */

    public static QueryBuffer getUpdateBuffer(Class clazz, String updatesql) {
        if (clazz == null) {
            return null;
        }
        String tableName = getTableName(clazz);
        QueryBuffer finder = new QueryBuffer("UPDATE ");
        finder.append(tableName).append(" SET ");
        if (StringUtil.isNotEmpty(updatesql)) {
            finder.append(updatesql).append(" ");
        }
        return finder;
    }

    /**
     * 根据 Entity(分表)或者Class返回删除语句的Finder
     * <p/>
     * 等同 getDeleteBuffer(User.class)==return new QueryBuffer("DELETE FROM  t_user ");
     *
     * @param clazz
     * @return
     * @throws Exception
     */

    public static QueryBuffer getDeleteBuffer(Class clazz) {
        if (clazz == null) {
            return null;
        }
        String tableName = getTableName(clazz);
        QueryBuffer finder = new QueryBuffer("DELETE FROM  ");
        finder.append(tableName).append(" ");
        return finder;
    }

    /**
     * 插入语句生成
     */
    public QueryBuffer insertInto(String table, String[] columns, Object[] values) {
        if ((columns.length == 0) || (columns.length != values.length)) {
            throw new TarnException("Insert into failed:values's length is not equal columns.length!");
        }
        sqlType = SqlType.INSERT;
        this.sql.append("insert into ");
        this.sql.append(table);
        this.sql.append(" (");
        initParams();
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                this.sql.append(",");
            }
            this.sql.append(columns[i]);
        }
        this.sql.append(") values (");
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                this.sql.append(",");
            }
            sql.append("#" + columns[i]);
            params.put(columns[i], values[i]);
        }
        this.sql.append(")");
        return this;
    }

    public static String getTableName(Class clazz) {
        String tableName = AppendDaoSQLUtils.classNameToupp(clazz.getName());
        boolean hasAnnotation = clazz.isAnnotationPresent(Etable.class);
        if (hasAnnotation) {
            Etable ann = (Etable) clazz.getAnnotation(Etable.class);
            tableName = ann.value();
        }
        return tableName;
    }

    @Override
    public String getSql() {
        if (sql == null)
            return null;
        return sql.toString();
    }

    @Override
    public void setSql(String sql) {
        this.sql = new StringBuffer(sql);
    }

    /**
     * 当前SQL类型
     */
    @Override
    public SqlType sqlType() {
        return sqlType;
    }

    private void verifyName(String field) {
        int i1 = field.indexOf('(');
        if (i1 > 0) {
            int i2 = field.indexOf(' ', i1);
            if (i2 > 0) {
                throw new TarnException("Maybe a SQL Injection: name=" + field);
            }
        } else if (field.indexOf(' ') > 0) {
            String[] arr = field.split("\\s");
            if (arr.length > 3)
                throw new TarnException("Maybe a SQL Injection: name=" + field);
            if ((arr.length == 3) && (!arr[1].equalsIgnoreCase("as")))
                throw new TarnException("Maybe a SQL Injection: name=" + field);
        }
    }


    @Override
    public Object clone() {
        QueryBuffer queryBuffer = null;
        try {
            queryBuffer = (QueryBuffer) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return queryBuffer;
    }

    /**
     * 获取参数集合
     */
    @Override
    public Mapx<String, Object> getParams() {
        return params;
    }

    private void initParams() {
        if (params == null)
            params = new Mapx<String, Object>();
    }

}
