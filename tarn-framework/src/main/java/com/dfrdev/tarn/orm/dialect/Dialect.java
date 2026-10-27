package com.dfrdev.tarn.orm.dialect;

/**
 * 数据库方言接口（Tarn 2.0+）。
 * <p>
 * 统一抽象各数据库的分页、标识符引用等差异，替代老代码中
 * {@code DBType} if/else 硬编码分支。支持国产数据库：达梦(DM)、
 * 人大金仓(Kingbase)、GaussDB、海量(Vastbase)等。
 *
 * @author dfr
 * @since 2.0
 */
public interface Dialect {

    /**
     * 方言名称（与配置值对应，如 MYSQL / DM / KINGBASE）
     */
    String name();

    /**
     * 生成分页 SQL
     *
     * @param sql    原始 SQL（不含分页）
     * @param offset 起始行（从 0 开始）
     * @param limit  每页条数；&lt;=0 表示不分页
     * @return 带分页的 SQL
     */
    String getPagedSql(String sql, int offset, int limit);

    /**
     * 标识符引用符（如 MySQL 反引号、Oracle 双引号），null 表示不引用
     */
    default String getIdentifierQuote() {
        return null;
    }

    /**
     * 引用标识符
     */
    default String quote(String identifier) {
        String quote = getIdentifierQuote();
        return quote == null ? identifier : quote + identifier + quote;
    }

    /**
     * 默认 JDBC Driver 类名
     */
    default String getDefaultDriverClass() {
        return null;
    }

    /**
     * 是否支持 LIMIT/OFFSET 语法
     */
    default boolean supportsLimitOffset() {
        return false;
    }
}
