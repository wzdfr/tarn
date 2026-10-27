package examples.standalone;

import com.dfrdev.tarn.standalone.StandaloneLauncher;

/**
 * 独立运行入口：内嵌 Jetty 启动 Tarn，无需 Tomcat。
 * <p>
 * 启动前请确认 {@code tarn-demo/src/main/resources/application.yml} 中：
 * <pre>
 * tarn:
 *   server:
 *     port: 8080
 *     context-path: /
 *     webapp-dir: standalone-web
 *     config-class: examples.standalone.StandaloneAppConfig
 * </pre>
 * 运行方式：
 * <ol>
 *     <li>IDE：直接运行本类 main（工作目录设为 {@code tarn-demo}）</li>
 *     <li>命令行：{@code mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.standalone.StandaloneApp}</li>
 *     <li>打包：{@code mvn package} 后
 *     {@code java -cp "tarn-demo/target/tarn-demo.jar;tarn-demo/target/lib/*"
 *            examples.standalone.StandaloneApp}
 *     （Windows 用分号，Linux/Mac 用冒号）</li>
 * </ol>
 *
 * @author dfr
 * @since 2.0
 */
public class StandaloneApp {

    public static void main(String[] args) {
        // 读取 application.yml 的 tarn.server.* 配置后启动
        StandaloneLauncher.run();
    }
}
