package examples.standalone.service;

import com.dfrdev.tarn.core.DaoException;
import examples.standalone.entity.Book;

import java.util.List;

/**
 * 服务层接口：由 {@code @EAutowired} 按字段名注入实现。
 *
 * @author dfr
 * @since 2.0
 */
public interface BookService {

    List<Book> list(String keyword) throws DaoException;

    Book findById(Long id) throws DaoException;

    /**
     * 新增图书，返回数据库生成的主键。
     */
    Long create(Book book) throws DaoException;

    /**
     * 按主键更新图书，返回受影响行数。
     */
    int update(Book book) throws DaoException;

    boolean delete(Long id) throws DaoException;
}
