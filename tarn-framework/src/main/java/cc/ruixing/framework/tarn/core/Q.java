package cc.ruixing.framework.tarn.core;

import cc.ruixing.framework.tarn.plugin.orm.SqlBuffer;
import cc.ruixing.framework.tarn.util.LogUtil;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * ORM 对外门面（业务代码入口）。
 * <p>
 * 调用关系：{@code Controller/Service → Q（门面）→ DB（实体↔SQL 语义）→ DAOAbstract（JDBC 原语）}
 * ，{@link Tarn} 仅在此之上补充配置/分页上下文等静态能力。
 * <p>
 * <b>2.1 起写操作的行为约定：</b>
 * <ul>
 *     <li>{@code save/update/delete} 保持原签名（{@code void}），失败会抛
 *     {@link TarnDataException}（非受检），不再静默吞异常</li>
 *     <li>需要受影响行数/主键请使用新增的
 *     {@link #insert(Object)} / {@link #insertReturnKey(Object)} /
 *     {@link #updateRows(Object)} / {@link #deleteRows(Object)}</li>
 * </ul>
 */
public class Q extends DB {
    /**
     * 保存一个Dao对象到数据库。
     *
     * @param d 需要保存的Dao对象。
     */
    public void save(DAO d) {
        super.ds(d);
    }


    /**
     * 保存一个Object对象到数据库。<br/>
     * 这个Object应该是一个标准的Bean，具有<code>get()</code>,<code>set()</code>方法。它是一个自定义的Bean不需要任何继承或和系统关联。
     * 系统提供简单的Bean注解，更进一步的完善了Bean与数据库表的对应。自定Bean还需要注意的是：<br/>
     * 主键设置，<code>Ekey()</code>
     *
     * @param clazz Bean对象
     */
    public void save(Object clazz) {
        super.ds(clazz);
    }

    // ==================== 2.1 新增：带结果的写操作（推荐） ====================

    /**
     * 插入实体，返回受影响行数。
     */
    public int insert(Object entity) {
        return super.insert(entity);
    }

    /**
     * 插入实体，返回数据库生成的主键并回填到实体主键字段。
     */
    public Object insertReturnKey(Object entity) {
        return super.insertReturnKey(entity);
    }

    /**
     * 按主键更新实体，返回受影响行数。
     */
    public int updateRows(Object entity) {
        return super.updateRows(entity);
    }

    /**
     * 按主键删除实体，返回受影响行数。
     */
    public int deleteRows(Object entity) {
        return super.deleteRows(entity);
    }

    /**
     * 批量插入（同一连接、单事务），返回总受影响行数。
     */
    public int batchInsert(List<?> entities) {
        return super.batchInsert(entities);
    }

    // ==================== 事务（委托 OrmTemplate，异常统一为非受检 TarnDataException） ====================

    /**
     * 开启事务（当前线程绑定连接）。
     * <p>更推荐声明式用法：{@code Orm.template().inTransaction(callback)}，
     * 正常返回提交、抛异常自动回滚。
     */
    public void beginTransaction() {
        cc.ruixing.framework.tarn.orm.OrmTemplate.shared().beginTransaction();
    }

    /**
     * 提交事务。
     */
    public void commit() {
        cc.ruixing.framework.tarn.orm.OrmTemplate.shared().commit();
    }

    /**
     * 回滚事务。
     */
    public void rollback() {
        cc.ruixing.framework.tarn.orm.OrmTemplate.shared().rollback();
    }

    /**
     * 保存一个Object对象到数据库。并返回主键集合<br/>
     * 这个Object应该是一个标准的Bean，具有<code>get()</code>,<code>set()</code>方法。它是一个自定义的Bean不需要任何继承或和系统关联。
     * 系统提供简单的Bean注解，更进一步的完善了Bean与数据库表的对应。自定Bean还需要注意的是：<br/>
     * 主键设置，<code>Ekey()</code>
     *
     * @param clazz Bean对象
     */
    public List<Object> saveRK(Object clazz) {
        return super.dsrk(clazz);
    }

    /**
     * 保存一个Dao对象.
     * 并返回保存该实体后由数据库返回的主键集合。
     *
     * @return 该实体的 <code>List(Object)</code> 主键集
     */
    public List<Object> saveRK(DAO d) {
        return super.dsrk(d);
    }

    /**
     * 保存实体。
     * 没有返回值,方法可同   public List<Object> save(String sql, Map<String, Object> map) 一致。系统专用
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 该实体的 <code>List(Object)</code> 主键集
     */
    public void saveEXEC(String sql, Map<String, Object> map) throws DaoException, SQLException {
        super.dsexec(sql, map);
    }


    /**
     * 修改一个Dao对象。 <br/>
     * 这里需要注意的是这儿只是单对象修改。根据主键来更新Dao对应数据库的某条记录.<br/>因此你需要指定主键值.默认主键名称为:id.若有不同则需要<code>dao.setPrimKey()</code>。
     *
     * @param d Dao类
     */
    public void update(DAO d) {
        super.du(d);
    }

    /**
     * 更新一个Object对象。
     * 方法调用和<code>save(Object clazz)</code>一致
     *
     * @param clazz 需要更新的对象。
     */
    public void update(Object clazz) {
        super.du(clazz);
    }

    /**
     * 更新一个Object对象。
     * 方法调用和<code>save(Object clazz)</code>一致
     *
     * @param object             需要更新的对象。
     * @param updateFieldKeyName 当更新一条数据并不是根据主键字段值更新，请指定字段名即可。
     */
    public void update(Object object, String updateFieldKeyName) {
        super.du(object, updateFieldKeyName);
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
     * @param rowMapper 行映射器
     * @return 符合条件的对象
     * @see cc.ruixing.framework.tarn.core.RowMapper
     */
    public Object getObj(String sql, Map<String, Object> map, RowMapper rowMapper) throws SQLException {
        return get(sql, map, rowMapper);
    }

    /**
     * 到数据库中查询并返回单条记录. <br >
     * 如果查询到的记录不止一条,此方法将返回该记录集的第一条数据.<br />
     *
     * @param tmb 参数对象。
     * @return Dao 查询出来的Dao对象
     */
    public DAO dao(Model tmb) throws SQLException {
        return super.dd(tmb, "");
    }

    /**
     * 到数据库中查询并返回单条记录. <br >
     * 如果查询到的记录不止一条,此方法将返回该记录集的第一条数据.<br />
     *
     * @param clazz 查询映射的类。
     * @param tmb   参数对象。
     * @return Dao 查询出来的Dao对象
     */
    public Object dao(Class clazz, Model tmb) throws SQLException, IllegalAccessException, InstantiationException {
        return super.dd(clazz, tmb, "");
    }

    /**
     * 到数据库里查询返回一个Dao[]数组多条记录.<br/>
     *
     * @param tmb 参数对象。
     * @return Dao[] Dao数组
     */
    public DAO[] daos(Model tmb) throws SQLException {
        return super.dds(tmb, "");
    }

    /**
     * 到数据库里查询返回一个Object[]数组多条记录.<br/>
     *
     * @param tmb 参数对象。
     * @return Object[] Object数组
     */
    public List daos(Class clazz, Model tmb) throws SQLException {
        return super.dds(clazz, tmb, "");
    }


    /**
     * 指定SQL语句查询数据,不分页，封装到Dao类<br/>
     *
     * @param sql sql查询语句
     * @throws java.sql.SQLException
     */
    public List<DAO> daos(String sql) throws SQLException {
        return super.dds(sql);
    }

    public List<DAO> daos(SqlBuffer sqlBuffer) throws SQLException {
        if (sqlBuffer == null) return null;
        return super.pager(DAO.class, sqlBuffer.getSql(), sqlBuffer.getParams());
    }

    public List<DAO> daos(Class clazz,SqlBuffer sqlBuffer) throws SQLException {
        if (sqlBuffer == null) return null;
        return super.pager(clazz, sqlBuffer.getSql(), sqlBuffer.getParams());
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
    public List daos(Class clazz, String sql) throws SQLException {
        return super.dds(clazz, sql);
    }

    /**
     * 查询数据分页。<br/>
     * 根据传人的Tmb，查出数据并映射到<code>Dao</code>后，组装到<code>Pager</code>分页类。<br/>
     *
     * @param tmb 数据库查询语句组合类
     * @throws java.sql.SQLException
     */
    public Pager pager(Model tmb) throws SQLException {
        return super.dpa(tmb, currentPageUrl());
    }


    /**
     * 查询数据分页。<br/>
     * 根据传人的Tmb，查出数据并映射到 clazz后，组装到<code>Pager</code>分页类。<br/>
     * 需要注意的是：这里不需要映射系统的<code>Dao</code>类，有可同方法调用：<code>pager(Model tmb)</code>.除系统自带的<code>Dao()</code>类，自定义的Bean的属性应与待查询数据库字段名对应，否则会导致查询失败<br/>
     *
     * @param clazz 需要映射的类
     * @param tmb   数据库查询语句组合类
     * @return Pager  分页组装类
     * @throws java.sql.SQLException
     */
    public Pager pager(Class clazz, Model tmb) throws SQLException {
        return super.dpa(clazz, tmb, currentPageUrl());
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
    public Pager pager(String sql, int pageSize, int pageIndex) {
        return super.dpa(sql, pageSize, pageIndex, currentPageUrl());
    }


    public Pager pager(SqlBuffer sqlBuffer, Integer pageSize, Integer pageIndex) throws SQLException {
        if (sqlBuffer == null) return null;
        List list = pager(DAO.class, AppendAbstractUtils.generatePageFragmentSql(sqlBuffer.getSql(), pageSize, pageIndex), sqlBuffer.getParams());
        int pageCount = list.size();
        if (pageSize != 0) pageCount = (list.size() + pageSize - 1) / pageSize;
        return new Pager(list, pageIndex, pageSize, pageCount, list.size(), currentPageUrl());
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
    public Pager pager(Class clazz, String sql, int pageSize, int pageIndex) {
        return super.dpa(clazz, sql, pageSize, pageIndex, currentPageUrl());

    }

    public Pager pager(Class clazz, SqlBuffer sqlBuffer, Integer pageSize, Integer pageIndex)  {
        if (sqlBuffer == null) return null;
        List list = pager(clazz, AppendAbstractUtils.generatePageFragmentSql(sqlBuffer.getSql(), pageSize, pageIndex), sqlBuffer.getParams());
        int pageCount = list.size();
        if (pageSize != 0) pageCount = (list.size() + pageSize - 1) / pageSize;
        return new Pager(list, pageIndex, pageSize, pageCount, list.size(), currentPageUrl());
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
    public Pager pagerAbsolute(Class clazz, String sql, int pageSize, int pageIndex) {
        return super.dpabs(clazz, sql, pageSize, pageIndex, currentPageUrl());

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
     * @see cc.ruixing.framework.tarn.core.RowMapper
     */
    public Object query(String sql, Map<String, Object> map, RowMapper rowMapper) {
        return super.dqu(sql, map, rowMapper);
    }

    public Object findById(Class tClass, Object o) throws SQLException {
        return super.dfid(tClass, o);
    }

    public Object findById(Class tClass, Object o, String fields) throws SQLException {
        return super.dfid(tClass, o, fields);
    }

    public Object find(Object clazz) throws SQLException {
        return super.dfi(clazz);
    }

    public Object find(Object clazz, String fields) throws SQLException {
        return super.dfi(clazz, fields);
    }

    /**
     * 查询对象，返回单个对象记录。不指定查询字段默认查询全部
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象
     */
    public Object query(Object clazz) throws SQLException {
        return super.dqu(clazz);
    }

    /**
     * 传人SQL语句查询对象，返回单个对象记录
     *
     * @param clazz 要查询的对象
     * @param sql   SQL查询语句
     * @return 返回查询结果后的对象
     */
    public Object find(Class clazz, String sql) throws SQLException {
        return super.dfi(clazz, sql);
    }

    public Object find(Class clazz, SqlBuffer sqlBuffer) throws SQLException {
        Object val = null;
        if (sqlBuffer == null) return null;
        if (isDebug) LogUtil.debug("Object query Sql:" + sqlBuffer.getSql());
        String sql = AppendAbstractUtils.generatePageFragmentSql(sqlBuffer.getSql(), 1, 1);
        List<Object> list = super.pager(clazz, sql, sqlBuffer.getParams());
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
    public Object query(Object clazz, String fields) throws SQLException {
        return super.dqu(clazz, fields);
    }

    /**
     * 查询对象集合，返回多个对象记录的集合。不指定查询字段默认查询全部
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象的集合
     */
    public List querys(Object clazz) throws SQLException {
        return super.dqus(clazz);
    }

    /**
     * 查询对象集合，返回多个对象记录的集合。指定查询字段
     *
     * @param clazz 要查询的对象
     * @return 返回查询结果后的对象的集合
     */
    public List querys(Object clazz, String fields) throws SQLException {
        return super.dqus(clazz, fields);
    }

    /**
     * 指定SQl语句，查询对象集合，返回多个对象的集合
     */
    public List querys(Class clazz, String sql) {
        return super.dqus(clazz, sql);
    }

    /**
     * 指定sqlBuffer，查询对象集合，返回多个对象的集合
     */
    public List querys(Class clazz, SqlBuffer sqlBuffer) {
        if (sqlBuffer == null) return null;
        return super.pager(clazz, sqlBuffer.getSql(), sqlBuffer.getParams());
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
    public void delete(DAO d) {
        super.ddel(d);
    }

    /**
     * 删除一个Object对象
     */
    public void delete(Object clazz) {
        super.ddel(clazz);
    }

    /**
     * 根据给定的SQL语句执行更新(UPDATE, INSERT,DELETE)查询.
     */
    public int exec(String sql) {
        return super.dexe(sql);
    }

    public int exec(SqlBuffer sqlBuffer) {
        if (sqlBuffer == null) return -1;
        return super.update(sqlBuffer.getSql(), sqlBuffer.getParams());
    }

    /**
     * 删除实体
     *
     * @param sql SQL语句
     * @param map 参数集
     * @return 数据库中受影响的记录数
     */
    public int delete(String sql, Map<String, Object> map) throws DaoException, SQLException {
        return super.update(sql, map);
    }

    /**
     * 返回当前页码URL
     *
     * @return String
     */
    public String currentPageUrl() {
        String url = "";
        PageContext context = Tarn.getPageContext();
        if (context == null) {
            return url;
        }
        HttpServletRequest request = context.getRequest();
        if (request != null) {
            url = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        }
        return url;
    }
}
