package com.dfrdev.tarn.orm.dialect;

/**
 * PostgreSQL 方言：LIMIT size OFFSET offset
 */
public class PostgreSQLDialect implements Dialect {

    public String name() {
        return "POSTGRESQL";
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
        return "org.postgresql.Driver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
