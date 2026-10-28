package examples.standalone.service.impl;

import com.dfrdev.tarn.aop.annotation.EService;
import com.dfrdev.tarn.core.db.DaoException;
import examples.standalone.dao.BookDao;
import examples.standalone.entity.Book;
import examples.standalone.service.BookService;

import java.util.List;

/**
 * 服务实现：{@code @EService("bookService")} 的名字要与控制器字段名一致
 * （框架按字段名从 context-mapping 扫描的 services 包中取实现）。
 *
 * @author dfr
 * @since 2.0
 */
@EService("bookService")
public class BookServiceImpl implements BookService {

    private final BookDao bookDao = new BookDao();

    @Override
    public List<Book> list(String keyword) throws DaoException {
        return bookDao.list(keyword);
    }

    @Override
    public Book findById(Long id) throws DaoException {
        return bookDao.findById(id);
    }

    @Override
    public Long create(Book book) throws DaoException {
        return bookDao.save(book);
    }

    @Override
    public int update(Book book) throws DaoException {
        return bookDao.update(book);
    }

    @Override
    public boolean delete(Long id) throws DaoException {
        return bookDao.deleteById(id) > 0;
    }
}
