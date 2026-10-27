package com.dfrdev.tarn.standalone;

import com.dfrdev.tarn.config.YamlConfigKit;
import com.dfrdev.tarn.core.TarnDispatcherFilter;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.DispatcherType;
import java.io.File;
import java.util.EnumSet;

/**
 * 独立运行启动器（Tarn 2.0+）：内嵌 Jetty，脱离 Tomcat 也可运行。
 * <p>
 * 用法：
 * <pre>
 *     public static void main(String[] args) {
 *         StandaloneLauncher.run();                 // 读 application.yml 的 tarn.server.*
 *         // 或显式指定：
 *         StandaloneLauncher.run(8080, "/", "web"); // port, contextPath, webappDir
 *         // 或同时指定 TarnConfig（覆盖 yml 的 config-class）：
 *         StandaloneLauncher.run(8081, "/", "web", "examples.filter.GlobalFilterConfig");
 *     }
 * </pre>
 * YAML 配置：
 * <pre>
 * tarn:
 *   server:
 *     port: 8080
 *     context-path: /
 *     webapp-dir: web            # 含 WEB-INF 的目录
 *     config-class:              # TarnConfig 实现类（等同 web.xml 的 configClass 初始化参数）
 * </pre>
 * 依赖：jetty-server / jetty-webapp 9.4.x（pom.xml 中为 optional，独立运行时才需引入）。
 * 部署到 Tomcat 的老项目不受影响——本类仅在显式调用时生效。
 *
 * @author dfr
 * @since 2.0
 */
public final class StandaloneLauncher {

    public static final String CONF_PREFIX = "tarn.server";

    private StandaloneLauncher() {
    }

    /**
     * 以 application.yml 的 tarn.server.* 配置启动
     */
    public static void run() {
        int port = YamlConfigKit.getInt(CONF_PREFIX + ".port", 8080);
        String contextPath = YamlConfigKit.get(CONF_PREFIX + ".context-path", "/");
        String webappDir = YamlConfigKit.get(CONF_PREFIX + ".webapp-dir", "web");
        run(port, contextPath, webappDir);
    }

    /**
     * 显式参数启动
     *
     * @param port        监听端口
     * @param contextPath 上下文路径（/ 开头）
     * @param webappDir   webapp 目录（包含 WEB-INF）
     */
    public static void run(int port, String contextPath, String webappDir) {
        run(port, contextPath, webappDir, YamlConfigKit.get(CONF_PREFIX + ".config-class", ""));
    }

    /**
     * 显式参数启动，并显式指定 TarnConfig 实现类（覆盖 application.yml 的
     * {@code tarn.server.config-class}）。
     * <p>
     * 用于一个工程内需要多套配置的场景（示例：不同入口装配不同的全局拦截器）。
     *
     * @param port        监听端口
     * @param contextPath 上下文路径（/ 开头）
     * @param webappDir   webapp 目录（包含 WEB-INF）
     * @param configClass TarnConfig 实现类全名，为空时读 application.yml
     */
    public static void run(int port, String contextPath, String webappDir, String configClass) {
        File dir = new File(webappDir);
        if (!dir.isDirectory()) {
            // 兼容从父工程根目录启动：webappDir 相对子模块（如 tarn-demo/standalone-web）
            File alt = new File("tarn-demo", webappDir);
            if (alt.isDirectory()) {
                dir = alt;
            }
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException("[StandaloneLauncher] webapp 目录不存在: " + dir.getAbsolutePath());
        }
        if (StringUtil.isEmpty(contextPath) || !contextPath.startsWith("/")) {
            contextPath = "/";
        }

        Server server = new Server(port);
        WebAppContext context = new WebAppContext();
        context.setContextPath(contextPath);
        context.setWar(dir.getAbsolutePath());
        // 允许从 WEB-INF/classes 与 WEB-INF/lib 加载（与 Tomcat 行为一致）
        context.setParentLoaderPriority(false);

        // 注册框架主过滤器（等同 web.xml 中的 TarnDispatcherFilter 配置）。
        // webapp 自身 web.xml 已声明该过滤器时跳过代码注册，避免重复执行。
        if (!webXmlDeclaresTarnFilter(dir)) {
            org.eclipse.jetty.servlet.FilterHolder filterHolder = context.addFilter(
                    TarnDispatcherFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            if (StringUtil.isNotEmpty(configClass)) {
                filterHolder.setInitParameter("configClass", configClass);
            }
        } else {
            LogUtil.info("[StandaloneLauncher] web.xml 已声明 TarnDispatcherFilter，跳过代码注册");
        }

        server.setHandler(context);
        try {
            server.start();
            LogUtil.info("[StandaloneLauncher] Tarn 独立运行已启动: http://127.0.0.1:" + port + contextPath
                    + " (webapp=" + dir.getAbsolutePath() + ")");
            server.join();
        } catch (Exception e) {
            throw new RuntimeException("[StandaloneLauncher] 启动失败: " + e.getMessage(), e);
        }
    }

    /**
     * webapp 的 WEB-INF/web.xml 是否已声明 TarnDispatcherFilter。
     * 先剥离 XML 注释，避免注释里出现类名造成误判。
     */
    private static boolean webXmlDeclaresTarnFilter(File webappDir) {
        File webXml = new File(webappDir, "WEB-INF/web.xml");
        if (!webXml.isFile()) {
            return false;
        }
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(webXml);
            byte[] buf = new byte[8192];
            int len;
            StringBuilder sb = new StringBuilder();
            while ((len = in.read(buf)) != -1) {
                sb.append(new String(buf, 0, len, "UTF-8"));
            }
            String xml = sb.toString().replaceAll("(?s)<!--.*?-->", "");
            return xml.indexOf("TarnDispatcherFilter") != -1;
        } catch (Exception ignore) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
