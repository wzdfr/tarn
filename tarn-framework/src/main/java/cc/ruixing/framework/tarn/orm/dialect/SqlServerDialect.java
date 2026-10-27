package cc.ruixing.framework.tarn.orm.dialect;

/**
 * SQL Server 方言（2012+）：OFFSET/FETCH。
 * 老代码中 MSSQL 2000 的 TOP/NOT IN 分页保留在 AppendAbstractUtils 中兼容，
 * 新代码请统一走本方言。
 */
public class SqlServerDialect implements Dialect {

    public String name() {
        return "MSSQL";
    }

    /**
     * SQL Server 2012+ OFFSET/FETCH 要求必须有 ORDER BY，
     * 无 ORDER BY 时自动追加按常量排序的兜底。
     */
    public String getPagedSql(String sql, int offset, int limit) {
        if (limit <= 0) {
            return sql;
        }
        String body = sql;
        if (!body.toUpperCase().contains("ORDER BY")) {
            body = body + " ORDER BY (SELECT 0)";
        }
        return body + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    public String getIdentifierQuote() {
        return "\"";
    }

    public String getDefaultDriverClass() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }
}
