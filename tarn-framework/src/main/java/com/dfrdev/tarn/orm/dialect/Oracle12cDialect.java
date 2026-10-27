package com.dfrdev.tarn.orm.dialect;

/**
 * Oracle 12c+ 方言：OFFSET ... FETCH NEXT（比 ROWNUM 嵌套更高效）
 */
public class Oracle12cDialect extends OracleDialect {

    @Override
    public String name() {
        return "ORACLE12C";
    }

    @Override
    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
}
