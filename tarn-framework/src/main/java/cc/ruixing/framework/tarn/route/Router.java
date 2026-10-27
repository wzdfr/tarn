package cc.ruixing.framework.tarn.route;

import cc.ruixing.framework.tarn.aop.ControllerInfo;
import cc.ruixing.framework.tarn.aop.ControllerMethodInfo;
import cc.ruixing.framework.tarn.aop.Inject;
import cc.ruixing.framework.tarn.aop.annotation.ERequestMapping;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 路由表（Tarn 2.0+，MVC 路由概念）。
 * <p>
 * 解决老调度器每请求遍历全部 Controller + 逐个正则匹配（O(n)）的性能问题：
 * <ul>
 *     <li><b>静态路由表</b>：精确路径 → 路由条目（HashMap，O(1) 直达）</li>
 *     <li><b>动态路由表</b>：{@code /delete/{id}}、{@code /a/${mapping}} 等映射在启动时编译为正则，
 *     静态未命中时按顺序匹配，命中后由 {@link RouteDispatcher} 完成路径变量注入</li>
 *     <li>两者都未命中时回退老的拦截器链，老项目行为不变</li>
 * </ul>
 *
 * <p>动态路由写法（{@code @EController} + {@code @ERequestMapping}）：
 * <pre>
 *     @EController("/api/demo")
 *     public class DemoController extends Controller {
 *
 *         // /api/demo/delete/1   正则限定纯数字
 *         @ERequestMapping(value = "/delete/{id}", method = RequestMethod.POST,
 *                          isPathVariable = true, regex = "[0-9]+")
 *         public JsonResult delete(@EPathVariable("id") Long id) { ... }
 *     }
 * </pre>
 *
 * @author dfr
 * @see RouteEntry
 * @since 2.0
 */
public final class Router {

    private static volatile Router INSTANCE;

    /** 静态路由表：精确路径 → 路由条目 */
    private final Map<String, RouteEntry> routes = new HashMap<String, RouteEntry>();

    /** 动态路由表：按匹配优先级排序（字面量越多的越靠前） */
    private final List<RouteEntry> dynamicRoutes = new ArrayList<RouteEntry>();

    private Router() {
    }

    /**
     * 获取路由表（懒加载构建，线程安全）
     */
    public static Router get() {
        if (INSTANCE == null) {
            synchronized (Router.class) {
                if (INSTANCE == null) {
                    Router router = new Router();
                    router.build();
                    INSTANCE = router;
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 重新构建路由表（热加载场景）
     */
    public static synchronized void rebuild() {
        Router router = new Router();
        router.build();
        INSTANCE = router;
    }

    /**
     * 查找路由。命中返回 RouteEntry，未命中返回 null（调用方回退老拦截器链）
     * <p>顺序：静态精确匹配 → 动态正则匹配（按优先级）。
     *
     * @param vpath      请求路径（不含 contextPath，以 / 开头）
     * @param httpMethod HTTP 方法（GET/POST...），用于 @ERequestMapping(method=) 约束
     */
    public RouteEntry lookup(String vpath, String httpMethod) {
        if (vpath == null) {
            return null;
        }
        String normalized = normalize(vpath);
        RouteEntry entry = routes.get(normalized);
        if (entry != null && entry.matchesHttpMethod(httpMethod)) {
            return entry;
        }
        for (RouteEntry dynamic : dynamicRoutes) {
            if (!dynamic.matchesHttpMethod(httpMethod)) {
                continue;
            }
            if (dynamic.getPattern() != null && dynamic.getPattern().matcher(normalized).matches()) {
                return dynamic;
            }
        }
        return null;
    }

    public int size() {
        return routes.size() + dynamicRoutes.size();
    }

    /**
     * 静态路由数量（诊断用）
     */
    public int staticSize() {
        return routes.size();
    }

    /**
     * 动态路由数量（诊断用）
     */
    public int dynamicSize() {
        return dynamicRoutes.size();
    }

    /**
     * 从控制器清单构建路由表
     */
    private void build() {
        List<ControllerInfo> controllers = Inject.getControllers();
        if (controllers == null) {
            return;
        }
        for (ControllerInfo ci : controllers) {
            String basePath = ci.getKey();
            if (isDynamic(basePath)) {
                continue; // 控制器根路径本身动态（${mapping} / ${method}）：保留老拦截器链处理
            }
            // 控制器根路径 → execute()
            register(basePath, new RouteEntry(ci, null, null, null, null, normalize(basePath)));

            List<ControllerMethodInfo> methods = ci.getMethodInfoList();
            if (methods == null) {
                continue;
            }
            for (ControllerMethodInfo mi : methods) {
                ERequestMapping mapping = mi.getRequestMapping();
                if (mapping == null) {
                    continue;
                }
                String mappingValue = mapping.value();
                if (mappingValue == null || mappingValue.trim().isEmpty()) {
                    continue;
                }
                String fullPath = joinPath(basePath, mappingValue);
                if (isDynamic(fullPath)) {
                    Pattern pattern = compile(fullPath, ci.getRegex(), mapping.regex());
                    if (pattern == null) {
                        LogUtil.error("[Router] 动态路由编译失败，回退老链路: " + fullPath);
                        continue;
                    }
                    dynamicRoutes.add(new RouteEntry(ci, mi.getName(), mapping.method(),
                            mappingValue, mi, fullPath, pattern));
                } else {
                    register(fullPath, new RouteEntry(ci, mi.getName(), mapping.method(),
                            mappingValue, mi, fullPath));
                }
            }
        }
        sortDynamicRoutes();
        LogUtil.info("[Router] 路由表构建完成, 静态 " + routes.size()
                + " 条, 动态 " + dynamicRoutes.size() + " 条");
        for (RouteEntry entry : dynamicRoutes) {
            LogUtil.info("[Router] 动态路由 " + entry.getPathTemplate()
                    + " -> " + entry.getControllerClass() + "#" + entry.getMethodName());
        }
    }

    /**
     * 动态路由优先级：字面量越长越靠前，避免 /user/{id} 抢占 /user/list/{type}
     */
    private void sortDynamicRoutes() {
        Collections.sort(dynamicRoutes, new Comparator<RouteEntry>() {
            @Override
            public int compare(RouteEntry o1, RouteEntry o2) {
                return literalLength(o2.getPathTemplate()) - literalLength(o1.getPathTemplate());
            }
        });
    }

    private static int literalLength(String template) {
        if (template == null) {
            return 0;
        }
        int len = 0;
        int i = 0;
        while (i < template.length()) {
            if (template.startsWith("${", i)) {
                int end = template.indexOf('}', i);
                if (end == -1) {
                    break;
                }
                i = end + 1;
            } else if (template.charAt(i) == '{') {
                int end = template.indexOf('}', i);
                if (end == -1) {
                    break;
                }
                i = end + 1;
            } else {
                len++;
                i++;
            }
        }
        return len;
    }

    /**
     * 编译动态路由模板为锚定正则。
     * <p>
     * 占位符替换规则：
     * <ul>
     *     <li>{@code ${mapping}} → 控制器 {@code @EController(regex=)}，缺省 {@code [^/]+}</li>
     *     <li>{@code {id}} → 方法 {@code @ERequestMapping(regex=)}，缺省 {@code [^/]+}</li>
     *     <li>{@code {*}} → {@code .+}（可跨层级，兼容老写法）</li>
     * </ul>
     */
    static Pattern compile(String template, String controllerRegex, String methodRegex) {
        StringBuilder sb = new StringBuilder("^");
        int i = 0;
        while (i < template.length()) {
            if (template.startsWith("${", i)) {
                int end = template.indexOf('}', i);
                if (end == -1) {
                    return null;
                }
                sb.append('(')
                        .append(StringUtil.isEmpty(controllerRegex) ? "[^/]+" : controllerRegex)
                        .append(')');
                i = end + 1;
            } else if (template.charAt(i) == '{') {
                int end = template.indexOf('}', i);
                if (end == -1) {
                    return null;
                }
                String name = template.substring(i + 1, end);
                String regex;
                if ("*".equals(name)) {
                    regex = ".+";
                } else if (StringUtil.isEmpty(methodRegex)) {
                    regex = "[^/]+";
                } else {
                    regex = methodRegex;
                }
                sb.append('(').append(regex).append(')');
                i = end + 1;
            } else {
                int start = i;
                while (i < template.length() && template.charAt(i) != '{' && !template.startsWith("${", i)) {
                    i++;
                }
                sb.append(Pattern.quote(template.substring(start, i)));
            }
        }
        sb.append('$');
        try {
            return Pattern.compile(sb.toString());
        } catch (Exception e) {
            LogUtil.error("[Router] 正则编译异常: " + template + " -> " + sb, e);
            return null;
        }
    }

    private void register(String path, RouteEntry entry) {
        String normalized = normalize(path);
        RouteEntry old = routes.put(normalized, entry);
        if (old != null) {
            LogUtil.error("[Router] 路由冲突: " + normalized + " -> "
                    + old + " 被覆盖为 " + entry);
        }
    }

    /**
     * 是否为动态路径（含 ${} 模板或 {} 路径变量）
     */
    static boolean isDynamic(String path) {
        return path != null && (path.contains("${") || path.contains("{"));
    }

    /**
     * 路径归一化：保证 / 开头、去尾部 /、压缩连续 //
     */
    static String normalize(String path) {
        if (path == null) {
            return "/";
        }
        String p = path.trim().replaceAll("/{2,}", "/");
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    static String joinPath(String base, String child) {
        String b = normalize(base);
        String c = child.trim();
        if (c.startsWith("/")) {
            c = c.substring(1);
        }
        return b.equals("/") ? "/" + c : b + "/" + c;
    }
}
