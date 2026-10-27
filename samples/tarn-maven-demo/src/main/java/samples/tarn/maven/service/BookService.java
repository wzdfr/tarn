package samples.tarn.maven.service;

import cc.ruixing.framework.tarn.core.DaoException;
import samples.tarn.maven.entity.Book;

import java.util.List;

/**
 * 服务层接口：实现由 {@code @EAutowired} 按字段名注入。
 *
 * @author dfr
 * @since 2.1
 */
public interface BookService {

    List<Book> list(String keyword) throws DaoException;

    Book findById(Long id) throws DaoException;

    Long create(Book book) throws DaoException;

    int update(Book book) throws DaoException;

    boolean delete(Long id) throws DaoException;
}
