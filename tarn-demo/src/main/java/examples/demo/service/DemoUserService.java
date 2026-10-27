package examples.demo.service;

import com.dfrdev.tarn.core.DaoException;
import com.dfrdev.tarn.core.Q;
import com.dfrdev.tarn.core.sql.ParameterizedSql;
import com.dfrdev.tarn.core.sql.QueryBuilder;
import com.dfrdev.tarn.core.sql.SqlBuilder;
import examples.demo.entity.DemoUser;
import examples.demo.mapper.DemoUserMapper;

import java.util.List;
import java.util.Map;

/**
 * 演示服务层：集中展示 Tarn 2.0 的四种数据访问姿势。
 * <ol>
 *     <li><b>ORM 实体方法</b>：{@code save / update / delete / findById / querys}（老 API，零改动）</li>
 *     <li><b>SqlBuilder 参数化查询</b>：手写 SQL 但值全部绑定，杜绝注入</li>
 *     <li><b>QueryBuilder 多表 JOIN + 方言分页</b>：{@code queryMaps} 返回 Map 行</li>
 *     <li><b>MyBatis Mapper</b>：复杂 SQL / 团队已习惯 MyBatis 的模块</li>
 * </ol>
 * 说明：{@link DaoException} 是老框架遗留的 {@code extends Throwable} 类型（非 Exception），
 * 因此方法签名需显式声明。新项目建议在服务层统一捕获并转换为业务异常。
 *
 * @author dfr
 */
public class DemoUserService {

    /** Q 是 ORM 门面（Q extends DB extends DAOAbstract），每次 new 无状态、线程安全 */
    private final Q q = new Q();

    // ==================== 1. ORM 实体方法（老 API，推荐存量改造沿用） ====================

    /**
     * 保存：主键自增时由数据库生成，可通过 {@code saveRK} 拿回主键。
     */
    public void save(DemoUser user) {
        q.save(user);
    }

    /**
     * 保存并返回数据库生成的主键集合
     */
    @SuppressWarnings("unchecked")
    public List<Object> saveAndReturnKey(DemoUser user) {
        return q.saveRK(user);
    }

    /**
     * 按主键更新：只更新非 null 字段；
     * 实体字段标注 {@code @EUpdateNull(true)} 时该字段为 null 也会写入。
     */
    public void update(DemoUser user) {
        q.update(user);
    }

    /**
     * 按主键删除
     */
    public void delete(DemoUser user) {
        q.delete(user);
    }

    /**
     * 按主键查询（使用实体上的 @Ekey 值）
     */
    public DemoUser findById(Long id) throws DaoException {
        DemoUser condition = new DemoUser();
        condition.setId(id);
        try {
            return (DemoUser) q.findById(DemoUser.class, condition);
        } catch (java.sql.SQLException e) {
            // 老 API 抛 SQLException，统一转成框架的 DaoException，便于上层只处理一种异常
            throw new DaoException("findById failed: " + e.getMessage(), e);
        }
    }

    /**
     * 条件查询：以实体非空字段作为 AND 条件
     */
    @SuppressWarnings("unchecked")
    public List<DemoUser> queryByExample(DemoUser condition) throws DaoException {
        try {
            return (List<DemoUser>) (List<?>) q.querys(condition);
        } catch (java.sql.SQLException e) {
            throw new DaoException("querys failed: " + e.getMessage(), e);
        }
    }

    // ==================== 2. SqlBuilder 参数化查询（新 API，防注入） ====================

    /**
     * 分页列表：手写条件 + 参数绑定 + 方言分页。
     * <p>
     * 注意：{@code SqlBuilder.create(...)} 的初始片段使用 {@code #{name}} + {@link SqlBuilder#bind}，
     * {@code where/and/append} 的片段使用 {@code ?} + 可变参数。
     */
    public List<DemoUser> pageByStatus(Integer status, int page, int size) throws DaoException {
        ParameterizedSql ps = SqlBuilder
                .select("ID, NAME, DEPTID, STATUS, REMARK")
                .from("demo_user")
                .where("STATUS = #{status}").bind("status", status)
                .orderBy("ID", false)
                .limit((Math.max(page, 1) - 1) * size, size)
                .build();
        @SuppressWarnings("unchecked")
        List<DemoUser> list = (List<DemoUser>) (List<?>) q.query(ps, DemoUser.class);
        return list;
    }

    /**
     * 统计
     */
    public int countByStatus(Integer status) throws DaoException {
        return q.count(SqlBuilder.create("SELECT COUNT(0) FROM demo_user")
                .where("STATUS = ?", status)
                .build());
    }

    /**
     * 参数化更新（批量上下架）：值全部走 PreparedStatement 绑定
     */
    public int updateStatus(Long id, Integer status) throws DaoException {
        return q.exec(SqlBuilder.create("UPDATE demo_user SET STATUS = #{status} WHERE ID = #{id}")
                .bind("status", status)
                .bind("id", id)
                .build());
    }

    // ==================== 3. QueryBuilder 多表 JOIN + 方言分页 ====================

    /**
     * 用户 + 部门联表查询。分页 SQL 由当前数据库方言自动生成
     * （MySQL LIMIT / Oracle ROWNUM / 达梦、金仓、GaussDB 等自动适配）。
     *
     * @return 每行 Map：ID / NAME / DEPTID / STATUS / DEPTNAME
     */
    public List<Map<String, Object>> pageWithDept(Integer status, int page, int size) throws DaoException {
        ParameterizedSql ps = QueryBuilder
                .select("u.ID, u.NAME, u.DEPTID, u.STATUS, d.NAME AS DEPTNAME")
                .from("demo_user u")
                .leftJoin("demo_dept d").on("u.DEPTID = d.ID")
                .where("u.STATUS = #{status}").bind("status", status)
                .orderBy("u.ID", false)
                .page(page, size)
                .build();
        return q.queryMaps(ps);
    }

    /**
     * JOIN 结果映射到实体列表（@ORMFilter 的 deptName 会被填充）
     */
    @SuppressWarnings("unchecked")
    public List<DemoUser> listWithDeptAsEntity(Integer status) throws DaoException {
        ParameterizedSql ps = QueryBuilder
                .select("u.ID, u.NAME, u.DEPTID, u.STATUS, d.NAME AS DEPTNAME")
                .from("demo_user u")
                .leftJoin("demo_dept d").on("u.DEPTID = d.ID")
                .where("u.STATUS = ?", status)
                .orderBy("u.ID", false)
                .build();
        return (List<DemoUser>) (List<?>) q.query(ps, DemoUser.class);
    }

    // ==================== 4. MyBatis Mapper ====================

    /**
     * MyBatis 查询（需 tarn.mybatis.enabled=true）。
     * 会话由框架在请求结束时自动关闭。
     */
    public DemoUser mybatisFindById(Long id) {
        return MybatisKitHolder.mapper().selectById(id);
    }

    /**
     * MyBatis 插入（回填自增主键到实体）
     */
    public int mybatisSave(DemoUser user) {
        return MybatisKitHolder.mapper().insert(user);
    }

    /**
     * MyBatis 条件列表
     */
    public List<DemoUser> mybatisListByStatus(Integer status) {
        return MybatisKitHolder.mapper().selectByStatus(status);
    }

    /**
     * 延迟解析 MyBatis 门面：未启用插件时抛 IllegalStateException 而不是类加载失败
     */
    private static final class MybatisKitHolder {
        static DemoUserMapper mapper() {
            return com.dfrdev.tarn.plugin.mybatis.MybatisKit.getMapper(DemoUserMapper.class);
        }
    }
}
