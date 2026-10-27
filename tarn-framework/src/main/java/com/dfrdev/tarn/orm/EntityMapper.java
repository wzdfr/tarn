package com.dfrdev.tarn.orm;

import com.dfrdev.tarn.aop.annotation.EFilter;
import com.dfrdev.tarn.aop.annotation.EUpdateNull;
import com.dfrdev.tarn.aop.annotation.Ekey;
import com.dfrdev.tarn.aop.annotation.Etable;
import com.dfrdev.tarn.aop.annotation.ORMFilter;
import com.dfrdev.tarn.json.JsonKit;
import com.dfrdev.tarn.util.TextKit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体类元数据 + 缓存解析器（Tarn 2.0+）。
 * <p>
 * 解决老 ORM 每次 CRUD 都全量反射解析实体类的问题：
 * <ul>
 *     <li>注解语义与老框架完全一致：{@code @Etable} 表名、{@code @Ekey} 主键、
 *     {@code @EFilter} 更新过滤、{@code @ORMFilter} 解除映射</li>
 *     <li>按 Class 缓存元数据（ConcurrentHashMap），反射只执行一次</li>
 *     <li>复杂类型字段（List/Map/Set/数组/自定义类）统一走 {@link JsonKit} 序列化入库</li>
 * </ul>
 *
 * @author dfr
 * @since 2.0
 */
public final class EntityMapper {

    /**
     * 列元数据
     */
    public static final class Column {
        public final Field field;
        public final String columnName;   // 数据库列名（大写，与老框架一致）
        public final Method getter;
        public final Method setter;       // 主键回填用，可能为 null
        public final boolean primaryKey;
        public final boolean pkAutoIncrement;
        public final boolean jsonSerialize; // 复杂类型需 JSON 序列化
        public final boolean updateIncludeNull; // @EUpdateNull(true)：值为 null 也参与 UPDATE

        Column(Field field, String columnName, Method getter, Method setter,
               boolean primaryKey, boolean pkAutoIncrement, boolean jsonSerialize,
               boolean updateIncludeNull) {
            this.field = field;
            this.columnName = columnName;
            this.getter = getter;
            this.setter = setter;
            this.primaryKey = primaryKey;
            this.pkAutoIncrement = pkAutoIncrement;
            this.jsonSerialize = jsonSerialize;
            this.updateIncludeNull = updateIncludeNull;
        }
    }

    /**
     * 实体元数据
     */
    public static final class Meta {
        public final Class<?> type;
        public final String tableName;
        public final List<Column> columns;      // 含主键
        public final Column primaryKeyColumn;   // 无主键时为 null

        Meta(Class<?> type, String tableName, List<Column> columns, Column primaryKeyColumn) {
            this.type = type;
            this.tableName = tableName;
            this.columns = Collections.unmodifiableList(columns);
            this.primaryKeyColumn = primaryKeyColumn;
        }
    }

    private static final Map<Class<?>, Meta> CACHE = new ConcurrentHashMap<Class<?>, Meta>();

    private EntityMapper() {
    }

    /**
     * 获取实体元数据（带缓存，反射只执行一次）
     */
    public static Meta meta(Class<?> type) {
        Meta meta = CACHE.get(type);
        if (meta == null) {
            meta = parse(type);
            Meta existed = CACHE.putIfAbsent(type, meta);
            if (existed != null) {
                meta = existed;
            }
        }
        return meta;
    }

    /**
     * 解析实体类（注解语义与老 AppendReflectSQLUtils 完全一致）
     */
    private static Meta parse(Class<?> type) {
        String tableName = classNameUpper(type.getName());
        Etable etable = type.getAnnotation(Etable.class);
        if (etable != null) {
            tableName = etable.value();
        }

        List<Column> columns = new ArrayList<Column>();
        Column pkColumn = null;

        for (Field f : type.getDeclaredFields()) {
            // @ORMFilter：完全解除映射（老语义：出现即跳过）
            if (f.isAnnotationPresent(ORMFilter.class)) {
                continue;
            }
            // 静态/瞬态字段不入库（老框架未处理，属于质量修复）
            int mod = f.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) {
                continue;
            }

            String fieldName = f.getName();
            Method getter = findGetter(type, fieldName);
            if (getter == null) {
                continue; // 无 getter 的字段老框架也无法取值，跳过
            }
            Method setter = findSetter(type, fieldName);

            Ekey ekey = f.getAnnotation(Ekey.class);
            boolean jsonSerialize = isComplexType(f.getType());
            EUpdateNull updateNull = f.getAnnotation(EUpdateNull.class);
            boolean includeNull = updateNull != null && updateNull.value();

            if (ekey != null) {
                // 主键字段：@EFilter 对主键不生效（老语义主键单独分支）
                Column col = new Column(f, ekey.key().toUpperCase(), getter, setter,
                        true, ekey.isAutoIncrement(), jsonSerialize, includeNull);
                columns.add(col);
                pkColumn = col;
            } else {
                // @EFilter(true) 表示保存/修改时过滤
                EFilter efilter = f.getAnnotation(EFilter.class);
                if (efilter != null && efilter.value()) {
                    continue;
                }
                columns.add(new Column(f, fieldName.toUpperCase(), getter, setter,
                        false, false, jsonSerialize, includeNull));
            }
        }
        return new Meta(type, tableName, columns, pkColumn);
    }

    /**
     * 提取实体的 列名→值 Map（含 JSON 序列化处理）。
     *
     * @param entity    实体对象
     * @param includePk 是否包含主键列（insert 时主键自增则传 false）
     */
    public static Map<String, Object> toColumnValueMap(Object entity, boolean includePk) {
        Meta meta = meta(entity.getClass());
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        for (Column col : meta.columns) {
            if (col.primaryKey && (!includePk || col.pkAutoIncrement)) {
                continue;
            }
            Object value = readValue(col, entity);
            if (value != null && col.jsonSerialize) {
                value = JsonKit.toJson(value);
            }
            map.put(col.columnName, value);
        }
        return map;
    }

    /**
     * 读取主键值，无主键返回 null
     */
    public static Object readPrimaryKeyValue(Object entity) {
        Meta meta = meta(entity.getClass());
        if (meta.primaryKeyColumn == null) {
            return null;
        }
        return readValue(meta.primaryKeyColumn, entity);
    }

    /**
     * 读取主键列名，无主键默认 "ID"（与老框架默认一致）
     */
    public static String primaryKeyName(Class<?> type) {
        Meta meta = meta(type);
        return meta.primaryKeyColumn != null ? meta.primaryKeyColumn.columnName : "ID";
    }

    public static String tableName(Class<?> type) {
        return meta(type).tableName;
    }

    /**
     * 读取实体指定列的值（公共入口，供老解析代码复用）
     */
    public static Object readValue(Column col, Object entity) {
        try {
            return col.getter.invoke(entity);
        } catch (Exception e) {
            com.dfrdev.tarn.util.LogUtil.error(
                    "[EntityMapper] 读取字段失败: " + col.field.getName()
                            + " @ " + entity.getClass().getName() + " -> " + e.getMessage());
            return null;
        }
    }

    /**
     * 把数据库生成的主键回填到实体（仅当字段有 setter）。
     * 自动做 Number / String 之间的类型转换，失败只记日志不影响主流程。
     *
     * @return 是否成功回填
     */
    public static boolean writePrimaryKey(Object entity, Object key) {
        if (entity == null || key == null) {
            return false;
        }
        Meta meta = meta(entity.getClass());
        Column pk = meta.primaryKeyColumn;
        if (pk == null || pk.setter == null) {
            return false;
        }
        try {
            pk.setter.invoke(entity, convert(key, pk.field.getType()));
            return true;
        } catch (Exception e) {
            com.dfrdev.tarn.util.LogUtil.warn("[EntityMapper] 主键回填失败: "
                    + pk.field.getName() + " @ " + entity.getClass().getName() + " -> " + e.getMessage());
            return false;
        }
    }

    /**
     * 列标签 → 实体字段名（Tarn 2.1+ 跨数据库兼容）。
     * <p>
     * Oracle / 达梦 / 人大金仓等数据库返回大写列标签（如 {@code ID}），
     * 而 Java 字段通常是小写（{@code id}），按标签精确查找字段会静默丢值。
     * 这里借助实体元数据做忽略大小写匹配；匹配不到时原样返回。
     */
    public static String resolveFieldName(Class<?> clazz, String columnLabel) {
        if (columnLabel == null) {
            return null;
        }
        try {
            for (Column col : meta(clazz).columns) {
                if (col.field.getName().equals(columnLabel)
                        || col.columnName.equalsIgnoreCase(columnLabel)) {
                    return col.field.getName();
                }
            }
        } catch (Throwable ignore) {
            // 元数据解析异常不影响原有映射流程
        }
        return columnLabel;
    }

    private static Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return ((Number) value).shortValue();
        }
        if (targetType == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(String.valueOf(value));
        }
        return value;
    }

    private static Method findGetter(Class<?> type, String fieldName) {
        try {
            return type.getMethod("get" + TextKit.firstLetterToUpper(fieldName));
        } catch (NoSuchMethodException e) {
            // boolean 类型尝试 isXxx
            try {
                return type.getMethod("is" + TextKit.firstLetterToUpper(fieldName));
            } catch (NoSuchMethodException ignore) {
                return null;
            }
        }
    }

    private static Method findSetter(Class<?> type, String fieldName) {
        String suffix = TextKit.firstLetterToUpper(fieldName);
        try {
            Method setter = type.getMethod("set" + suffix, type.getDeclaredField(fieldName).getType());
            return setter;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static boolean isComplexType(Class<?> type) {
        if (type.isPrimitive() || type.isEnum()) {
            return false;
        }
        String name = type.getName();
        if (name.startsWith("java.lang.") || name.startsWith("java.math.")
                || name.startsWith("java.util.Date") || name.startsWith("java.sql.")
                || name.startsWith("java.time.")) {
            return false;
        }
        return java.util.Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type)
                || type.isArray()
                || !name.startsWith("java.");
    }

    private static String classNameUpper(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf(".") + 1).toUpperCase();
    }
}
