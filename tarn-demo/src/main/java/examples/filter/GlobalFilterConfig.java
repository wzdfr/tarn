package examples.filter;

import com.dfrdev.tarn.core.web.Filters;
import com.dfrdev.tarn.core.boot.TarnConfig;
import com.dfrdev.tarn.core.boot.Plugins;
import com.dfrdev.tarn.util.LogUtil;

/**
 * 全局拦截配置示例：在 {@link TarnConfig#configFilter(Filters)} 中装配 Tarn 扩展拦截器。
 * <p>
 * 三种接入方式的区别（三选一，可混用，执行顺序为「先装配先执行」）：
 * <table border="1">
 *     <tr><th>方式</th><th>配置位置</th><th>适用场景</th></tr>
 *     <tr>
 *         <td>编程式（本类）</td>
 *         <td>{@code configFilter(Filters)}，由 {@code configClass} 指定的 TarnConfig 子类实现</td>
 *         <td>推荐：拦截器需要读配置、需要多实例/条件装配</td>
 *     </tr>
 *     <tr>
 *         <td>声明式</td>
 *         <td>classpath 下 {@code expand-filter.xml} 的 {@code <filters><expand-impl class="..."/>}</td>
 *         <td>固定拦截器、无需条件判断的老项目平滑迁移</td>
 *     </tr>
 *     <tr>
 *         <td>注解式</td>
 *         <td>Controller / Controller 方法上的 {@code @Before(Interceptor[])}</td>
 *         <td>只拦截某个控制器或某个方法</td>
 *     </tr>
 * </table>
 * 装配顺序即执行顺序：CORS（预检直接短路）→ 鉴权（失败短路）→ 访问日志（记录 + 注入链路 ID）。
 *
 * @author dfr
 * @since 2.1
 */
public class GlobalFilterConfig extends TarnConfig {

    @Override
    public void configFilter(Filters filters) {
        filters.add(new CorsFilter())
                .add(new AuthFilter())
                .add(new AccessLogFilter());
        LogUtil.info("[GlobalFilter] 已注册扩展拦截器：CORS → Auth → AccessLog");
    }

    @Override
    public void configPlugin(Plugins me) {
        // 本示例不额外装配插件
    }
}
