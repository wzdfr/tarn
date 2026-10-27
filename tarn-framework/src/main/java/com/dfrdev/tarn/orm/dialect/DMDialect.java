package com.dfrdev.tarn.orm.dialect;

/**
 * 达梦数据库 DM8 方言（国产数据库支持）。
 * 达梦兼容 MySQL 风格 LIMIT 语法与 Oracle 风格 ROWNUM，推荐使用 LIMIT。
 */
public class DMDialect implements Dialect {

    public String name() {
        return "DM";
    }

    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        // DM8 支持 LIMIT offset, size（兼容 MySQL 风格）
        return sql + " LIMIT " + offset + ", " + limit;
    }

    public String getIdentifierQuote() {
        return "\"";
    }

    public String getDefaultDriverClass() {
        return "dm.jdbc.driver.DmDriver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
