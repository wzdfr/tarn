package examples.demo.mapper;

import examples.demo.entity.DemoUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 演示 MyBatis Mapper（Tarn 2.0 插件化集成）。
 * <p>
 * 无需 XML、无需在 mybatis-config.xml 中声明：{@code MybatisKit.getMapper()} 首次调用时
 * 自动向 MyBatis 注册本接口。注解中的 SQL 由 MyBatis 负责参数绑定，
 * 与框架用同一个数据源（Druid / 内置池），遵循同一套事务配置。
 * <p>
 * 适用场景：复杂 SQL、需数据库方言特定语法、或团队已习惯 MyBatis 的模块。
 *
 * @author dfr
 * @see cc.ruixing.framework.tarn.plugin.mybatis.MybatisKit
 */
public interface DemoUserMapper {

    /**
     * 按主键查询（下划线列名可用别名映射到驼峰属性）
     */
    @Select("SELECT ID AS id, NAME AS name, DEPTID AS deptid, STATUS AS status, "
            + "REMARK AS remark FROM demo_user WHERE ID = #{id}")
    DemoUser selectById(@Param("id") Long id);

    /**
     * 按状态查询列表
     */
    @Select("SELECT ID AS id, NAME AS name, DEPTID AS deptid, STATUS AS status, "
            + "REMARK AS remark FROM demo_user WHERE STATUS = #{status} ORDER BY ID DESC")
    List<DemoUser> selectByStatus(@Param("status") Integer status);

    /**
     * 插入并回填自增主键到实体
     */
    @Insert("INSERT INTO demo_user (NAME, DEPTID, STATUS, REMARK) "
            + "VALUES (#{name}, #{deptid}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DemoUser user);

    /**
     * 统计
     */
    @Select("SELECT COUNT(0) FROM demo_user WHERE STATUS = #{status}")
    int countByStatus(@Param("status") Integer status);
}
