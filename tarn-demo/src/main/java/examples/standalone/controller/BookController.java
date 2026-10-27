package examples.standalone.controller;

import cc.ruixing.framework.tarn.aop.annotation.EAutowired;
import cc.ruixing.framework.tarn.aop.annotation.EController;
import cc.ruixing.framework.tarn.aop.annotation.EPathVariable;
import cc.ruixing.framework.tarn.aop.annotation.ERequestBody;
import cc.ruixing.framework.tarn.aop.annotation.ERequestMapping;
import cc.ruixing.framework.tarn.aop.annotation.ERequestParam;
import cc.ruixing.framework.tarn.aop.annotation.Json;
import cc.ruixing.framework.tarn.aop.annotation.RequestMethod;
import cc.ruixing.framework.tarn.core.Controller;
import cc.ruixing.framework.tarn.core.DaoException;
import cc.ruixing.framework.tarn.json.JsonResult;
import examples.standalone.entity.Book;
import examples.standalone.service.BookService;

import java.util.HashMap;
import java.util.Map;

/**
 * 独立运行（{@code StandaloneLauncher}）完整示例控制器。
 * <p>
 * 路由说明（Tarn 2.0 Router）：
 * <ul>
 *     <li>控制器根路径 {@code /standalone/book} 为静态路径，整体注册进路由表</li>
 *     <li>{@code /list}、{@code /save}、{@code /ping} 为静态路由，O(1) 命中</li>
 *     <li>{@code /detail/{id}}、{@code /delete/{id}} 含路径变量，进入<b>动态路由表</b>，
 *     由 {@code @ERequestMapping(regex=)} 限定只匹配数字，命中后自动注入 {@code @EPathVariable}</li>
 * </ul>
 * 启动：运行 {@code examples.standalone.StandaloneApp#main}，或执行
 * <pre>mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.standalone.StandaloneApp</pre>
 *
 * @author dfr
 * @since 2.0
 */
@EController("/standalone/book")
public class BookController extends Controller {

    @EAutowired
    private BookService bookService;

    public void setBookService(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /standalone/book/list?keyword=Java
     */
    @Json
    @ERequestMapping(value = "/list", method = RequestMethod.GET)
    public JsonResult list(@ERequestParam("keyword") String keyword) {
        try {
            return JsonResult.ok(bookService.list(keyword));
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * GET /standalone/book/detail/1
     * <p>regex = "[0-9]+"：非数字路径不会命中该路由，直接 404，避免脏参数进入业务。
     */
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

    /**
     * POST /standalone/book/save
     * Content-Type: application/json
     * {"title":"Tarn 实战","author":"dfr","stock":10}
     */
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

    /**
     * POST /standalone/book/update
     * Content-Type: application/json
     * {"id":1,"title":"Tarn 实战（第2版）","stock":20}
     */
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

    /**
     * POST /standalone/book/delete/1
     */
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

    /**
     * GET /standalone/book/ping —— 验证服务是否启动（不依赖数据库）
     */
    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() {
        return JsonResult.ok("pong");
    }
}
