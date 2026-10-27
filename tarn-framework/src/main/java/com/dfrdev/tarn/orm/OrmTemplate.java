package com.dfrdev.tarn.orm;

import com.dfrdev.tarn.core.AppendAbstractUtils;
import com.dfrdev.tarn.core.Config;
import com.dfrdev.tarn.core.DAO;
import com.dfrdev.tarn.core.Tarn;
import com.dfrdev.tarn.core.TarnDataException;
import com.dfrdev.tarn.core.RowMapper;
import com.dfrdev.tarn.aop.ActionInvocation;
import com.dfrdev.tarn.core.connection.ConnectionFactory;
import com.dfrdev.tarn.core.sql.ParameterizedSql;
import com.dfrdev.tarn.plugin.DbFieldMapping;
import com.dfrdev.tarn.supports.SQLEXECLOG;
import com.dfrdev.tarn.util.LogUtil;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ORM 引擎（Tarn 2.1 重构：组合代替继承）。
 * <p>
 * <b>设计动机：</b>老调用链 {@code Tarn → Q → DB → DAOAbstract → ConnectionFactory}
 * 是一条 4 层继承链，业务类被迫通过 {@code extends Q} 获得数据库能力，
 * 导致：API 面污染（一个 Q 实例携带全部 JDBC 内部方法）、职责混杂、无法独立替换实现。
 * <p>
 * 本类把 ORM 能力收敛为一个<b>组合式引擎</b>：
 * <ul>
 *     <li><b>组合</b>：内部持有 {@link ConnectionFactory}（HAS-A），不再继承连接层</li>
 *     <li><b>工厂</b>：通过 {@link Orm#template()} 获取共享实例，业务代码零继承即可使用</li>
 *     <li><b>单一实现</b>：实体写语义（EntitySql）与 JDBC 原语只在此处实现，
 *     老继承链（Q/DB/DAOAbstract）退化为薄门面逐层委托</li>
 *     <li><b>事务</b>：支持 lambda 风格 {@link #inTransaction(TransactionCallback)}，
 *     正常返回提交、抛异常自动回滚</li>
 * </ul>
 * 新代码推荐写法：
 * <pre>
 *     OrmTemplate orm = Orm.template();
 *     Object key = orm.insertReturnKey(user);          // 新增并回填主键
 *     int rows = orm.updateRows(user);                 // 按主键更新
 *     List&lt;User&gt; list = orm.query(ps, User.class);     // 参数化查询
 *     orm.inTransaction(tx -&gt; { ... });                // 声明式事务
 * </pre>
 * 老 API（{@code new Q()} / {@code extends Q} / {@code Tarn.xxx}）全部保留，
 * 内部同样委托到本引擎，行为一致。
 *
 * @author dfr
 * @see Orm 工厂入口
 * @see EntitySql 实体 SQL 生成
 * @since 2.1
 */
public class OrmTemplate {

    /**
     * 共享实例：无状态、线程安全（连接与事务绑定在 ThreadLocal）
     */
    private static final OrmTemplate SHARED = new OrmTemplate();

    /**
     * 组合而非继承：连接池/事务能力委托给连接工厂
     */
    private final ConnectionFactory connections = new ConnectionFactory();

    public static OrmTemplate shared() {
        return SHARED;
    }

    // ==================== 实体写操作（语义由 EntitySql 定义） ====================

    /**
     * 插入实体，返回受影响行数（不返回主键）。
     */
    public int insert(Object entity) {
        return exec(EntitySql.insert(entity));
    }

    /**
     * 插入实体，返回数据库生成的主键并回填到实体主键字段。
     *
     * @return 生成的主键；驱动不支持或非自增主键时返回 null
     */
    public Object insertReturnKey(Object entity) {
        List<Object> keys = execReturnKeys(EntitySql.insert(entity));
        if (keys.isEmpty()) {
            return null;
        }
        Object key = keys.get(0);
        EntityMapper.writePrimaryKey(entity, key);
        return key;
    }

    /**
     * 按主键更新实体，返回受影响行数（0 表示主键不存在或字段均未变化）。
     * 默认跳过 null 字段，{@code @EUpdateNull(true)} 字段参与更新。
     */
    public int updateRows(Object entity) {
        return exec(EntitySql.update(entity));
    }

    /**
     * 按主键删除实体，返回受影响行数。
     */
    public int deleteRows(Object entity) {
        return exec(EntitySql.deleteByPrimaryKey(entity));
    }

    /**
     * 批量插入（同一连接、单事务）。任一失败整体回滚并抛 {@link TarnDataException}。
     *
     * @return 成功插入的总行数
     */
    public int batchInsert(List<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int total = 0;
        try {
            beginTransaction();
            for (Object entity : entities) {
                total += exec(EntitySql.insert(entity));
            }
            commit();
            return total;
        } catch (Throwable e) {
            rollbackQuietly();
            throw new TarnDataException("batchInsert 失败，已回滚: " + e.getMessage(), e);
        }
    }

    // ==================== 参数化 SQL 执行 ====================

    /**
     * 执行参数化 UPDATE / INSERT / DELETE，返回受影响行数。
     */
    public int exec(ParameterizedSql ps) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(ps.getSql());
            bindParams(pstmt, ps);
            if (Tarn.isDebug()) LogUtil.debug("SQL:" + ps);
            int rows = pstmt.executeUpdate();
            logSql(ps.getSql());
            return rows;
        } catch (SQLException e) {
            LogUtil.error("[Tarn-ORM] exec failed: " + ps.getSql() + " -> " + e.getMessage());
            throw new TarnDataException("SQL exec failed: " + e.getMessage(), e);
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, null, pstmt);
        }
    }

    /**
     * 执行参数化 INSERT 并返回数据库生成的主键集合。
     */
    public List<Object> execReturnKeys(ParameterizedSql ps) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object> keys = new ArrayList<Object>();
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(ps.getSql(), Statement.RETURN_GENERATED_KEYS);
            bindParams(pstmt, ps);
            if (Tarn.isDebug()) LogUtil.debug("SQL:" + ps);
            pstmt.executeUpdate();
            rs = pstmt.getGeneratedKeys();
            while (rs.next()) {
                keys.add(rs.getObject(1));
            }
            logSql(ps.getSql());
            return keys;
        } catch (SQLException e) {
            LogUtil.error("[Tarn-ORM] insert failed: " + ps.getSql() + " -> " + e.getMessage());
            throw new TarnDataException("SQL insert failed: " + e.getMessage(), e);
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    /**
     * 参数化 COUNT 查询（SQL 应为 SELECT COUNT(...) 形式，可用 SqlBuilder 构建）。
     */
    public int count(ParameterizedSql ps) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(ps.getSql());
            bindParams(pstmt, ps);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            LogUtil.error("[Tarn-ORM] count failed: " + ps.getSql() + " -> " + e.getMessage());
            throw new TarnDataException("SQL count failed: " + e.getMessage(), e);
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    // ==================== 参数化查询 ====================

    /**
     * 参数化查询，结果按 {@link RowMapper} 逐行映射。
     */
    public List<?> query(ParameterizedSql ps, RowMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("RowMapper 不能为 null");
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object> result = new ArrayList<Object>();
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(ps.getSql());
            bindParams(pstmt, ps);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(mapper.mapRow(rs));
            }
            logSql(ps.getSql());
            return result;
        } catch (SQLException e) {
            LogUtil.error("[Tarn-ORM] query failed: " + ps.getSql() + " -> " + e.getMessage());
            throw new TarnDataException("SQL query failed: " + e.getMessage(), e);
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    /**
     * 参数化查询并映射为实体列表。
     * <pre>
     *     List&lt;Book&gt; books = Orm.template().query(
     *             SqlBuilder.select("*").from("demo_book").where("STOCK &gt; ?", 0).build(),
     *             Book.class);
     * </pre>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> List<T> query(ParameterizedSql ps, Class<T> clazz) {
        return (List<T>) queryEntityList(ps.getSql(), ps.getParams(), (Class) clazz);
    }

    /**
     * 参数化查询并返回首条实体；无结果返回 null。
     */
    public <T> T queryOne(ParameterizedSql ps, Class<T> clazz) {
        List<T> list = query(ps, clazz);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 参数化查询，结果以 List&lt;Map&gt; 返回（列名→值）。
     * 适用于多表 JOIN 等无法映射到单一实体的场景。
     */
    public List<Map<String, Object>> queryMaps(ParameterizedSql ps) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(ps.getSql());
            bindParams(pstmt, ps);
            rs = pstmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                for (int j = 1; j <= colCount; j++) {
                    row.put(rsmd.getColumnLabel(j), rs.getObject(j));
                }
                list.add(row);
            }
            logSql(ps.getSql());
            return list;
        } catch (SQLException e) {
            LogUtil.error("[Tarn-ORM] queryMaps failed: " + ps.getSql() + " -> " + e.getMessage());
            throw new TarnDataException("queryMaps failed: " + e.getMessage(), e);
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    /**
     * 实体查询（全参数绑定）。行→实体映射复用老框架的 {@code ActionInvocation.invokeSetORM}
     * 与 DbFieldMapping 插件逻辑，行为与老版本一致。
     *
     * @param sql    含 ? 占位的 SQL
     * @param params 参数（按 ? 顺序）
     * @param clazz  目标实体类型
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> queryEntityList(String sql, List<Object> params, Class clazz) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object> list = new ArrayList<Object>();
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
            }
            if (Tarn.isDebug()) LogUtil.debug("SQL:" + sql + " params:" + params);
            rs = pstmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();
            String[] cols = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                cols[i - 1] = rsmd.getColumnLabel(i);
            }
            DbFieldMapping fieldMapping = Config.getPlugins().findDbFieldMapping();
            while (rs.next()) {
                Object target = clazz.newInstance();
                for (int j = 1; j <= colCount; j++) {
                    String fieldName = cols[j - 1];
                    Object value = (rsmd.getColumnType(j) == Types.CLOB)
                            ? AppendAbstractUtils.clobToString(rs.getClob(j)) : rs.getObject(j);
                    boolean expanded = false;
                    if (fieldMapping != null) {
                        expanded = fieldMapping.invokeFieldORMToEntity(target, fieldName, value,
                                rsmd.getColumnType(j), null);
                        if (!expanded) {
                            fieldName = fieldMapping.invokeFieldName(target, cols[j - 1]);
                        }
                    }
                    if (!expanded) {
                        com.dfrdev.tarn.aop.ActionInvocation.invokeSetORM(
                                target, EntityMapper.resolveFieldName(clazz, fieldName), value, null);
                    }
                }
                list.add(target);
            }
            logSql(sql);
            return list;
        } catch (Exception e) {
            LogUtil.error("[Tarn-ORM] queryEntityList failed: " + sql + " -> " + e.getMessage());
            throw new TarnDataException("queryEntityList failed: " + e.getMessage(),
                    e instanceof SQLException ? (SQLException) e : new SQLException(e));
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    // ==================== 事务 ====================

    /**
     * 声明式事务：回调正常返回则提交；抛出任何异常则回滚，
     * 并包装为 {@link TarnDataException}（非受检）抛出。
     * <pre>
     *     Orm.template().inTransaction(new TransactionCallback&lt;Void&gt;() {
     *         public Void doInTransaction(OrmTemplate orm) {
     *             orm.insert(order);
     *             orm.batchInsert(items);
     *             return null;
     *         }
     *     });
     * </pre>
     */
    public <T> T inTransaction(TransactionCallback<T> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("TransactionCallback 不能为 null");
        }
        try {
            beginTransaction();
        } catch (TarnDataException e) {
            throw e;
        } catch (Exception e) {
            throw new TarnDataException("开启事务失败: " + e.getMessage(), e);
        }
        try {
            T result = callback.doInTransaction(this);
            commit();
            return result;
        } catch (Throwable e) {
            rollbackQuietly();
            if (e instanceof TarnDataException) {
                throw (TarnDataException) e;
            }
            throw new TarnDataException("事务执行失败，已回滚: " + e.getMessage(), e);
        }
    }

    /**
     * 开启事务（当前线程绑定连接，隔离级别 READ_COMMITTED）。
     */
    public void beginTransaction() {
        try {
            connections.transOpen();
        } catch (Exception e) {
            throw new TarnDataException("开启事务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提交当前线程事务。
     */
    public void commit() {
        try {
            connections.transCommit();
        } catch (Exception e) {
            throw new TarnDataException("提交事务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 回滚当前线程事务。
     */
    public void rollback() {
        try {
            connections.transRollback();
        } catch (Exception e) {
            throw new TarnDataException("回滚事务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 当前线程是否已有未提交事务（true = 存在事务）
     */
    public boolean inTransaction() {
        return ConnectionFactory.DATA_SOURCE_POOL != null
                && ConnectionFactory.DATA_SOURCE_POOL.getThreadLocalConnection() != null;
    }

    private void rollbackQuietly() {
        try {
            connections.transRollback();
        } catch (Throwable ignore) {
            // 回滚失败不掩盖原始异常
        }
    }

    // ==================== 老 API 桥接（#{name} 命名占位 SQL → 参数化执行） ====================
    // 老继承链（Q/DB/DAOAbstract）的 JDBC 原语全部委托到这里，保证"接口不变、内核升级"。

    /**
     * 老框架命名占位符：#name（与老 AppendParmeterSQLUtils 的参数名字符集一致）
     */
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile("#([a-zA-Z_0-9-]+)");

    /**
     * 将老框架 {@code #{name}} 风格（实际写法为 {@code #name}）命名占位 SQL
     * 转换为 {@link ParameterizedSql}（{@code ?} 占位 + 按出现顺序排列的参数）。
     * <p>缺省语义与 {@code setParameterValued} 一致：参数缺失时绑定 null。
     */
    public static ParameterizedSql named(String namedSql, Map<String, Object> params) {
        return named(namedSql, params, false, false);
    }

    /**
     * @param strict    true = 参数缺失时抛 {@link TarnDataException}
     *                  （与老 setParameterValue / setParameterValueUpdate 的 TarnException 一致）；
     *                  false = 缺失绑定 null（与老 setParameterValued 一致）
     * @param likeAware true = 占位符前 5 个字符为 "like "（忽略大小写）时，
     *                  值自动包装为 %value%（与老 setParameterValue 的查询语义一致）
     */
    public static ParameterizedSql named(String namedSql, Map<String, Object> params, boolean strict, boolean likeAware) {
        if (namedSql == null) {
            throw new IllegalArgumentException("SQL 不能为 null");
        }
        List<Object> values = new ArrayList<Object>();
        Matcher m = NAMED_PARAM_PATTERN.matcher(namedSql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            Object value = params == null ? null : params.get(name);
            if (value == null && strict) {
                throw new TarnDataException(name + " 没有被设置！");
            }
            if (likeAware && value != null && m.start() >= 5
                    && namedSql.substring(m.start() - 5, m.start()).equalsIgnoreCase("like ")) {
                value = "%" + value + "%";
            }
            values.add(value);
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return new ParameterizedSql(sb.toString(), values);
    }

    /**
     * 执行老命名占位 INSERT/UPDATE/DELETE，返回受影响行数（参数缺失绑定 null）。
     */
    public int execNamed(String namedSql, Map<String, Object> params) {
        return exec(named(namedSql, params));
    }

    /**
     * @param strict true = 参数缺失抛 {@link TarnDataException}（对应老 update() 语义）
     */
    public int execNamed(String namedSql, Map<String, Object> params, boolean strict) {
        return exec(named(namedSql, params, strict, false));
    }

    /**
     * 执行老命名占位 INSERT，返回数据库生成的主键集合。
     */
    public List<Object> execNamedReturnKeys(String namedSql, Map<String, Object> params) {
        return execReturnKeys(named(namedSql, params));
    }

    /**
     * 老命名占位查询，行→DAO/实体映射（行为与老 DAOAbstract.pager(clazz, sql, params) 一致）。
     */
    @SuppressWarnings("rawtypes")
    public List<Object> queryNamedLegacy(String namedSql, Map<String, Object> params, Class clazz) {
        ParameterizedSql ps = named(namedSql, params);
        return queryLegacy(ps.getSql(), ps.getParams(), clazz, null);
    }

    /**
     * RowMapper 查询取首行（老 get(sql, map, rowMapper) 语义：参数缺失抛错、like 自动包装 %值%）。
     */
    public Object queryOneNamed(String namedSql, Map<String, Object> params, RowMapper mapper) {
        List<?> list = query(named(namedSql, params, true, true), mapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * setter 名匹配映射取首行（老 get(clazz, sql, map) 语义：列名与 setXxx 忽略大小写匹配）。
     */
    @SuppressWarnings("unchecked")
    public <T> T queryOneBean(String namedSql, Map<String, Object> params, final Class<T> clazz) {
        List<?> list = query(named(namedSql, params, true, true), new RowMapper() {
            public Object mapRow(ResultSet rs) throws SQLException {
                try {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int count = rsmd.getColumnCount();
                    T obj = clazz.newInstance();
                    Method[] ms = clazz.getMethods();
                    for (int i = 1; i <= count; i++) {
                        String col = rsmd.getColumnLabel(i);
                        for (Method method : ms) {
                            if (method.getName().equalsIgnoreCase("set" + col)) {
                                try {
                                    method.invoke(obj, rs.getObject(col));
                                } catch (IllegalArgumentException ignore) {
                                    // 重载 setter 类型不匹配，尝试下一个（老实现直接异常返回 null，此处更宽容）
                                }
                            }
                        }
                    }
                    return obj;
                } catch (SQLException e) {
                    throw e;
                } catch (Exception e) {
                    throw new TarnDataException("行映射失败: " + e.getMessage(), e);
                }
            }
        });
        return list.isEmpty() ? null : (T) list.get(0);
    }

    /**
     * 老语义实体查询：行→DAO 走 {@link AppendAbstractUtils#setValue}（含 setNull/tableName），
     * 行→实体走 {@code ActionInvocation.invokeSetORM}（含递归字段 map 与 CLOB 处理），
     * 行为与老 DAOAbstract.pager/pagerList 一致。
     *
     * @param tableName DAO 映射时回填的表名；null 时取结果集第一列所属表
     */
    @SuppressWarnings("rawtypes")
    public List<Object> queryLegacy(String sql, List<Object> params, Class clazz, String tableName) {
        return queryLegacy(sql, params, clazz, tableName, false);
    }

    /**
     * @param skipLastColumn ORACLE 分页 SQL 末尾附带 ROWNUM_FOR_ORACLE 伪列时为 true
     *                       （映射时跳过最后一列，与老 pagerList 一致）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> queryLegacy(String sql, List<Object> params, Class clazz, String tableName, boolean skipLastColumn) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Object> list = new ArrayList<Object>();
        try {
            conn = connections.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
            }
            if (Tarn.isDebug()) LogUtil.debug("SQL:" + sql + (params != null && !params.isEmpty() ? " params:" + params : ""));
            rs = pstmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int num = rsmd.getColumnCount();
            if (skipLastColumn && num > 1) {
                num--;
            }
            String[] cols = new String[num];
            boolean fieldIsRecursive = false;
            for (int j = 0; j < num; j++) {
                cols[j] = rsmd.getColumnLabel(j + 1);
                if (cols[j].indexOf(".") != -1) {
                    fieldIsRecursive = true;
                }
            }
            boolean isDao = DAO.class.equals(clazz);
            String daoTable = tableName != null ? tableName
                    : (rsmd.getColumnCount() > 0 ? rsmd.getTableName(1) : null);
            DbFieldMapping fieldMapping = Config.getPlugins().findDbFieldMapping();
            while (rs.next()) {
                if (isDao) {
                    DAO d = new DAO();
                    for (int j = 1; j <= num; j++) {
                        AppendAbstractUtils.setValue(cols, rsmd.getColumnType(j), j, rs, d);
                    }
                    d.setNull(false);
                    if (daoTable != null) {
                        d.setTableName(daoTable);
                    }
                    list.add(d);
                } else {
                    Object target = clazz.newInstance();
                    HashMap map = fieldIsRecursive ? new HashMap() : null;
                    for (int j = 1; j <= num; j++) {
                        String fieldName = cols[j - 1];
                        Object value = (rsmd.getColumnType(j) == Types.CLOB)
                                ? AppendAbstractUtils.clobToString(rs.getClob(j)) : rs.getObject(j);
                        boolean expanded = false;
                        if (fieldMapping != null) {
                            expanded = fieldMapping.invokeFieldORMToEntity(target, cols[j - 1],
                                    value, rsmd.getColumnType(j), map);
                            if (!expanded) {
                                fieldName = fieldMapping.invokeFieldName(target, cols[j - 1]);
                            }
                        }
                        if (!expanded) {
                            // 经 EntityMapper 大小写不敏感解析（与 queryEntityList 一致）：
                            // 老实现直接按列标签 getDeclaredField，SELECT * 大写列名必然 NoSuchFieldException 静默跳过
                            ActionInvocation.invokeSetORM(target,
                                    EntityMapper.resolveFieldName(clazz, fieldName), value, map);
                        }
                    }
                    list.add(target);
                }
            }
            logSql(sql);
            return list;
        } catch (Exception e) {
            LogUtil.error("[Tarn-ORM] queryLegacy failed: " + sql + " -> " + e.getMessage());
            throw new TarnDataException("queryLegacy failed: " + e.getMessage(),
                    e instanceof SQLException ? (SQLException) e : new SQLException(e));
        } finally {
            ConnectionFactory.DATA_SOURCE_POOL.close(conn, rs, pstmt);
        }
    }

    // ==================== 内部工具 ====================

    private void bindParams(PreparedStatement pstmt, ParameterizedSql ps) throws SQLException {
        List<Object> params = ps.getParams();
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    private void logSql(String sql) {
        if (Tarn.isSaveSqlLogger()) {
            SQLEXECLOG.addSQLExecvo(sql, sql, Tarn.getPageContext() == null ? null : Tarn.getPageContext().getRequest());
        }
    }
}
