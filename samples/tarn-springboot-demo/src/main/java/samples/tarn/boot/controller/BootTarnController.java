package samples.tarn.boot.controller;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.EPathVariable;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.json.JsonResult;
import samples.tarn.boot.service.BootGreetService;

import java.util.HashMap;
import java.util.Map;

/**
 * Tarn MVC 控制器（运行在 Spring Boot 容器内）。
 * <p>
 * 根路径 {@code /tarn/boot} 为静态路径，会被自动注册进 Tarn 路由表；
 * {@code /hello/{name}} 含路径变量，进入动态路由表并由 regex 限制字符集。
 *
 * @author dfr
 * @since 2.1
 */
@EController("/tarn/boot")
public class BootTarnController extends Controller {

    @EAutowired
    private BootGreetService bootGreetService;

    public void setBootGreetService(BootGreetService bootGreetService) {
        this.bootGreetService = bootGreetService;
    }

    /**
     * GET /tarn/boot/ping
     */
    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() {
        return JsonResult.ok("pong-tarn-in-boot");
    }

    /**
     * GET /tarn/boot/hello/dfr
     */
    @Json
    @ERequestMapping(value = "/hello/{name}", method = RequestMethod.GET,
            isPathVariable = true, regex = "[A-Za-z0-9_-]+")
    public JsonResult hello(@EPathVariable("name") String name) {
        return JsonResult.ok(bootGreetService.greet(name));
    }

    /**
     * GET /tarn/boot/add?x=1&y=2
     */
    @Json
    @ERequestMapping(value = "/add", method = RequestMethod.GET)
    public JsonResult add(@ERequestParam("x") int x, @ERequestParam("y") int y) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("x", x);
        data.put("y", y);
        data.put("sum", x + y);
        return JsonResult.ok(data);
    }
}
