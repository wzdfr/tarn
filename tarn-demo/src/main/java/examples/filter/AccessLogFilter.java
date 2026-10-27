package examples.filter;

import com.dfrdev.tarn.core.FilterInterface;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 访问日志拦截器示例：记录请求行、来源 IP，并注入链路 ID。
 * <p>
 * 注意：{@link FilterInterface} 只提供<b>前置</b>钩子（框架在路由分发前调用），
 * 因此这里只能记录「请求进入」的信息，无法统计整体耗时。
 * 若需要统计<b>包含 Controller 执行</b>在内的耗时或包装响应体，
 * 请在容器层注册标准 Servlet 过滤器（{@code javax.servlet.Filter}），
 * 并让它排在 {@code TarnDispatcherFilter} <b>之前</b>执行：
 * <ul>
 *     <li>Tomcat：web.xml 中把该 filter-mapping 写在 TarnDispatcherFilter 之前</li>
 *     <li>Spring Boot：{@code FilterRegistrationBean.setOrder()} 小于 {@code tarn.spring.order}（默认 10）</li>
 * </ul>
 *
 * @author dfr
 * @since 2.1
 */
public class AccessLogFilter implements FilterInterface {

    /** 请求进入时间戳，供后续拦截器/控制器计算耗时 */
    public static final String ATTR_START_TIME = "tarn.request.start";
    /** 链路 ID，供日志与响应头关联 */
    public static final String ATTR_TRACE_ID = "tarn.request.traceId";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String traceId = req.getHeader("X-Trace-Id");
        if (StringUtil.isEmpty(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        req.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        req.setAttribute(ATTR_TRACE_ID, traceId);
        resp.setHeader("X-Trace-Id", traceId);

        LogUtil.info("[AccessLog] " + req.getMethod() + " " + req.getRequestURI()
                + " ip=" + req.getRemoteAddr() + " trace=" + traceId);
        return true;
    }

    @Override
    public void destroy() {
    }
}
