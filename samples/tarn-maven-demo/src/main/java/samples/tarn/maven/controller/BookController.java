package samples.tarn.maven.controller;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.EPathVariable;
import com.dfrdev.tarn.aop.annotation.ERequestBody;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.core.DaoException;
import com.dfrdev.tarn.json.JsonResult;
import samples.tarn.maven.entity.Book;
import samples.tarn.maven.service.BookService;

import java.util.HashMap;
import java.util.Map;

/**
 * 控制器示例：演示静态路由、动态路由（路径变量 + 正则）、JSON 入参出参。
 * <p>
 * 路由表：
 * <ul>
 *     <li>GET  /book/ping         —— 无需数据库，验证服务已启动</li>
 *     <li>GET  /book/list         —— 列表查询</li>
 *     <li>GET  /book/detail/{id}  —— 动态路由，regex 限定数字</li>
 *     <li>POST /book/save         —— ORM 新增（返回主键）</li>
 *     <li>POST /book/update       —— ORM 按主键更新（返回行数）</li>
 *     <li>POST /book/delete/{id}  —— 动态路由删除</li>
 * </ul>
 *
 * @author dfr
 * @since 2.1
 */
@EController("/book")
public class BookController extends Controller {

    @EAutowired
    private BookService bookService;

    public void setBookService(BookService bookService) {
        this.bookService = bookService;
    }

    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() {
        return JsonResult.ok("pong");
    }

    @Json
    @ERequestMapping(value = "/list", method = RequestMethod.GET)
    public JsonResult list(@ERequestParam("keyword") String keyword) {
        try {
            return JsonResult.ok(bookService.list(keyword));
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/detail/{id}", method = RequestMethod.GET,
            isPathVariable = true, regex = "[0-9]+")
    public JsonResult detail(@EPathVariable("id") Long id) {
        try {
            Book book = bookService.findById(id);
            return book == null ? JsonResult.fail(404, "图书不存在") : JsonResult.ok(book);
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/save", method = RequestMethod.POST)
    public JsonResult save(@ERequestBody Book book) {
        if (book == null || book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return JsonResult.fail(400, "title 不能为空");
        }
        try {
            Long id = bookService.create(book);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("id", id);
            return JsonResult.ok("保存成功", data);
        } catch (DaoException e) {
            return JsonResult.fail("保存失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/update", method = RequestMethod.POST)
    public JsonResult update(@ERequestBody Book book) {
        if (book == null || book.getId() == null) {
            return JsonResult.fail(400, "id 不能为空");
        }
        try {
            int rows = bookService.update(book);
            return rows > 0 ? JsonResult.ok("更新成功", rows) : JsonResult.fail(404, "图书不存在");
        } catch (DaoException e) {
            return JsonResult.fail("更新失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/delete/{id}", method = RequestMethod.POST,
            isPathVariable = true, regex = "[0-9]+")
    public JsonResult delete(@EPathVariable("id") Long id) {
        try {
            return bookService.delete(id) ? JsonResult.ok("删除成功", id)
                    : JsonResult.fail(404, "图书不存在");
        } catch (DaoException e) {
            return JsonResult.fail("删除失败: " + e.getMessage());
        }
    }
}
