package com.dfrdev.tarn.core;

import com.dfrdev.tarn.orm.EntityMapper;
import com.dfrdev.tarn.orm.OrmTemplate;
import com.dfrdev.tarn.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * ORM 写操作门面（Tarn 2.1 重构：委托模式）。
 * <p>
 * <b>架构说明：</b>实体写操作的真实实现已收敛到
 * {@link com.dfrdev.tarn.orm.OrmTemplate}（组合式引擎，单一实现），
 * 本类仅保留老继承链（{@code Q extends DB extends DAOAbstract}）的兼容入口，
 * 逐方法委托到 {@link OrmTemplate#shared()}。
 * 新代码请直接使用 {@link com.dfrdev.tarn.orm.Orm#template()}，无需继承任何类。
 * <p>
 * 写操作语义（由 {@link com.dfrdev.tarn.orm.EntitySql} 定义）：
 * <ul>
 *     <li>{@code insert}：自增主键不写入，返回受影响行数</li>
 *     <li>{@code insertReturnKey}：返回主键并回填到实体</li>
 *     <li>{@code updateRows}：按主键更新，默认跳过 null 字段（{@code @EUpdateNull} 例外）</li>
 *     <li>{@code deleteRows}：严格按主键删除</li>
 * </ul>
 * 任何 SQL 失败都以 {@link TarnDataException}（非受检）抛出，不再静默吞掉。
 */
public class DB extends DAOAbstract {

    protected DB() {

    }

    // ==================== 实体写操作（委托 OrmTemplate，单一实现） ====================

    /**
     * 插入实体，返回受影响行数（不返回主键）。
     */
    public int insert(Object entity) {
        return OrmTemplate.shared().insert(entity);
    }

    /**
     * 插入实体，返回数据库生成的主键并回填到实体主键字段。
     *
     * @return 生成的主键；驱动不支持或非自增主键时返回 null
     */
    public Object insertReturnKey(Object entity) {
        return OrmTemplate.shared().insertReturnKey(entity);
    }

    /**
     * 按主键更新实体，返回受影响行数（0 表示主键不存在或字段均未变化）。
     */
    public int updateRows(Object entity) {
        return OrmTemplate.shared().updateRows(entity);
    }

    /**
     * 按主键删除实体，返回受影响行数。
     */
    public int deleteRows(Object entity) {
        return OrmTemplate.shared().deleteRows(entity);
    }

    /**
     * 批量插入（同一连接、单事务）。任一失败整体回滚。
     */
    public int batchInsert(List<?> entities) {
        return OrmTemplate.shared().batchInsert(entities);
    }

    // ==================== 老 API（保持签名，语义修正） ====================

    /**
     * 保存一个Dao对象到数据库。
     *
     * @param d 需要保存的Dao对象。
     */
    protected void ds(DAO d) {
        Map map = d.getCm();
        String sql = AppendDaoSQLUtils.generateInsertSql(d);
        if (sql == null) return;
        if (isDebug) LogUtil.debug("save sql:" + sql);
        saven(sql, map);
    }

    /**
     * 保存一个Object对象到数据库。<br/>
     * 这个Object应该是一个标准的Bean，具有<code>get()</code>,<code>set()</code>方法。它是一个自定义的Bean不需要任何继承或和系统关联。
     * 系统提供简单的Bean注解，更进一步的完善了Bean与数据库表的对应。自定Bean还需要注意的是：<br/>
     * 主键设置，<code>Ekey()</code>
     *
     * @param clazz Bean对象
     */
    public void ds(Object clazz) {
        insert(clazz);
    }

    /**
     * 保存一个Object对象到数据库。并返回主键集合<br/>
     * 这个Object应该是一个标准的Bean，具有<code>get()</code>,<code>set()</code>方法。它是一个自定义的Bean不需要任何继承或和系统关联。
     * 系统提供简单的Bean注解，更进一步的完善了Bean与数据库表的对应。自定Bean还需要注意的是：<br/>
     * 主键设置，<code>Ekey()</code>
     *
     * @param clazz Bean对象
     */
    public List<Object> dsrk(Object clazz) {
        Object key = insertReturnKey(clazz);
        List<Object> keys = new ArrayList<Object>();
        if (key != null) {
            keys.add(key);
        }
        return keys;
    }

    /**
     * 保存一个Dao对象.
     * 并返回保存该实体后由数据库返回的主键集合。
     *
     * @return 该实体的 <code>List(Object)</code> 主键集
     */
    public List<Object> dsrk(DAO d) {
        Map map = d.getCm();
        String sql = AppendDaoSQLUtils.generateInsertSql(d);
        if (sql == null) return null;
        if (isDebug) LogUtil.debug("Dao SaveRK Sql:" + sql);
        try {
            return save(sql, map);
        } catch (DaoException e) {
            throw new TarnDataException("Insert 失败: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new TarnDataException("Insert 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存实体。
     * 没有返回值,方法可同   public List<Object> save(String sql, Map<String, Object> map) 一致。系统专用
     * <p>2.1：委托 {@link OrmTemplate} 参数化执行（5 个位置参数顺序不变）。
     *
     * @param sql SQL语句
     * @param map 参数集
     */
    public void dsexec(String sql, Map<String, Object> map) throws DaoException, SQLException {
        List<Object> params = new ArrayList<Object>();
        params.add(map.get("precSql"));
        params.add(map.get("realSql"));
        params.add(map.get("ip"));
        params.add(map.get("addr"));
        params.add(map.get("time"));
        OrmTemplate.shared().exec(new com.dfrdev.tarn.core.sql.ParameterizedSql(sql, params));
    }


    /**
     * 修改一个Dao对象。 <br/>
     * 这里需要注意的是这儿只是单对象修改。根据主键来更新Dao对应数据库的某条记录.<br/>因此你需要指定主键值.默认主键名称为:id.若有不同则需要<code>Dao.setPrimKey()</code>。
     *
     * @param d Dao类
     */
    public void du(DAO d) {
        Map map = d.getCm();
        String sql = AppendDaoSQLUtils.generateUpdateSql(d);
        if (sql == null) return;
        if (isDebug) LogUtil.debug("Dao Update Sql:" + sql);
        updaten(sql, map);
    }

    /**
     * 更新一个Object对象。
     * 方法调用和<code>save(Object clazz)</code>一致
     *
     * @param clazz 需要更新的对象。
     */
    public void du(Object clazz) {
        updateRows(clazz);
    }

    /**
     * 按指定字段更新对象（非主键定位）。
     * <p>
     * 保留老语义：未设置该字段时会退化为“ID 条件”，建议优先使用 {@link #updateRows(Object)}。
     *
     * @param clazz          需要更新的对象。
     * @param updateFieldKey 定位记录的字段名。
     */
    public void du(Object clazz, String updateFieldKey) {
        AppendReflectSQLEntity arse = AppendReflectSQLUtils.parsePackagingUpdate(clazz, updateFieldKey);
        String sql = AppendDaoSQLUtils.generateUpdateSql(arse.getTableName(), arse.getFileds(), arse.getPk());
        if (sql == null) return;
        if (isDebug) LogUtil.debug("Object Update Sql:" + sql);
        updaten(sql, arse.getValueKey());
    }

    /**
     * 根据SQL语句、参数集和行映射器 获取一个实体。
     * 若数据库中有多条满足条件的记录，则返回第一条。
     * <p>该方法需要一个行映射器 <code>RowMapper</code>，
     * 用于实现从 <code>ResultSet</code> 结果集到实体的映射工作。
     * <p>例如#
     * <blockquote><pre>
     * User user = (User)dao.get(sql, map, new RowMapper() {
     * <p/>
     * 	public Object mapRow(ResultSet rs) throws SQLException {
     * 		User user = new User();
     * 		user.setId(rs.getInt("id"));
     * 		user.setName(rs.getString("name"));
     * 		user.setPassword(rs.getString("password"));
     * 		return user;
     *    }
     * });
     * </pre></blockquote>
     *
     * @param sql       SQL语句
     * @param map       参数集
     * @param rowMapper 行映射器
     * @return 符合条件的对象
     * @see com.dfrdev.tarn.core.RowMapper
     */
    public Object dgo(String sql, Map<String, Object> map, RowMapper rowMapper) throws SQLException {
        return get(sql, map, rowMapper);
    }

    /**
     * 到数据库中查询并返回单条记录. <br >
     * 如果查询到的记录不止一条,此方法将返回该记录集的第一条数据.<br />
     *
     * @param tmb 参数对象。
     * @return Dao 查询出来的Dao对象
     */
    public DAO dd(Model tmb, String pageUrl) {
        tmb.pageSize = 1;
        tmb.absPage = 1;
        List<DAO> list = pagerList(DAO.class, tmb, pageUrl).getDatas();
        DAO d = new DAO(tmb.table);
        for (DAO o : list) {
            d = o;
            d.setNull(false);
        }
        return d;
    }

    /**
     * 到数据库里查询返回一个Dao[]数组多条记录.<br/>
     *
     * @param tmb 参数对象。
     * @return Dao[] Dao数组
     */
    public DAO[] dds(Model tmb, String pageUrl) {
        List<DAO> list = dpa(tmb, pageUrl).getDatas();
        DAO d[] = new DAO[list.size()];
        int i = 0;
        for (DAO o : list) {
            o.setNull(false);
            d[i++] = o;
        }
        return d;
    }


    /**
     * 到数据库里查询返回一个Object[]数组多条记录.<br/>
     *
     * @param tmb 参数对象。
     * @return Object[] Object数组
     */
    public List dds(Class clazz, Model tmb, String pageUrl) {
        List list = dpa(clazz, tmb, pageUrl).getDatas();
//        Object d[] = new Object[list.size()];
//
//        int i = 0;
//        for (Object o : list) {
//            d[i++] = o;
//        }
        return list;
    }

    /**
     * 到数据库中查询并返回单条记录. <br >
     * 如果查询到的记录不止一条,此方法将返回该记录集的第一条数据.<br />
     *
     * @param clazz 查询映射的类。
     * @param tmb   参数对象。
     * @return Dao 查询出来的Dao对象
     */
    public Object dd(Class clazz, Model tmb, String pageUrl) throws IllegalAccessException, InstantiationException {
        tmb.pageSize = 1;
        tmb.absPage = 1;
        List list = dpa(clazz, tmb, pageUrl).getDatas();
        Object d = null;
        if (list.size() > 0) {
            for (Object o : list) d = o;
        } else {
            d = clazz.newInstance();
        }
        return d;
    }

    /**
     * 指定SQL语句查询数据,不分页，封装到Dao类<br/>
     *
     * @param sql sql查询语句
     * @throws java.sql.SQLException
     */
    public List<DAO> dds(String sql) throws SQLException {
        return super.pager(DAO.class, sql);
    }

    /**
     * 指定SQL语句查询数据,不分页，封装到指定类<br/>
     * 这个方法其实和<code>pager(Class clazz, String sql, int pageSize, int absPage, String thisUrl) </code>很类似，但为什么要提供这样一个方法呢，用上面的方法也可以解决问题。<br/>
     * 我们可以做一个分析，上面方法根据SQL语句查询数据然后封装到指定的<code>clazz</code>,在封装到分页类，重要的它使用了游标，个人任何游标是非常昂贵的，非常紧缺的，占资源的。为什么这么说：<br/>
     * 设想一下，Oracle数据库默认是300，假设你一个导入Excel文件的功能程序，这个Excel有1000行（呵呵，这还算少的）其中还对行数据进行判断，比较的负责的SQL语句,使用系统的<code>Model()</code>不是很好处理，这时你肯定会想自己写SQL语句(换了是我也会这样)。<br/>
     * 这时你就会选择上面的方法，那么它的工作原理是这样的：每循环一行查询判断一次（或不只一次）查询了整张表并开启了游标，然后向数据库在插入数据，这一次的循环游标是不会关闭因为你在保存是有用到它，而且是同一个链接。整个循环结束了资源才会被释放。那么你开了多少个游标呢？这显然就会抛异常，而且这速度就非常慢的。<br/>
     * 如果是使用当前方法，他不会抛出异常，而且速度会提成非常多，它只是把查询的数据到你的类到List中没做其它额外的事情，他也不会查询整张表。当然上面的方法也是非常有必要存在的不是说一点作用没有
     *
     * @param sql sql查询语句
     * @throws java.sql.SQLException
     */
    public List dds(Class clazz, String sql) throws SQLException {
        return super.pager(clazz, sql);
    }

    /**
     * 查询数据分页。<br/>
     * 根据传人的Tmb，查出数据并映射到<code>Dao</code>后，组装到<code>Pager</code>分页类。<br/>
     *
     * @param model 数据库查询语句组合类
     * @throws java.sql.SQLException
     */
    public Pager dpa(Model model, String pageUrl) {
        return pager(DAO.class, model.toSql(), model.pageSize, model.absPage, pageUrl);
    }

    /**
     * 查询数据分页。<br/>
     * 根据传人的Tmb，查出数据并映射到 clazz后，组装到<code>Pager</code>分页类。<br/>
     * 需要注意的是：这里不需要映射系统的<code>Dao</code>类，有可同方法调用：<code>pager(Model tmb)</code>.除系统自带的<code>Dao()</code>类，自定义的Bean的属性应与待查询数据库字段名对应，否则会导致查询失败<br/>
     *
     * @param clazz 需要映射的类
     * @param model 数据库查询语句组合类
     * @return Pager  分页组装类
     * @throws java.sql.SQLException
     */
    public Pager dpa(Class clazz, Model model, String pageUrl) {
        return pager(clazz, model.toSql(), model.pageSize, model.absPage, pageUrl);
    }

    /**
     * 指定SQL语句查询数据分页。<br/>
     * 查出数据并映射到<code>Dao</code>后，组装到<code>Pager</code>分页类。<br/>
     * 提示：<br/>该方法处理大数据性能是比较低下，因为它每次都是查询整张表数据，其原理是通过<code>rs.absolute()</code>.建议使用<code>pager(Model tmb)</code>
     *
     * @param sql       查询语句
     * @param pageSize  指定分页大小
     * @param pageIndex 指定当前页
     * @return Pager  分页组装类
     * @throws java.sql.SQLException 如果执行SQL发生错误,将抛出此异常
     */
    public Pager dpa(String sql, int pageSize, int pageIndex, String pageUrl) {
        return pager(DAO.class, sql, pageSize, pageIndex, pageUrl);
    }


    /**
     * 此方法弥补了查询分页使用游标，将sql语句直接生成带分页的SQL语句。耗资低、效率高<br>
     * 保留使用流标的形式分页是为了，一些负责SQL语句，最好使用以下方法。原因就不细说了<br/>
     *
     * @param clazz    需要转化的类
     * @param sql      sql查询语句
     * @param pageSize 指定分页大小.不指定则传入0
     * @param pageSize 指定当前页
     * @throws java.sql.SQLException 如果执行SQL发生错误,将抛出此异常
     */
    public Pager dpa(Class clazz, String sql, int pageSize, int pageIndex, String pageUrl) {
        return pager(clazz, sql, pageSize, pageIndex, pageUrl);

    }

    /**
     * 根据SQL语句查询语句返回封装好的Pager对象<br/>
     * Pager是个分页对象。提示：<br/>该方法处理大数据性能是比较低下，并且使用了游标<code>pager()</code>因为它每次都是查询整张表数据，其原理是通过<code>rs.absolute()</code>.
     *
     * @param clazz     需要转化的类
     * @param sql       sql查询语句
     * @param pageSize  指定分页大小.不指定则传入0
     * @param pageIndex 指定当前页
     * @throws java.sql.SQLException 如果执行SQL发生错误,将抛出此异常
     */
    public Pager dpabs(Class clazz, String sql, int pageSize, int pageIndex, String pageUrl) {
        return pagerAbsolute(clazz, sql, pageSize, pageIndex, pageUrl);

    }


    /**
     * 根据SQL语句、参数集和行映射器 获取一个实体
     * <p>若数据库中有多条满足条件的记录，则返回第一条。
     * <p>该方法需要一个行映射器 <code>RowMapper</code>，
     * 用于实现从 <code>ResultSet</code> 结果集到实体的映射工作。
     * <p>例如#
     * <blockquote><pre>
     * User user = (User)dao.get(sql, map, new RowMapper() {
     * <p/>
     * 	public Object mapRow(ResultSet rs) throws SQLException {
     * 		User user = new User();
     * 		user.setId(rs.getInt("id"));
     * 		user.setName(rs.getString("name"));
     * 		user.setPassword(rs.getString("password"));
     * 		return user;
     *    }
     * });
     * </pre></blockquote>
     *
     * @param sql       SQL语句
     * @param map       参数集
     * @param rowMapper 行映射器
     * @return 符合条件的对象
     * @see com.dfrdev.tarn.core.RowMapper
     */
    public Object dqu(String sql, Map<String, Object> map, RowMapper rowMapper) {
        return this.get(sql, map, rowMapper);
    }

    public Object dfid(Class tClass, Object o) throws SQLException {
        return dfid(tClass, o, "*");
    }

    public Object dfid(Class tClass, Object o, String fields) {
        AppendReflectSQLEntity arse = AppendReflectSQLUtils.parsePackagingSelectByKey(tClass, o);
        // Tarn 2.0+: 参数化路径，替代有注入风险的 generateSELECTSqlObj 拼接
        com.dfrdev.tarn.core.sql.ParameterizedSql ps =
                AppendDaoSQLUtils.generateSelectSqlParameterized(arse.getTableName(), fields, arse.getValueKey(), arse.getOrderBy());
        String sql = AppendAbstractUtils.generatePageFragmentSql(ps.getSql(), 1, 1);
        if (isDebug) LogUtil.debug("Object query fineById Sql:" + sql);
        List<Object> list = queryEntityListUnchecked(sql, ps.getParams(), tClass);
        return list.size() > 0 ? list.get(0) : null;
    }

    public Object dfi(Object clazz) throws SQLException {
        return dfi(clazz, "*");
    }

    public Object dfi(Object clazz, String fields) {
        AppendReflectSQLEntity arse = AppendReflectSQLUtils.parsePackagingSelectById(clazz);
        com.dfrdev.tarn.core.sql.ParameterizedSql ps =
                AppendDaoSQLUtils.generateSelectSqlParameterized(arse.getTableName(), fields, arse.getValueKey(), arse.getOrderBy());
        String sql = AppendAbstractUtils.generatePageFragmentSql(ps.getSql(), 1, 1);
        if (isDebug) LogUtil.debug("Object query fineById Sql:" + sql);
        List<Object> list = queryEntityListUnchecked(sql, ps.getParams(), clazz.getClass());
        return list.size() > 0 ? list.get(0) : null;
    }

    /**
     * 查询对象，返回单个对象记录。不指定查询字段默认查询全部
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象
     */
    public Object dqu(Object clazz) throws SQLException {
        return dqu(clazz, "*");
    }

    /**
     * 传人SQL语句查询对象，返回单个对象记录
     *
     * @param clazz 要查询的对象
     * @param sql   SQL查询语句
     * @return 返回查询结果后的对象
     */
    public Object dfi(Class clazz, String sql) {
        Object val = null;
        if (sql == null) return val;
        if (isDebug) LogUtil.debug("query Sql: {}" ,sql);
        sql = AppendAbstractUtils.generatePageFragmentSql(sql, 1, 1);
        List<Object> list = super.pager(clazz, sql);
        if (list.size() > 0) {
            val = list.get(0);
        }
        return val;
    }

    /**
     * 查询对象，返回单个对象记录。指定查询字段
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象
     */
    public Object dqu(Object clazz, String fields) {
        AppendReflectSQLEntity arse = AppendReflectSQLUtils.parsePackagingSelect(clazz);
        com.dfrdev.tarn.core.sql.ParameterizedSql ps =
                AppendDaoSQLUtils.generateSelectSqlParameterized(arse.getTableName(), fields, arse.getValueKey(), arse.getOrderBy());
        String sql = AppendAbstractUtils.generatePageFragmentSql(ps.getSql(), 1, 1);
        if (isDebug) LogUtil.debug("Object query Sql:" + sql);
        List<Object> list = queryEntityListUnchecked(sql, ps.getParams(), clazz.getClass());
        return list.size() > 0 ? list.get(0) : null;
    }

    /**
     * 查询对象集合，返回多个对象记录的集合。不指定查询字段默认查询全部
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象的集合
     */
    public List dqus(Object clazz) throws SQLException {
        return dqus(clazz, "*");
    }

    /**
     * 查询对象集合，返回多个对象记录的集合。指定查询字段
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象的集合
     */
    public List dqus(Object clazz, String fields) {
        AppendReflectSQLEntity arse = AppendReflectSQLUtils.parsePackagingSelect(clazz);
        com.dfrdev.tarn.core.sql.ParameterizedSql ps =
                AppendDaoSQLUtils.generateSelectSqlParameterized(arse.getTableName(), fields, arse.getValueKey(), arse.getOrderBy());
        if (isDebug) LogUtil.debug("Object query Sql:" + ps.getSql());
        return queryEntityListUnchecked(ps.getSql(), ps.getParams(), clazz.getClass());
    }

    /**
     * 查询失败统一抛 TarnDataException（由 OrmTemplate 包装），保持老方法签名不抛 checked 异常
     */
    private List<Object> queryEntityListUnchecked(String sql, List<Object> params, Class clazz) {
        return queryEntityList(sql, params, clazz);
    }

    /**
     * 指定SQl语句，查询对象集合，返回多个对象的集合
     */
    public List dqus(Class clazz, String sql) {
        return super.pager(clazz, sql);
    }

    /**
     * 根据类、SQL语句和参数集 获取一个实体
     * <p>若数据库中有多条满足条件的记录，则返回第一条。
     * <p>该方法需要指定需要获取的实体的类型 <code>Class</code>，
     * 根据指定的类型，将根据SQL查询出的结果集自动填充到该实体中。
     * <br>映射规则为SQL语句中所要查询内容的别名与该实体的属性名一致。
     *
     * @param clazz 实体类型
     * @param sql   SQL语句
     * @param map   参数集
     * @return 符合条件的对象
     */
    public <T> T get(Class<T> clazz, String sql, Map<String, Object> map) {
        return super.get(clazz, sql, map);
    }

    /**
     * 删除一个Dao对象
     */
    public void ddel(DAO d) {
        Map map = d.getCm();
        String sql = AppendDaoSQLUtils.generateDeleteSql(d);
        if (sql == null) return;
        if (isDebug) LogUtil.debug("Dao Delete Sql:" + sql);
        updaten(sql, map);
    }

    /**
     * 删除一个Object对象（严格按主键删除）。
     * <p>
     * 老实现用“所有非空字段”拼 WHERE，主键为空时会退化成按其它字段批量删除，这里已修正为仅按主键。
     */
    public void ddel(Object clazz) {
        deleteRows(clazz);
    }

    /**
     * 根据给定的SQL语句执行更新(UPDATE, INSERT,DELETE)查询.
     */
    public int dexe(String sql) {
        return super.exec(sql);
    }

    /**
     * 删除实体
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 数据库中受影响的记录数
     */
    public int ddel(String sql, Map<String, Object> map) throws DaoException, SQLException {
        return this.update(sql, map);
    }

    protected static boolean isSaveSQLLOGDB = Boolean.parseBoolean(Tarn.sysDevConf("isSaveSQLLOGDB"));
    protected static boolean isDebug = Boolean.parseBoolean(Tarn.sysDevConf("isDebug"));

}
