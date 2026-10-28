package com.dfrdev.tarn.core.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.dfrdev.tarn.core.boot.Tarn;
/**
 * MyBatis 风格链式 SQL 构建器（Tarn 2.0+）。
 * <p>
 * 同时支持两种占位符，构建时统一转为 PreparedStatement 的 ?：
 * <ul>
 *     <li>位置占位 {@code ?}：配合 append/where 的可变参数，按顺序绑定</li>
 *     <li>命名占位 {@code #{name}}：配合 {@link #bind(String, Object)}，按名取值</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *     ParameterizedSql ps = SqlBuilder.create("SELECT * FROM sys_users")
 *             .where("status = #{status}").bind("status", 1)
 *             .and("age > ?", 18)
 *             .orderBy("id", false)
 *             .limit(0, 20)
 *             .build();
 *     List&lt;?&gt; list = db.query(ps, rowMapper);
 * </pre>
 * 安全设计：
 * <ul>
 *     <li>值一律走参数绑定，杜绝 SQL 注入</li>
 *     <li>orderBy/groupBy 仅允许标识符白名单字符（字母数字下划线点、asc/desc）</li>
 * </ul>
 *
 * @author dfr
 * @see ParameterizedSql
 * @since 2.0
 */
public class SqlBuilder {

    /**
     * 标识符/排序片段白名单：字段名（可带表前缀 a.b）、方向 asc|desc、多列逗号分隔
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?"
                    + "(\\s+(?i:asc|desc))?"
                    + "(\\s*,\\s*[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?(\\s+(?i:asc|desc))?)*$");

    private final StringBuilder sql;
    private final List<Object> positionalParams = new ArrayList<Object>();
    private final Map<String, Object> namedParams = new HashMap<String, Object>();
    private String limitClause = null;

    private SqlBuilder(String initial) {
        this.sql = new StringBuilder(initial == null ? "" : initial.trim());
    }

    public static SqlBuilder create(String initialSql) {
        return new SqlBuilder(initialSql);
    }

    public static SqlBuilder select(String columns) {
        return new SqlBuilder("SELECT " + (columns == null || columns.trim().isEmpty() ? "*" : columns.trim()));
    }

    public SqlBuilder from(String table) {
        checkIdentifier(table, "table");
        sql.append(" FROM ").append(table.trim());
        return this;
    }

    /**
     * 追加任意 SQL 片段（可携带 ? 位置参数）
     */
    public SqlBuilder append(String fragment, Object... positionalBinds) {
        if (fragment != null && !fragment.trim().isEmpty()) {
            if (sql.length() > 0) {
                sql.append(' ');
            }
            sql.append(fragment.trim());
            for (Object bind : positionalBinds) {
                positionalParams.add(bind);
            }
        }
        return this;
    }

    /**
     * WHERE 条件（首次调用）。条件中的值请使用 ? 或 #{name} 占位，不要拼接字面量
     */
    public SqlBuilder where(String condition, Object... positionalBinds) {
        sql.append(" WHERE ").append(condition.trim());
        for (Object bind : positionalBinds) {
            positionalParams.add(bind);
        }
        return this;
    }

    /**
     * AND 条件
     */
    public SqlBuilder and(String condition, Object... positionalBinds) {
        sql.append(" AND ").append(condition.trim());
        for (Object bind : positionalBinds) {
            positionalParams.add(bind);
        }
        return this;
    }

    /**
     * OR 条件
     */
    public SqlBuilder or(String condition, Object... positionalBinds) {
        sql.append(" OR ").append(condition.trim());
        for (Object bind : positionalBinds) {
            positionalParams.add(bind);
        }
        return this;
    }

    /**
     * 命名参数绑定，配合 #{name} 占位使用
     */
    public SqlBuilder bind(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    /**
     * 排序（白名单校验，防注入）。支持 "id desc" 或 "a.name asc, b.id desc"
     */
    public SqlBuilder orderBy(String column) {
        checkIdentifier(column, "orderBy column");
        sql.append(" ORDER BY ").append(column.trim());
        return this;
    }

    /**
     * 排序便捷方法
     */
    public SqlBuilder orderBy(String column, boolean asc) {
        checkIdentifier(column, "orderBy column");
        sql.append(" ORDER BY ").append(column.trim()).append(asc ? " ASC" : " DESC");
        return this;
    }

    /**
     * 分组（白名单校验）
     */
    public SqlBuilder groupBy(String columns) {
        checkIdentifier(columns, "groupBy columns");
        sql.append(" GROUP BY ").append(columns.trim());
        return this;
    }

    /**
     * 通用 HAVING
     */
    public SqlBuilder having(String condition, Object... positionalBinds) {
        sql.append(" HAVING ").append(condition.trim());
        for (Object bind : positionalBinds) {
            positionalParams.add(bind);
        }
        return this;
    }

    /**
     * 追加 LIMIT。offset &lt; 0 表示不指定起始位置
     */
    public SqlBuilder limit(int offset, int size) {
        if (offset < 0) {
            limitClause = " LIMIT " + size;
        } else {
            limitClause = " LIMIT " + offset + ", " + size;
        }
        return this;
    }

    /**
     * 构建最终的参数化 SQL。#{name} 必须已通过 bind() 绑定，否则抛出 IllegalArgumentException
     */
    public ParameterizedSql build() {
        List<Object> ordered = new ArrayList<Object>();
        StringBuilder out = new StringBuilder();

        String body = sql.toString();
        if (limitClause != null) {
            body = body + limitClause;
        }

        int i = 0;
        int paramIndex = 0;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '#' && i + 1 < body.length() && body.charAt(i + 1) == '{') {
                int end = body.indexOf('}', i);
                if (end < 0) {
                    throw new IllegalArgumentException("SqlBuilder: unclosed #{ placeholder: " + body);
                }
                String name = body.substring(i + 2, end).trim();
                if (!namedParams.containsKey(name)) {
                    throw new IllegalArgumentException("SqlBuilder: named param not bound: #{" + name + "}");
                }
                out.append('?');
                ordered.add(namedParams.get(name));
                i = end + 1;
            } else if (c == '?') {
                if (paramIndex >= positionalParams.size()) {
                    throw new IllegalArgumentException(
                            "SqlBuilder: no bind value for ? at index " + paramIndex + " in: " + body);
                }
                out.append('?');
                ordered.add(positionalParams.get(paramIndex++));
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        if (paramIndex < positionalParams.size()) {
            throw new IllegalArgumentException(
                    "SqlBuilder: " + (positionalParams.size() - paramIndex) + " bind value(s) unused in: " + body);
        }
        return new ParameterizedSql(out.toString(), ordered);
    }

    /**
     * 标识符白名单校验（表名/字段名/排序），杜绝标识符位置注入
     */
    private static void checkIdentifier(String identifier, String what) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier.trim()).matches()) {
            throw new IllegalArgumentException("SqlBuilder: illegal " + what + ": " + identifier);
        }
    }
}
