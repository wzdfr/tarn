package cc.ruixing.framework.tarn.orm.dialect;

/**
 * MySQL / MariaDB 方言：LIMIT offset, size
 */
public class MySQLDialect implements Dialect {

    public String name() {
        return "MYSQL";
    }

    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        return sql + " LIMIT " + offset + ", " + limit;
    }

    public String getIdentifierQuote() {
        return "`";
    }

    public String getDefaultDriverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    public boolean supportsLimitOffset() {
        return true;
    }
}
