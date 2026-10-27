package cc.ruixing.framework.tarn.orm.dialect;

/**
 * 人大金仓 KingbaseES 方言（国产数据库支持）。
 * KingbaseES 基于 PostgreSQL 内核，分页语法与 PostgreSQL 一致。
 */
public class KingbaseDialect implements Dialect {

    public String name() {
        return "KINGBASE";
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
        return "com.kingbase8.Driver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
