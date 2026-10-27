package com.dfrdev.tarn.orm;

import com.dfrdev.tarn.core.sql.ParameterizedSql;
import com.dfrdev.tarn.json.JsonKit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 实体写操作 SQL 生成器（Tarn 2.1+）。
 * <p>
 * 职责单一：只把实体对象翻译成参数化 SQL（{@link ParameterizedSql}），不接触连接与事务。
 * 与老实现（{@code AppendDaoSQLUtils.generateInsertSql/generateUpdateSql} + {@code #name} 占位）
 * 的关键差异：
 * <ul>
 *     <li>INSERT 时自增主键不写入；非自增主键写入</li>
 *     <li>UPDATE 的 WHERE 只认主键，SET 中不含主键（老实现会生成 {@code SET ID=?}）</li>
 *     <li>DELETE 只按主键删除（老实现用“所有非空字段”拼 WHERE，主键为空时会误删）</li>
 *     <li>UPDATE 默认跳过 null 字段，{@code @EUpdateNull(true)} 字段参与更新</li>
 *     <li>一律使用 {@code ?} 顺序绑定，不再依赖 {@code #name} 文本替换</li>
 * </ul>
 *
 * @author dfr
 * @since 2.1
 */
public final class EntitySql {

    /**
     * 表名/列名白名单：允许字母下划线开头，可带 schema 前缀
     */
    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*)?$");

    private EntitySql() {
    }

    /**
     * 生成 INSERT 语句
     */
    public static ParameterizedSql insert(Object entity) {
        EntityMapper.Meta meta = requireMeta(entity);
        StringBuilder columns = new StringBuilder();
        StringBuilder holders = new StringBuilder();
        List<Object> params = new ArrayList<Object>();

        for (EntityMapper.Column col : meta.columns) {
            if (col.primaryKey && col.pkAutoIncrement) {
                continue; // 自增主键交给数据库生成
            }
            appendColumn(columns, holders, params, col, EntityMapper.readValue(col, entity));
        }
        if (params.isEmpty()) {
            throw new IllegalArgumentException("实体没有任何可写入字段: " + meta.type.getName());
        }
        String sql = "INSERT INTO " + table(meta) + " (" + columns + ") VALUES (" + holders + ")";
        return new ParameterizedSql(sql, params);
    }

    /**
     * 按主键生成 UPDATE 语句
     */
    public static ParameterizedSql update(Object entity) {
        EntityMapper.Meta meta = requireMeta(entity);
        EntityMapper.Column pk = requirePrimaryKey(meta);
        Object pkValue = EntityMapper.readValue(pk, entity);
        if (pkValue == null) {
            throw new IllegalArgumentException("更新缺少主键值: " + meta.type.getName()
                    + "#" + pk.columnName);
        }

        StringBuilder set = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        for (EntityMapper.Column col : meta.columns) {
            if (col.primaryKey) {
                continue; // 主键只进 WHERE，不进 SET
            }
            Object value = EntityMapper.readValue(col, entity);
            if (value == null && !col.updateIncludeNull) {
                continue; // 默认不更新 null 字段
            }
            if (set.length() > 0) {
                set.append(", ");
            }
            set.append(columnName(col)).append(" = ?");
            params.add(serialize(col, value));
        }
        if (params.isEmpty()) {
            throw new IllegalArgumentException("更新没有任何非空字段（可用 @EUpdateNull(true) 声明允许写 null）: "
                    + meta.type.getName());
        }
        params.add(serialize(pk, pkValue));
        String sql = "UPDATE " + table(meta) + " SET " + set
                + " WHERE " + columnName(pk) + " = ?";
        return new ParameterizedSql(sql, params);
    }

    /**
     * 按主键生成 DELETE 语句
     */
    public static ParameterizedSql deleteByPrimaryKey(Object entity) {
        EntityMapper.Meta meta = requireMeta(entity);
        EntityMapper.Column pk = requirePrimaryKey(meta);
        Object pkValue = EntityMapper.readValue(pk, entity);
        if (pkValue == null) {
            throw new IllegalArgumentException("删除缺少主键值: " + meta.type.getName()
                    + "#" + pk.columnName);
        }
        List<Object> params = new ArrayList<Object>();
        params.add(serialize(pk, pkValue));
        String sql = "DELETE FROM " + table(meta) + " WHERE " + columnName(pk) + " = ?";
        return new ParameterizedSql(sql, params);
    }

    private static void appendColumn(StringBuilder columns, StringBuilder holders,
                                      List<Object> params, EntityMapper.Column col,
                                      Object value) {
        if (columns.length() > 0) {
            columns.append(", ");
            holders.append(", ");
        }
        columns.append(columnName(col));
        holders.append('?');
        params.add(serialize(col, value));
    }

    private static Object serialize(EntityMapper.Column col, Object value) {
        if (value != null && col.jsonSerialize) {
            return JsonKit.toJson(value);
        }
        return value;
    }

    private static EntityMapper.Meta requireMeta(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为 null");
        }
        return EntityMapper.meta(entity.getClass());
    }

    private static EntityMapper.Column requirePrimaryKey(EntityMapper.Meta meta) {
        if (meta.primaryKeyColumn == null) {
            throw new IllegalArgumentException("实体未声明主键 @Ekey: " + meta.type.getName());
        }
        return meta.primaryKeyColumn;
    }

    private static String table(EntityMapper.Meta meta) {
        return safe(meta.tableName, "table");
    }

    private static String columnName(EntityMapper.Column col) {
        return safe(col.columnName, "column");
    }

    private static String safe(String identifier, String what) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("非法 " + what + " 名称: " + identifier);
        }
        return identifier;
    }
}
