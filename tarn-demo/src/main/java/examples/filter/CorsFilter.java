package examples.filter;

import com.dfrdev.tarn.core.web.FilterInterface;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 跨域拦截器示例：按白名单统一写 CORS 响应头，并短路 OPTIONS 预检请求。
 * <p>
 * 说明：{@link FilterInterface} 是 Tarn 的<b>前置</b>扩展点，在路由分发之前执行，
 * 返回 {@code false} 表示中断本次请求（不再进入 Controller）。
 * <p>
 * 允许的来源通过 Filter 初始化参数 {@code allowOrigins}（逗号分隔）或 JVM 系统属性
 * {@code tarn.cors.allowOrigins} 配置；未配置时不输出 CORS 头（仅同源可用）。
 * 生产环境请填写具体域名，避免使用 {@code *}。
 *
 * @author dfr
 * @since 2.1
 */
public class CorsFilter implements FilterInterface {

    private static final String ALLOW_METHODS = "GET,POST,PUT,DELETE,OPTIONS";
    private static final String ALLOW_HEADERS = "Content-Type,X-Token,X-Requested-With";
    private static final String MAX_AGE = "3600";

    private Set<String> allowOrigins = Collections.emptySet();

    @Override
    public void init(FilterConfig filterConfig) {
        String conf = filterConfig == null ? null : filterConfig.getInitParameter("allowOrigins");
        if (conf == null || conf.trim().length() == 0) {
            conf = System.getProperty("tarn.cors.allowOrigins");
        }
        if (conf != null && conf.trim().length() > 0) {
            Set<String> origins = new HashSet<String>();
            for (String origin : conf.split(",")) {
                if (origin.trim().length() > 0) {
                    origins.add(origin.trim());
                }
            }
            allowOrigins = Collections.unmodifiableSet(origins);
        }
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        if (origin != null && (allowOrigins.contains("*") || allowOrigins.contains(origin))) {
            resp.setHeader("Access-Control-Allow-Origin", allowOrigins.contains("*") ? "*" : origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Methods", ALLOW_METHODS);
            resp.setHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
            resp.setHeader("Access-Control-Max-Age", MAX_AGE);
        }

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
