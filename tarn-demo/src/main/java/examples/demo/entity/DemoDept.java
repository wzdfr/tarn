package examples.demo.entity;

import cc.ruixing.framework.tarn.aop.annotation.Ekey;
import cc.ruixing.framework.tarn.aop.annotation.Etable;

/**
 * 演示实体：部门（用于多表 JOIN 查询示例）。
 * <p>
 * DDL：
 * <pre>
 * CREATE TABLE demo_dept (
 *     ID   BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     NAME VARCHAR(64) NOT NULL
 * );
 * </pre>
 *
 * @author dfr
 */
@Etable("demo_dept")
public class DemoDept {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
