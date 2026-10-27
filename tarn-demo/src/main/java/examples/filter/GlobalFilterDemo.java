package examples.filter;

import cc.ruixing.framework.tarn.standalone.StandaloneLauncher;

/**
 * 全局拦截示例入口（内嵌 Jetty，非 Tomcat 环境）。
 * <p>
 * 运行：{@code mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.filter.GlobalFilterDemo}
 * （需先在父工程执行过 {@code mvn -DskipTests install}，或加 {@code -am}）
 * <p>
 * 通过 {@code StandaloneLauncher.run(port, contextPath, webappDir, configClass)} 显式指定
 * {@link GlobalFilterConfig}，无需改 application.yml。
 * <p>
 * 验证：
 * <pre>
 * curl -i http://127.0.0.1:8081/standalone/book/ping              # 白名单，200
 * curl -i http://127.0.0.1:8081/standalone/book/list              # 无 token，401
 * curl -i -H "X-Token: tarn-demo-token" \
 *      http://127.0.0.1:8081/standalone/book/list                  # 200，响应头含 X-Trace-Id
 * curl -i -X OPTIONS http://127.0.0.1:8081/standalone/book/list    # 204，含 CORS 头
 * </pre>
 * 其他环境的等价配置：
 * <ul>
 *     <li><b>Tomcat</b>：web.xml 中 {@code TarnDispatcherFilter} 的
 *     {@code <init-param>configClass</init-param>} 指向
 *     {@code examples.filter.GlobalFilterConfig}</li>
 *     <li><b>Spring Boot</b>：starter 已在 {@code tarn-spring-boot-starter} 中注册过滤器，
 *     只需在 application.yml 配置
 *     {@code tarn.spring.config-class: examples.filter.GlobalFilterConfig}</li>
 * </ul>
 *
 * @author dfr
 * @since 2.1
 */
public class GlobalFilterDemo {

    public static void main(String[] args) {
        // 端口用 8081，避免与 examples.standalone.StandaloneApp（8080）冲突
        StandaloneLauncher.run(8081, "/", "standalone-web", GlobalFilterConfig.class.getName());
    }
}
