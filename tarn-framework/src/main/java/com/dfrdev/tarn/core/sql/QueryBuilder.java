package com.dfrdev.tarn.core.sql;

import com.dfrdev.tarn.orm.dialect.DialectFactory;

import java.util.regex.Pattern;

/**
 * 查询构建器（Tarn 2.0+）：在 {@link SqlBuilder} 基础上支持多表 JOIN 与方言分页。
 * <p>
 * 多表关联查询示例：
 * <pre>
 *     ParameterizedSql ps = QueryBuilder.select("u.id, u.name, r.name AS role_name")
 *             .from("sys_users u")
 *             .leftJoin("sys_roles r").on("u.role_id = r.id")
 *             .where("u.status = #{status}").bind("status", 1)
 *             .orderBy("u.id", false)
 *             .page(1, 20)   // 自动按当前数据库方言生成分页
 *             .build();
 *     List&lt;Map&lt;String, Object&gt;&gt; rows = db.queryMaps(ps);
 * </pre>
 * JOIN 的表名/字段名走标识符白名单校验，值一律参数绑定。
 *
 * @author dfr
 * @since 2.0
 */
public class QueryBuilder {

    private static final Pattern TABLE_REF = Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_]*(\\s+(AS\\s+)?[A-Za-z_][A-Za-z0-9_]*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_CONDITION = Pattern.compile(
            "^[A-Za-z0-9_.\\s=<>!&|'\"?#{}()%-]+$");

    private final SqlBuilder delegate;
    private Integer pageOffset;
    private Integer pageSize;

    private QueryBuilder(String columns) {
        this.delegate = SqlBuilder.select(columns);
    }

    public static QueryBuilder select(String columns) {
        return new QueryBuilder(columns);
    }

    public QueryBuilder from(String table) {
        checkTableRef(table);
        delegate.from(table);
        return this;
    }

    // ---------- JOIN 支持（多表关联查询） ----------

    public QueryBuilder join(String table) {
        return appendJoin("JOIN", table);
    }

    public QueryBuilder leftJoin(String table) {
        return appendJoin("LEFT JOIN", table);
    }

    public QueryBuilder rightJoin(String table) {
        return appendJoin("RIGHT JOIN", table);
    }

    public QueryBuilder innerJoin(String table) {
        return appendJoin("INNER JOIN", table);
    }

    private QueryBuilder appendJoin(String joinType, String table) {
        checkTableRef(table);
        delegate.append(joinType + " " + table.trim());
        return this;
    }

    /**
     * JOIN ON 条件。条件中只允许 SQL 运算符/标识符/占位符字符，杜绝注入
     */
    public QueryBuilder on(String condition, Object... positionalBinds) {
        if (condition == null || !ON_CONDITION.matcher(condition.trim()).matches()) {
            throw new IllegalArgumentException("QueryBuilder: illegal ON condition: " + condition);
        }
        delegate.append("ON " + condition.trim(), positionalBinds);
        return this;
    }

    // ---------- 条件 / 绑定（透传 SqlBuilder） ----------

    public QueryBuilder where(String condition, Object... binds) {
        delegate.where(condition, binds);
        return this;
    }

    public QueryBuilder and(String condition, Object... binds) {
        delegate.and(condition, binds);
        return this;
    }

    public QueryBuilder or(String condition, Object... binds) {
        delegate.or(condition, binds);
        return this;
    }

    public QueryBuilder bind(String name, Object value) {
        delegate.bind(name, value);
        return this;
    }

    public QueryBuilder orderBy(String column, boolean asc) {
        delegate.orderBy(column, asc);
        return this;
    }

    public QueryBuilder groupBy(String columns) {
        delegate.groupBy(columns);
        return this;
    }

    public QueryBuilder having(String condition, Object... binds) {
        delegate.having(condition, binds);
        return this;
    }

    /**
     * 方言分页（absPage 从 1 开始）。替代老 {@code LIMIT} 硬编码，
     * 自动适配 MySQL/Oracle/PG/达梦/人大金仓/GaussDB 等。
     */
    public QueryBuilder page(int absPage, int size) {
        int page = absPage <= 0 ? 1 : absPage;
        this.pageOffset = (page - 1) * size;
        this.pageSize = size;
        return this;
    }

    /**
     * 构建参数化 SQL。设置过 {@link #page(int, int)} 时自动应用当前数据库方言分页
     */
    public ParameterizedSql build() {
        ParameterizedSql inner = delegate.build();
        if (pageSize != null) {
            String pagedSql = DialectFactory.paged(inner.getSql(), pageOffset, pageSize);
            return new ParameterizedSql(pagedSql, inner.getParams());
        }
        return inner;
    }

    private static void checkTableRef(String table) {
        if (table == null || !TABLE_REF.matcher(table.trim()).matches()) {
            throw new IllegalArgumentException("QueryBuilder: illegal table reference: " + table);
        }
    }
}
