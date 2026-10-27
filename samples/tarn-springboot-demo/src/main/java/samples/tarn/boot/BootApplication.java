package samples.tarn.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类（Tarn MVC 集成示例）。
 * <p>
 * 启动后：
 * <ul>
 *     <li>Tarn 路由：<a href="http://127.0.0.1:8081/tarn/boot/ping">/tarn/boot/ping</a></li>
 *     <li>Tarn 动态路由：/tarn/boot/hello/dfr</li>
 *     <li>Spring MVC 路由：/spring/hello（证明两套 MVC 可共存）</li>
 * </ul>
 * 无需 web.xml、无需手动注册 Filter —— 由 tarn-spring-boot-starter 自动装配。
 *
 * @author dfr
 * @since 2.1
 */
@SpringBootApplication
public class BootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }
}
