package com.dfrdev.tarn.core.sql;

import java.util.Collections;
import java.util.List;

/**
 * 参数化 SQL 载体（Tarn 2.0+）。
 * <p>
 * 由 {@link SqlBuilder} 构建产出：最终 SQL 已将 #{name} 命名占位与 ? 位置占位
 * 统一替换为 PreparedStatement 的 ?，参数按出现顺序排列。
 *
 * @author dfr
 * @see SqlBuilder
 * @since 2.0
 */
public class ParameterizedSql {

    private final String sql;
    private final List<Object> params;

    public ParameterizedSql(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params == null ? Collections.<Object>emptyList() : Collections.unmodifiableList(params);
    }

    /**
     * 最终 SQL（占位符全部为 ?）
     */
    public String getSql() {
        return sql;
    }

    /**
     * 与 ? 顺序对应的参数列表
     */
    public List<Object> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "ParameterizedSql{sql='" + sql + "', params=" + params + '}';
    }
}
