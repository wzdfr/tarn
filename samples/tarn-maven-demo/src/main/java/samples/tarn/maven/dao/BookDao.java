package samples.tarn.maven.dao;

import cc.ruixing.framework.tarn.core.DaoException;
import cc.ruixing.framework.tarn.core.Q;
import cc.ruixing.framework.tarn.core.sql.ParameterizedSql;
import cc.ruixing.framework.tarn.core.sql.SqlBuilder;
import cc.ruixing.framework.tarn.util.StringUtil;
import samples.tarn.maven.entity.Book;

import java.util.List;

/**
 * DAO 层示例：ORM 写操作 + 参数化 SQL 查询。
 * <p>
 * 分层：Controller → Service → Dao → {@link Q}（门面）→ DB（实体↔SQL）→ DAOAbstract（JDBC）。
 *
 * @author dfr
 * @since 2.1
 */
public class BookDao {

    /** Q 无状态，可复用；连接由线程上下文管理 */
    private final Q q = new Q();

    /**
     * 新增并返回数据库生成的主键（主键同时回填到实体）。
     */
    public Long save(Book book) {
        Object key = q.insertReturnKey(book);
        return key instanceof Number ? ((Number) key).longValue() : book.getId();
    }

    /**
     * 按主键更新，返回受影响行数（null 字段默认跳过）。
     */
    public int update(Book book) {
        return q.updateRows(book);
    }

    /**
     * 列表查询：keyword 为空查全部。
     */
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

    /**
     * 主键查询。
     */
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

    /**
     * 主键删除，返回受影响行数。
     */
    public int deleteById(Long id) throws DaoException {
        ParameterizedSql ps = SqlBuilder
                .create("DELETE FROM demo_book WHERE ID = #{id}")
                .bind("id", id)
                .build();
        return q.exec(ps);
    }
}
