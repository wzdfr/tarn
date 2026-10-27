package cc.ruixing.framework.tarn.orm;

/**
 * 事务回调（Tarn 2.1+，配合 {@link OrmTemplate#inTransaction(TransactionCallback)} 使用）。
 * <p>
 * 回调正常返回则提交；抛出任何异常则回滚并把异常包装为
 * {@link cc.ruixing.framework.tarn.core.TarnDataException}（非受检）抛出。
 *
 * <pre>
 *     Long id = Orm.template().inTransaction(new TransactionCallback&lt;Long&gt;() {
 *         public Long doInTransaction(OrmTemplate orm) {
 *             orm.insert(order);
 *             orm.batchInsert(items);
 *             return order.getId();
 *         }
 *     });
 * </pre>
 *
 * @param <T> 回调返回值类型
 * @author dfr
 * @since 2.1
 */
public interface TransactionCallback<T> {

    /**
     * 在事务内执行的业务逻辑。
     *
     * @param orm 事务绑定的 ORM 引擎（与外部共享实例等价，连接绑定在当前线程）
     * @return 业务结果
     * @throws Exception 业务异常（触发回滚）
     */
    T doInTransaction(OrmTemplate orm) throws Exception;
}
