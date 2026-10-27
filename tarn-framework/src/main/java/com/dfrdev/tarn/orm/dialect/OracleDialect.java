package com.dfrdev.tarn.orm.dialect;

/**
 * Oracle 方言（11g 及之前，ROWNUM 三层嵌套分页）。
 * Oracle 12c+ 建议改用 {@link Oracle12cDialect}。
 */
public class OracleDialect implements Dialect {

    public String name() {
        return "ORACLE";
    }

    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        return "SELECT * FROM (SELECT A.*, ROWNUM ROWNUM_FOR_ORACLE FROM (" + sql
                + ") A WHERE ROWNUM <= " + (offset + limit)
                + ") WHERE ROWNUM_FOR_ORACLE > " + offset;
    }

    public String getIdentifierQuote() {
        return "\"";
    }

    public String getDefaultDriverClass() {
        return "oracle.jdbc.OracleDriver";
    }
}
