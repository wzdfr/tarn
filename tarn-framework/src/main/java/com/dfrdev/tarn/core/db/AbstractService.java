package com.dfrdev.tarn.core.db;


import com.dfrdev.tarn.plugin.orm.QueryBuffer;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;

import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.web.Pager;
/**
 * 默认的抽象服务实现
 */
public abstract class AbstractService<T> implements
        Service<T> {
    public static Tarn tarn = new Tarn();
    private Class<T> tClass;

    /**
     * 开启事物
     */
    @Override
    public void openTransaction() throws SQLException {
        tarn.transOpen();
    }

    /**
     * 提交事物
     */
    @Override
    public void submitTransaction() throws SQLException {
        tarn.transCommit();
    }

    /**
     * 回滚事物
     */
    @Override
    public void rollbackTransaction() throws SQLException {
        tarn.transRollback();
    }

    /**
     * 对象持久化保存
     *
     * @param t 泛型
     */
    @Override
    public void save(T t) {
        tarn.save(t);
    }

    /**
     * 对象持久化保存。并返回主键集合
     *
     * @param t 泛型
     */
    @Override
    public List saveRK(T t) {
        return tarn.saveRK(t);
    }

    /**
     * 查询对象集合
     *
     * @param bean 待查询对象
     */
    @Override
    public List<T> querys(Object bean) {
        List f = null;
        try {
            f = tarn.querys(bean);
        } catch (SQLException e) {
            LogUtil.error("查询对象集合失败: " + e.getMessage(), e);
        }
        return f;
    }

    /**
     * 指定SQL查询单个对象
     *
     * @param sql 查询SQL语句
     */
    @Override
    @Deprecated  // 存在 SQL注入风险，请使用 ParameterizedSql + OrmTemplate 参数化查询
    public T query(String sql) {
        T t = null;
        try {
            t = (T) tarn.find(tClass, sql);
        } catch (SQLException e) {
            LogUtil.error("指定 SQL 查询单个对象失败: " + e.getMessage(), e);
        }
        return t;
    }

    /**
     * 指定sql语句查询返回多条记录
     *
     * @param sql
     */
    @Override
    @Deprecated  // 存在 SQL注入风险，请使用 ParameterizedSql + OrmTemplate 参数化查询
    public List<T> querys(String sql) {
        return tarn.querys(tClass, sql);
    }

    /**
     * 查询对象集合
     *
     * @param bean   待查询对象
     * @param fields 指定查询字段
     */
    @Override
    public List<T> querys(Object bean, String fields) {
        List f = null;
        try {
            f = tarn.querys(bean, fields);
        } catch (SQLException e) {
            LogUtil.error("查询对象集合失败 (指定字段): " + e.getMessage(), e);
        }
        return f;
    }


    @Override
    public T query(QueryBuffer queryBuffer) {
        T t = null;
        try {
            t = (T) tarn.find(tClass, queryBuffer);
        } catch (SQLException e) {
            LogUtil.error("查询单个对象失败 (QueryBuffer): " + e.getMessage(), e);
        }
        return t;
    }

    @Override
    public List<T> querys(QueryBuffer queryBuffer) {
       return tarn.querys(tClass,queryBuffer);
    }

    /**
     * 当前时间
     */
    public Date now() {
        return new Date();
    }

    public AbstractService() {
        tClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * 对象持久化保存
     *
     * @param t 泛型
     */
    public void persist(T t) {
        tarn.save(t);
    }

    /**
     * 删除对象
     *
     * @param t 泛型
     */
    public void remove(T t) {
        tarn.delete(t);
    }

    /**
     * 对象查询
     *
     * @param t 泛型
     */
    public T merge(T t) {
        T f = null;
        try {
            f = (T) tarn.query(t);
        } catch (SQLException e) {
            LogUtil.error("对象合并失败: " + e.getMessage(), e);
        }
        return f;
    }

    /**
     * 根据主键值查询对象
     *
     * @param ID_or_Entity 主键值或实例对象
     */
    public T findById(Object ID_or_Entity) {
        T t = null;
        try {
            if (ID_or_Entity instanceof String || ID_or_Entity instanceof Integer) {
                t = (T) tarn.findById(tClass, ID_or_Entity);
            } else {
                t = (T) tarn.find(ID_or_Entity);

            }
        } catch (SQLException e) {
            LogUtil.error("根据主键查询对象失败: " + e.getMessage(), e);
        }
        return t;
    }

    /**
     * 根据主键值查询对象,并指定查询字段
     *
     * @param ID_or_Entity 主键值或实例对象
     */
    public T findById(Object ID_or_Entity, String fields) {
        T t = null;
        try {
            if (ID_or_Entity instanceof String || ID_or_Entity instanceof Integer) {
                t = (T) tarn.findById(tClass, ID_or_Entity);
            } else {
                t = (T) tarn.find(ID_or_Entity);
            }
        } catch (SQLException e) {
            LogUtil.error("根据主键查询对象失败 (指定字段): " + e.getMessage(), e);
        }
        return t;
    }


    /**
     * 查询所有数据
     */
    public List<T> findAll() {
        List f = null;
        try {
            f = tarn.querys(tClass.newInstance());
        } catch (SQLException e) {
            LogUtil.error("查询所有数据失败: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            LogUtil.error("查询所有数据失败 (实例化异常): " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LogUtil.error("查询所有数据失败 (非法访问): " + e.getMessage(), e);
        }
        return f;
    }

    /**
     * 指定SQL查询数据，返回<code>Dao</code>集合
     *
     * @param jpql 查询SQL语句
     */
    public List<T> findByQuery(String jpql) {
        List f = null;
        try {
            f = tarn.daos(tClass, jpql);
        } catch (SQLException e) {
            LogUtil.error("按 SQL 查询数据失败: " + e.getMessage(), e);
        }
        return f;
    }


    @Override
    public Pager<T> findPager(Integer pageSize, Integer page,String querySql,String countSql) {
        Pager pager = null;
        pager = tarn.pager(tClass, querySql, pageSize, page);
        if(StringUtil.isNotEmpty(countSql))  pager.setTotalRecord(tarn.count(countSql));
        pager.setCurrPageStr(pager.getPageStr(6));
        return pager;
    }


    @Override
    public Pager<T> findPager(Integer pageSize, Integer page, QueryBuffer queryBuffer, String countSql) {
        Pager pager = null;
        pager = tarn.pager(tClass, queryBuffer, pageSize, page);
        if(StringUtil.isNotEmpty(countSql))  pager.setTotalRecord(tarn.count(countSql));
        pager.setCurrPageStr(pager.getPageStr(6));
        return pager;
    }

    @Override
    @Deprecated  // 存在 SQL注入风险，请使用 ParameterizedSql + OrmTemplate 参数化查询
    public void execSql(String sql) {
        tarn.exec(sql);
    }

}
