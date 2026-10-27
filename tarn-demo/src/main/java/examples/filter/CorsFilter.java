package examples.filter;

import cc.ruixing.framework.tarn.core.FilterInterface;
import cc.ruixing.framework.tarn.util.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 跨域拦截器示例：统一写 CORS 响应头，并短路 OPTIONS 预检请求。
 * <p>
 * 说明：{@link FilterInterface} 是 Tarn 的<b>前置</b>扩展点，在路由分发之前执行，
 * 返回 {@code false} 表示中断本次请求（不再进入 Controller）。
 *
 * @author dfr
 * @since 2.1
 */
public class CorsFilter implements FilterInterface {

    /** 允许的来源；生产环境建议改为具体域名白名单，不要用 * */
    private static final String ALLOW_ORIGIN = "*";
    private static final String ALLOW_METHODS = "GET,POST,PUT,DELETE,OPTIONS";
    private static final String ALLOW_HEADERS = "Content-Type,X-Token,X-Requested-With";
    private static final String MAX_AGE = "3600";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        resp.setHeader("Access-Control-Allow-Methods", ALLOW_METHODS);
        resp.setHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
        resp.setHeader("Access-Control-Max-Age", MAX_AGE);

        // 预检请求到此结束，不进入路由
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {
    }
}
