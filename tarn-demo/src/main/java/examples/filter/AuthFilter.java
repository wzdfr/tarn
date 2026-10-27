package examples.filter;

import com.dfrdev.tarn.core.FilterInterface;
import com.dfrdev.tarn.json.JsonResult;
import com.dfrdev.tarn.util.StringUtil;
import com.alibaba.fastjson.JSON;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录鉴权拦截器示例：校验请求头 {@code X-Token}，失败直接返回 401 JSON 并中断请求。
 * <p>
 * 演示要点：
 * <ul>
 *     <li>白名单放行（静态资源、健康检查）</li>
 *     <li>返回 {@code false} 后框架不会继续路由，Controller 不会被执行</li>
 *     <li>真实项目把 {@link #DEMO_TOKEN} 换成 JWT / Session / 网关透传的用户身份校验</li>
 * </ul>
 *
 * @author dfr
 * @since 2.1
 */
public class AuthFilter implements FilterInterface {

    private static final String TOKEN_HEADER = "X-Token";
    /** 演示用固定令牌；生产项目务必替换为真实鉴权 */
    private static final String DEMO_TOKEN = "tarn-demo-token";
    private static final String[] WHITE_LIST = {"/", "/index.html", "/standalone/book/ping"};

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (inWhiteList(path)) {
            return true;
        }
        String token = req.getHeader(TOKEN_HEADER);
        if (StringUtil.isNotEmpty(token) && DEMO_TOKEN.equals(token.trim())) {
            return true;
        }
        writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED, "缺少或无效的 " + TOKEN_HEADER);
        return false;
    }

    @Override
    public void destroy() {
    }

    private boolean inWhiteList(String path) {
        for (String item : WHITE_LIST) {
            if (item.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void writeJson(HttpServletResponse resp, int status, String message) {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        try {
            resp.getWriter().write(JSON.toJSONString(JsonResult.fail(status, message)));
        } catch (IOException e) {
            // 响应已提交或客户端断开，无需再处理
        }
    }
}
