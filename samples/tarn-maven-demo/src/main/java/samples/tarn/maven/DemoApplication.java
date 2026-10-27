package samples.tarn.maven;

import com.dfrdev.tarn.standalone.StandaloneLauncher;

/**
 * 示例启动类：内嵌 Jetty 独立运行（无需 Tomcat）。
 * <p>
 * 运行方式：
 * <pre>
 * cd samples/tarn-maven-demo
 * mvn -q compile exec:java
 * </pre>
 * 或打包后：
 * <pre>
 * mvn clean package
 * java -cp "target/classes:target/lib/*" samples.tarn.maven.DemoApplication
 * </pre>
 * 验证：curl http://127.0.0.1:9090/book/ping
 *
 * @author dfr
 * @since 2.1
 */
public class DemoApplication {

    public static void main(String[] args) {
        // port, contextPath, webappDir（webappDir 相对当前工作目录）
        StandaloneLauncher.run(9090, "/", "web");
    }
}
