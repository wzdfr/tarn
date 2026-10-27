package examples.demo.entity;

import cc.ruixing.framework.tarn.aop.annotation.EFilter;
import cc.ruixing.framework.tarn.aop.annotation.Ekey;
import cc.ruixing.framework.tarn.aop.annotation.Etable;
import cc.ruixing.framework.tarn.aop.annotation.EUpdateNull;
import cc.ruixing.framework.tarn.aop.annotation.ORMFilter;

/**
 * 演示实体：用户（Tarn 2.0 ORM）。
 * <p>
 * 映射规则（与老框架完全一致）：
 * <ul>
 *     <li>{@code @Etable("demo_user")} 指定表名，不写则用类名大写</li>
 *     <li>字段无注解时，数据库列名 = 字段名大写（{@code name} → {@code NAME}），
 *     因此实体字段建议使用单段小写命名</li>
 *     <li>{@code @Ekey} 标记主键；{@code isAutoIncrement=true} 时 INSERT 不写入主键列，
 *     更新/删除按主键定位</li>
 *     <li>{@code @EFilter} 保存与更新时跳过该字段（如库默认值/审计字段）</li>
 *     <li>{@code @EUpdateNull(false)} 更新时该字段为 null 则不覆盖库中原值</li>
 *     <li>{@code @ORMFilter} 完全解除映射（VO 展示字段，不入库）</li>
 * </ul>
 * 对应 DDL：
 * <pre>
 * CREATE TABLE demo_user (
 *     ID      BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     NAME    VARCHAR(64) NOT NULL,
 *     DEPTID  BIGINT,
 *     STATUS  INT DEFAULT 1,
 *     REMARK  VARCHAR(255)
 * );
 * </pre>
 *
 * @author dfr
 */
@Etable("demo_user")
public class DemoUser {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String name;

    /** 部门 ID，列名 DEPTID */
    private Long deptid;

    private Integer status;

    /** 备注：更新时传 null 表示“不修改”，显式清空请传空串 */
    @EUpdateNull(false)
    private String remark;

    /** 仅用于 JOIN 查询结果展示，不参与 ORM 映射（@ORMFilter） */
    @ORMFilter
    private String deptName;

    /** 由数据库默认值填充，保存/更新时不写入（@EFilter） */
    @EFilter
    private java.util.Date createtime;

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

    public Long getDeptid() {
        return deptid;
    }

    public void setDeptid(Long deptid) {
        this.deptid = deptid;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public java.util.Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(java.util.Date createtime) {
        this.createtime = createtime;
    }
}
