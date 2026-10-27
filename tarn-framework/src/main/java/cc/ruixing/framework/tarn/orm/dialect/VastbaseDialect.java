package cc.ruixing.framework.tarn.orm.dialect;

/**
 * 海量数据 Vastbase 方言（国产数据库支持）。
 * Vastbase G100 基于 PostgreSQL/openGauss 内核，分页语法一致。
 */
public class VastbaseDialect implements Dialect {

    public String name() {
        return "VASTBASE";
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
        return "com.vastdata.jdbc.Driver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
