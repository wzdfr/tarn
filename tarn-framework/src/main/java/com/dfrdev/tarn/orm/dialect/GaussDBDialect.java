package com.dfrdev.tarn.orm.dialect;

/**
 * 华为 GaussDB / openGauss 方言（国产数据库支持）。
 * 基于 PostgreSQL 内核，分页语法一致。
 */
public class GaussDBDialect implements Dialect {

    public String name() {
        return "GAUSSDB";
    }

    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    public String getIdentifierQuote() {
        return "\"";
    }

    public String getDefaultDriverClass() {
        return "org.opengauss.Driver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
