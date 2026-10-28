package com.dfrdev.tarn.core.db;

import com.dfrdev.tarn.plugin.orm.QueryBuffer;

import java.sql.SQLException;
import java.util.List;

import com.dfrdev.tarn.core.web.Pager;
/**
 * 常用的数据库服务操作接口
 */
public interface Service<T> {
    /**
     * 开启事物
     */
    public void openTransaction() throws SQLException;

    /**
     * 提交事物
     */
    public void submitTransaction() throws SQLException;

    /**
     * 回滚事物
     */
    public void rollbackTransaction() throws SQLException;

    /**
     * 对象持久化
     */
    public void persist(T t);

    /**
     * 删除对象
     */
    public void remove(T t);

    /**
     * 保存一个对象等同与persist(T t)
     */
    public void save(T t);

    /**
     * 持久化对象并返回生成的主键
     *
     * @return List
     */
    public List saveRK(T t);

    /**
     * 返回一个查询后的对象
     */
    public T merge(T t);

    /**
     * 根据主键值查询对象,并指定查询字段
     *
     * @param ID_or_Entity 主键值或实例对象
     */
    public T findById(Object ID_or_Entity);

    /**
     * 根据主键值查询对象,并指定查询字段
     *
     * @param ID_or_Entity 主键值或实例对象
     */
    public T findById(Object ID_or_Entity, String fields);

    /**
     * 查询所有数据
     */
    public List<T> findAll();

    /**
     * 指定SQL语句查询，返回多条记录
     */
    public List<T> findByQuery(String jpql);


    /**
     * 指定sql语句查询返回单条记录
     */
    public @Deprecated  // 存在 SQL注入风险，请使用 ParameterizedSql + OrmTemplate 参数化查询
    T query(String sql);

    public T query(QueryBuffer queryBuffer);

    /**
     * 指定sql语句查询返回多条记录
     */
    public @Deprecated  // 存在 SQL注入风险，请使用 ParameterizedSql + OrmTemplate 参数化查询
    List<T> querys(String sql);
    public List<T> querys(QueryBuffer queryBuffer);



    /**
     * 查询多条记录
     */
    public List<T> querys(Object bean);

    public List<T> querys(Object bean, String fields);


    public Pager<T> findPager(Integer pageSize, Integer page,String querySql,String countSql);
    public Pager<T> findPager(Integer pageSize, Integer page,QueryBuffer queryBuffer,String countSql);


    public void execSql(String sql);

}
