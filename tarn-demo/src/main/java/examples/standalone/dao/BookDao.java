package examples.standalone.dao;

import cc.ruixing.framework.tarn.core.DaoException;
import cc.ruixing.framework.tarn.core.Q;
import cc.ruixing.framework.tarn.core.sql.ParameterizedSql;
import cc.ruixing.framework.tarn.core.sql.SqlBuilder;
import cc.ruixing.framework.tarn.util.StringUtil;
import examples.standalone.entity.Book;

import java.util.List;

/**
 * 数据访问层示例：基于 {@link Q} 的 ORM + 参数化 SQL。
 * <p>
 * 所有查询都走 {@code SqlBuilder} 参数绑定，条件值不拼接字符串，避免 SQL 注入。
 *
 * @author dfr
 * @since 2.0
 */
public class BookDao {

    /** Q 无状态，可安全复用（内部连接由线程上下文管理） */
    private final Q q = new Q();

    /**
     * 新增并返回数据库生成的主键（Tarn 2.1：{@code insertReturnKey} 会同时把主键回填到实体）。
     *
     * @return 生成的主键；驱动不支持时返回实体原有主键
     */
    public Long save(Book book) {
        Object key = q.insertReturnKey(book);
        return key instanceof Number ? ((Number) key).longValue() : book.getId();
    }

    /**
     * 按主键更新（Tarn 2.1：默认跳过 null 字段，{@code @EUpdateNull(true)} 字段例外）。
     *
     * @return 受影响行数，0 表示主键不存在或字段未变化
     */
    public int update(Book book) {
        return q.updateRows(book);
    }

    /** 列表查询：keyword 为空时查全部 */
    @SuppressWarnings("unchecked")
    public List<Book> list(String keyword) throws DaoException {
        SqlBuilder builder = SqlBuilder
                .select("ID, TITLE, AUTHOR, STOCK")
                .from("demo_book");
        if (StringUtil.isNotEmpty(keyword)) {
            builder.where("TITLE LIKE #{keyword}").bind("keyword", "%" + keyword + "%");
        }
        builder.orderBy("ID", false);
        return (List<Book>) (List<?>) q.query(builder.build(), Book.class);
    }

    /** 主键查询 */
    public Book findById(Long id) throws DaoException {
        ParameterizedSql ps = SqlBuilder
                .select("ID, TITLE, AUTHOR, STOCK")
                .from("demo_book")
                .where("ID = #{id}")
                .bind("id", id)
                .build();
        List<?> list = q.query(ps, Book.class);
        return list.isEmpty() ? null : (Book) list.get(0);
    }

    /** 主键删除，返回受影响行数 */
    public int deleteById(Long id) throws DaoException {
        ParameterizedSql ps = SqlBuilder
                .create("DELETE FROM demo_book WHERE ID = #{id}")
                .bind("id", id)
                .build();
        return q.exec(ps);
    }
}
