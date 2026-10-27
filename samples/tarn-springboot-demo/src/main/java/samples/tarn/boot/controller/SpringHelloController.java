package samples.tarn.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 原生 Spring MVC 控制器：与 Tarn 控制器共存于同一个 Spring Boot 应用。
 * <p>
 * Tarn 过滤器只接管自己的路由；未命中 Tarn 路由的请求会 {@code filterChain.doFilter}
 * 继续交给 Spring 的 {@code DispatcherServlet}。
 *
 * @author dfr
 * @since 2.1
 */
@RestController
@RequestMapping("/spring")
public class SpringHelloController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("framework", "Spring MVC");
        data.put("msg", "spring-dispatcher-servlet");
        return data;
    }
}
