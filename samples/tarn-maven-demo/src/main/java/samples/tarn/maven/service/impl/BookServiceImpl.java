package samples.tarn.maven.service.impl;

import com.dfrdev.tarn.aop.annotation.EService;
import com.dfrdev.tarn.core.DaoException;
import samples.tarn.maven.dao.BookDao;
import samples.tarn.maven.entity.Book;
import samples.tarn.maven.service.BookService;

import java.util.List;

/**
 * 服务实现：{@code @EService} 的名字必须与控制器字段名一致。
 *
 * @author dfr
 * @since 2.1
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
