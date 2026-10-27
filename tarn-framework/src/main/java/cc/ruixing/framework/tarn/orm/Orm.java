package cc.ruixing.framework.tarn.orm;

/**
 * ORM 工厂入口（Tarn 2.1+，组合模式）。
 * <p>
 * 业务代码<b>零继承</b>即可获得全部 ORM 能力：
 * <pre>
 *     OrmTemplate orm = Orm.template();
 *     orm.insertReturnKey(book);            // 新增并回填主键
 *     orm.updateRows(book);                 // 按主键更新
 *     orm.query(SqlBuilder.select("*").from("demo_book").build(), Book.class);
 *     orm.inTransaction(tx -&gt; { ... });     // 声明式事务
 * </pre>
 * <p>
 * 与老入口的关系：
 * <ul>
 *     <li>老代码 {@code new Q()} / {@code extends Q} / {@code new Tarn()} 继续可用，
 *     其内部同样委托到 {@link OrmTemplate#shared()}，行为完全一致</li>
 *     <li>新代码推荐 {@link Orm#template()}：不污染业务类继承体系，便于单测与替换</li>
 * </ul>
 * 未来如需多数据源，可扩展为 {@code Orm.template("dataSourceName")} 返回命名实例，
 * 现阶段单数据源共享单例。
 *
 * @author dfr
 * @since 2.1
 */
public final class Orm {

    private Orm() {
    }

    /**
     * 获取共享 ORM 引擎（无状态、线程安全）。
     */
    public static OrmTemplate template() {
        return OrmTemplate.shared();
    }
}
