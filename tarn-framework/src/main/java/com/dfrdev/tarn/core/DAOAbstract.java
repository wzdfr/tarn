package com.dfrdev.tarn.core;


import com.dfrdev.tarn.core.connection.ConnectionFactory;
import com.dfrdev.tarn.core.sql.ParameterizedSql;
import com.dfrdev.tarn.orm.OrmTemplate;
import com.dfrdev.tarn.plugin.orm.SqlBuffer;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dao工具抽象类（Tarn 2.1：薄门面层）。
 * <p>
 * <b>架构说明：</b>全部持久化原语（save / saven / update / updaten / exec / count /
 * pager / get 等）的真实实现已收敛到 {@link OrmTemplate}（组合式引擎，单一实现），
 * 本类仅保留老继承链（{@code Q extends DB extends DAOAbstract}）的兼容入口，
 * 方法名与签名不变，内部逐方法委托——老项目零改动，实际执行已全部切换到新引擎：
 * <ul>
 *     <li>{@code #name} 命名占位 SQL 由 {@link OrmTemplate#named} 统一转为 {@code ?} 参数绑定，
 *     不再走 Statement 字符串替换，彻底消除注入面</li>
 *     <li>行映射（DAO 的 setValue / 实体的 invokeSetORM / CLOB / 递归字段 map）
 *     由 {@link OrmTemplate#queryLegacy} 复刻老语义</li>
 *     <li>SQL 失败统一抛 {@link TarnDataException}（非受检），不再静默吞掉返回 null/0</li>
 * </ul>
 */
public abstract class DAOAbstract extends ConnectionFactory {
    /**
     * 保存实体。
     * 并返回保存该实体后由数据库返回的主键集合。
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 该实体的 <code>List(Object)</code> 主键集
     */
    protected List<Object> save(String sql, Map<String, Object> map) throws DaoException, SQLException {
        return OrmTemplate.shared().execNamedReturnKeys(sql, map);
    }

    /**
     * 保存实体。
     * 并返回保存该实体后由数据库返回的主键集合。
     *
     * @param sql  SQL语句
     * @param arse 参数集
     * @return 该实体的 <code>List(Object)</code> 主键集
     */
    protected List<Object> save(String sql, AppendReflectSQLEntity arse) throws DaoException, SQLException {
        return OrmTemplate.shared().execNamedReturnKeys(sql, arse.getValueKey());
    }

    /**
     * 保存实体。
     * 没有返回值,方法可同   public List<Object> save(String sql, Map<String, Object> map) 一致。
     *
     * @param sql SQL语句
     * @param map 参数集
     */
    protected void saven(String sql, Map<String, Object> map) {
        OrmTemplate.shared().execNamed(sql, map);
    }

    protected void saven(String sql, AppendReflectSQLEntity arse) {
        OrmTemplate.shared().execNamed(sql, arse.getValueKey());
    }

    /**
     * 更新实体。
     * <p>保持老语义：参数缺失抛错（老 setParameterValueUpdate 的 TarnException → 现 TarnDataException）。
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 数据库中受影响的记录数
     */
    public int update(String sql, Map<String, Object> map) {
        return OrmTemplate.shared().execNamed(sql, map, true);
    }

    /**
     * 更新实体（参数缺失绑定 null，与老 setParameterValued 一致）。
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 数据库中受影响的记录数
     */
    protected int updaten(String sql, Map<String, Object> map) {
        return OrmTemplate.shared().execNamed(sql, map);
    }

    /**
     * 根据给定的SQL语句执行更新(UPDATE, INSERT,DELETE)查询.
     */
    protected int exec(String sql) {
        return OrmTemplate.shared().exec(new ParameterizedSql(sql, null));
    }

    /**
     * 返回查询总数
     */
    public int count(Model tmb) {
        String sql = "SELECT COUNT(0) AS num FROM " + tmb.table;
        String fil = tmb.filter;
        if (fil != null && !fil.equals("")) {
            int odb = fil.toLowerCase().lastIndexOf("order by");
            if (odb > 0) {
                fil = fil.substring(0, odb);
            }
            sql += " WHERE " + fil;
        }
        return OrmTemplate.shared().count(new ParameterizedSql(sql, null));
    }

    /**
     * 返回查询总数
     */
    public int count(String sql) {
        return OrmTemplate.shared().count(new ParameterizedSql(sql, null));
    }

    public int count(SqlBuffer sqlBuffer) {
        String sql = sqlBuffer.getCountSql();
        if (StringUtil.isNotEmpty(sql)) {
            if (Tarn.isDebug()) LogUtil.info(sql);
            return OrmTemplate.shared().count(OrmTemplate.named(sql, sqlBuffer.getParams()));
        }
        return 0;
    }

    // ==================== Tarn 2.0+ 参数化安全 API ====================
    // 真实实现在 OrmTemplate（组合式引擎），此处为老继承链兼容入口，逐个委托，行为一致。

    /**
     * 执行参数化更新 SQL（INSERT/UPDATE/DELETE）。值全部参数绑定，失败抛 TarnDataException（非受检）。
     *
     * @param ps 由 {@link com.dfrdev.tarn.core.sql.SqlBuilder} 构建参数化 SQL
     * @return 受影响行数
     */
    public int exec(ParameterizedSql ps) {
        return OrmTemplate.shared().exec(ps);
    }

    /**
     * 执行参数化 INSERT 并返回数据库生成的主键集合（Tarn 2.1+）。
     *
     * @param ps 参数化 INSERT SQL
     * @return 数据库返回的主键列表，驱动不支持时返回空列表
     */
    protected List<Object> execReturnKeys(ParameterizedSql ps) {
        return OrmTemplate.shared().execReturnKeys(ps);
    }

    /**
     * 参数化查询，结果按 {@link RowMapper} 逐行映射。
     *
     * @param ps     参数化 SQL
     * @param mapper 行映射器
     * @return 映射后的对象列表，无结果返回空列表
     */
    public List<?> query(ParameterizedSql ps, RowMapper mapper) {
        return OrmTemplate.shared().query(ps, mapper);
    }

    /**
     * 参数化 COUNT 查询：要求传入的 SQL 为 SELECT COUNT(0) 形式，
     * 可通过 SqlBuilder.create("SELECT COUNT(0) AS num FROM t").where(...) 构建。
     */
    public int count(ParameterizedSql ps) {
        return OrmTemplate.shared().count(ps);
    }

    /**
     * Tarn 2.0+: 参数化实体查询（PreparedStatement 全参数绑定）。
     * 行→实体映射逻辑在 {@link OrmTemplate}，
     * 复用老框架的 {@code ActionInvocation.invokeSetORM}，行为一致。
     *
     * @param sql    含 ? 占位的 SQL
     * @param params 参数（按 ? 顺序）
     * @param clazz  目标实体类型
     */
    @SuppressWarnings("rawtypes")
    public List<Object> queryEntityList(String sql, List<Object> params, Class clazz) {
        return OrmTemplate.shared().queryEntityList(sql, params, clazz);
    }

    /**
     * Tarn 2.0+: 参数化查询，结果以 List&lt;Map&gt; 返回（列名→值）。
     * 适用于多表 JOIN 等无法映射到单一实体的场景。
     */
    public List<Map<String, Object>> queryMaps(ParameterizedSql ps) {
        return OrmTemplate.shared().queryMaps(ps);
    }

    /**
     * Tarn 2.0+: 参数化查询并映射为实体列表（QueryBuilder 便捷入口）。
     * <pre>
     *     List&lt;User&gt; users = (List&lt;User&gt;)(List&lt;?&gt;) db.query(QueryBuilder.select("*")
     *         .from("sys_users").where("status=?", 1).build(), User.class);
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public List<?> query(ParameterizedSql ps, Class clazz) {
        return OrmTemplate.shared().query(ps, clazz);
    }

    /**
     * 返回查询总数
     */
    public int count(Model tmb, String filter) {
        String sql = "SELECT COUNT(0) AS num FROM " + tmb.table;
        String fil = filter;
        if (fil != null && !fil.equals("")) {
            int odb = fil.toLowerCase().lastIndexOf("order by");
            if (odb > 0) {
                fil = fil.substring(0, odb);
            }
            sql += " WHERE " + fil;
        }
        return OrmTemplate.shared().count(new ParameterizedSql(sql, null));
    }

    /**
     * 查询数据分页。<br/>
     * 根据传人的Tmb，查出数据并映射到 clazz后，组装到<code>Pager</code>分页类。<br/>
     * 需要注意的是：除系统自带的<code>Dao()</code>类，自定义的Bean的属性应与待查询数据库字段名对应，否则会导致查询失败<br/>
     * <p>
     * 2.1：行查询委托 {@link OrmTemplate#queryLegacy}（PreparedStatement），
     * 不再使用 Statement；总数统计走 {@link OrmTemplate#count}。
     *
     * @param clazz 需要映射的类
     * @param tmb   数据库查询语句组合类
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Pager pagerList(Class clazz, Model tmb, String pageUrl) {
        String sql = AppendAbstractUtils.generateSelectSql(tmb);
        if (Tarn.isDebug) LogUtil.info("SQL:" + sql);
        int pageSize = tmb.pageSize;
        int absPage = tmb.absPage;
        Object os[] = tmb.prepareVal;
        int rowCount = 0;
        int pageCount = 0;
        String thisUrl = null;
        if (pageSize > 0) {
            thisUrl = pageUrl;
            if (os == null) {
                rowCount = count(tmb);
            } else {
                // 老语义：count 的 filter 将 ? 顺序替换为字面量（保持原样，仅用于 COUNT 文本）
                String stmp = tmb.filter;
                if (stmp != null) {
                    for (int i = 1, n = os.length; i <= n; i++) {
                        stmp = stmp.replaceFirst("\\?", "'" + os[i - 1] + "'");
                    }
                }
                rowCount = count(tmb, stmp);
            }
            pageCount = (rowCount + pageSize - 1) / pageSize;
            if (absPage > pageCount && pageCount > 0) {
                absPage = pageCount;
            }
        }
        // ORACLE 分页 SQL 末尾附带 ROWNUM_FOR_ORACLE 伪列，映射时跳过最后一列（与老 pagerList 一致）
        boolean oraclePaged = pageSize > 0
                && "ORACLE".equalsIgnoreCase(DATA_SOURCE_POOL.getDatabaseType());
        List<Object> params = os == null ? null : new ArrayList<Object>(Arrays.asList(os));
        List list = OrmTemplate.shared().queryLegacy(sql, params, clazz, tmb.table, oraclePaged);
        // 老语义：Model 设置了空值默认值时回填到每条 DAO 记录
        if (DAO.class.equals(clazz) && tmb.getRecordNullDefaultValue() != null) {
            for (Object o : list) {
                ((DAO) o).setRecordNullDefaultValue(tmb.getRecordNullDefaultValue());
            }
        }
        return new Pager<DAO>(list, tmb.absPage, tmb.pageSize, pageCount, rowCount, thisUrl);
    }

    /**
     * 指定SQL语句查询数据,不分页<br/>
     * 2.1：委托 {@link OrmTemplate#queryLegacy}（PreparedStatement 执行，行映射语义不变）。
     *
     * @param clazz 需要转化的类
     * @param sql   sql查询语句
     */
    @SuppressWarnings("rawtypes")
    public List pager(Class clazz, String sql) {
        return OrmTemplate.shared().queryLegacy(sql, null, clazz, null);
    }


    /**
     * 预编译查询（#name 命名占位 + Map 参数）
     * 2.1：委托 {@link OrmTemplate#queryNamedLegacy}（命名占位统一转 ? 参数绑定）。
     */
    @SuppressWarnings("rawtypes")
    public List pager(Class clazz, String sql, Map params) {
        return OrmTemplate.shared().queryNamedLegacy(sql, params, clazz);
    }


    /**
     * 此方法弥补了查询分页使用游标，将sql语句直接生成带分页的SQL语句。耗资低、效率高<br>
     * 保留使用流标的形式分页是为了，一些负责SQL语句，最好使用以下方法。原因就不细说了<br/>
     *
     * @param clazz    需要转化的类
     * @param sql      sql查询语句
     * @param pageSize 指定分页大小.不指定则传入0
     * @param absPage  指定当前页
     */
    @SuppressWarnings("rawtypes")
    protected Pager pager(Class clazz, String sql, int pageSize, int absPage, String thisUrl) {
        List list = pager(clazz, AppendAbstractUtils.generatePageFragmentSql(sql, pageSize, absPage));
        int pageCount = list.size();
        if (pageSize != 0) pageCount = (list.size() + pageSize - 1) / pageSize;
        return new Pager(list, absPage, pageSize, pageCount, list.size(), thisUrl);

    }


    /**
     * 根据SQL语句查询语句返回封装好的Pager对象.耗资高、效率低<br/>
     * Pager是个分页对象。提示：该方法每次查询整张表数据（老实现通过 <code>rs.absolute()</code> 游标定位，
     * 2.1 改为查询后内存切片，语义与返回值不变，不再持有滚动游标）。
     *
     * @param clazz    需要转化的类
     * @param sql      sql查询语句
     * @param pageSize 指定分页大小.不指定则传入0
     * @param absPage  指定当前页
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Pager pagerAbsolute(Class clazz, String sql, int pageSize, int absPage, String thisUrl) {
        List<Object> all = OrmTemplate.shared().queryLegacy(sql, null, clazz, null);
        int rowCount = all.size();
        if (absPage < 1) absPage = 1;
        int pageCount;
        int size;
        List list;
        if (pageSize > 0) {
            pageCount = (rowCount + pageSize - 1) / pageSize;
            if (absPage > pageCount) absPage = pageCount;
            if (rowCount == 0) {
                size = 0;
            } else {
                size = pageCount > absPage ? pageSize : rowCount % pageSize;
                if (size == 0) size = pageSize;
            }
            int start = Math.max(0, pageSize * (absPage - 1));
            int end = Math.min(start + size, rowCount);
            list = start >= rowCount ? new ArrayList() : new ArrayList(all.subList(start, end));
        } else {
            thisUrl = null;
            pageCount = 1;
            absPage = 1;
            size = rowCount;
            list = all;
        }
        return new Pager(list, absPage, pageSize, pageCount, size, thisUrl);
    }


    /**
     * 根据SQL语句、参数集和行映射器 获取一个实体。
     * 若数据库中有多条满足条件的记录，则返回第一条。
     * <p>该方法需要一个行映射器 <code>RowMapper</code>，
     * 用于实现从 <code>ResultSet</code> 结果集到实体的映射工作。
     * <p>2.1：委托 {@link OrmTemplate#queryOneNamed}（参数缺失抛错、like 自动包装 %值%，与老语义一致）。
     *
     * @param sql       SQL语句
     * @param map       参数集
     * @param rowMapper 行映射器
     * @return 符合条件的对象
     * @see RowMapper
     */
    protected Object get(String sql, Map<String, Object> map, RowMapper rowMapper) {
        return OrmTemplate.shared().queryOneNamed(sql, map, rowMapper);
    }


    /**
     * 根据类、SQL语句和参数集 获取一个实体
     * <p>若数据库中有多条满足条件的记录，则返回第一条。
     * <p>该方法需要指定需要获取的实体的类型 <code>Class</code>，
     * 根据指定的类型，将根据SQL查询出的结果集自动填充到该实体中。
     * <br>映射规则为SQL语句中所要查询内容的别名与该实体的属性名一致。
     * <p>2.1：委托 {@link OrmTemplate#queryOneBean}（setter 名匹配映射，行为一致）。
     *
     * @param clazz 实体类型
     * @param sql   SQL语句
     * @param map   参数集
     * @return 符合条件的对象
     */
    protected <T> T get(Class<T> clazz, String sql, Map<String, Object> map) {
        return OrmTemplate.shared().queryOneBean(sql, map, clazz);
    }

    /**
     * 删除实体
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 数据库中受影响的记录数
     */
    protected int delete(String sql, Map<String, Object> map) throws DaoException, SQLException {
        return this.update(sql, map);
    }

    /**
     * 根据SQL语句、参数集和行映射器 获取一个实体
     * <p>若数据库中有多条满足条件的记录，则返回第一条。
     *
     * @param sql       SQL语句
     * @param map       参数集
     * @param rowMapper 行映射器
     * @return 符合条件的对象
     * @see RowMapper
     */
    protected Object query(String sql, Map<String, Object> map, RowMapper rowMapper) {
        return this.get(sql, map, rowMapper);
    }

    /**
     * 根据类、SQL语句和参数集 获取一个实体
     * <p>若数据库中有多条满足条件的记录，则返回第一条。
     *
     * @param clazz 实体类型
     * @param sql   SQL语句
     * @param map   参数集
     * @return 符合条件的对象
     */
    public <T> T query(Class<T> clazz, String sql, Map<String, Object> map) throws Exception {
        return this.get(clazz, sql, map);
    }

    public <T> List<T> saveAll(String sql, List<T> list) {
        // 历史遗留空实现（老版本同样未执行任何写入），保持行为不变
        return null;
    }

    public <T> void deleteAll(List<T> list) {

    }

    public <T> void updateAll(List<T> list) {

    }

    public <T> void getAll(List<T> list) {

    }


    private boolean connTrank = false;
}
