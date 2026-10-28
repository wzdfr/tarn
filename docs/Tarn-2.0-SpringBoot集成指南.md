# Tarn 1.0 Spring Boot 集成指南

> 目标：让已有的 Spring Boot 项目（Boot 2.x / JDK 8）**只加一个依赖**就能使用 Tarn MVC。
> 示例工程：[samples/tarn-springboot-demo](../samples/tarn-springboot-demo)
> Maven 引用与本地/私服安装见：[Tarn-1.0-Maven集成指南.md](./Tarn-1.0-Maven集成指南.md)
> **合并版总手册**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md)（Spring Boot 章节见 §11）

---

## 1. 集成方式：一个 Filter，两套 MVC 共存

`tarn-spring-boot-starter` 只做一件事：把 Tarn 的 `TarnDispatcherFilter` 注册进 Spring Boot 的
Servlet 容器（`FilterRegistrationBean`），不需要 `web.xml`，也不需要改 `DispatcherServlet`。

请求处理流程：

```text
浏览器请求
   │
   ▼
OrderedCharacterEncodingFilter  (Spring Boot)
   │
   ▼
TarnDispatcherFilter             (本 starter 注册，默认 order=10)
   ├─ 命中 Tarn 路由          → Tarn 控制器执行，输出 JSON / 视图，请求结束
   └─ 未命中 Tarn 路由        → filterChain.doFilter(...)
                                   │
                                   ▼
                            Spring DispatcherServlet → @RestController ...
```

关键点：Tarn **未命中的请求会放行**，所以两套 MVC 可以在同一个应用内共存，迁移可以按接口粒度逐步进行。

---

## 2. 依赖

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

starter 的传递依赖：`tarn-framework`（MVC/ORM/路由/连接池），并对 Spring Boot 的
`spring-boot`、`spring-boot-autoconfigure` 声明为 `optional`，不会与业务工程的 Boot 版本冲突。

---

## 3. 三步接入

### 第一步：启动类照旧

```java
@SpringBootApplication
public class BootApplication {
    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }
}
```

不需要 `@Import`、不需要 `@ServletComponentScan`、不需要 `@Bean`。

### 第二步：放两个资源文件

`src/main/resources/context-mapping.xml`（Tarn 扫描控制器/服务，**必须**）：

```xml
<framework>
    <controllers>
        <mapping package="samples.tarn.boot.controller" name="Spring Boot 集成示例" author="dfr"/>
    </controllers>
    <services>
        <mapping package="samples.tarn.boot.service.impl" name="Spring Boot 集成示例服务"/>
    </services>
    <repositorys/>
</framework>
```

`src/main/resources/application.yml`（Tarn 自身配置 + starter 配置）：

```yaml
tarn:
  spring:                    # starter 专属
    enabled: true
    url-patterns:
      - /*
    order: 10
    # 2.1：TarnConfig 实现类，用于注册全局拦截器 / 插件
    config-class: samples.tarn.boot.BootTarnConfig
  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    default-view-type: OTHER
  system:
    debug: true
    encoding: utf-8
  datasource:
    type: MYSQL
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password:
    driver-class-name: com.mysql.cj.jdbc.Driver
    decrypt: false           # 2.1：true 时建池前用 Druid ConfigTools 解密 password
    # public-key: "..."      # 留空用内置默认公钥
```

### 第三步：写 Tarn 控制器（老写法不变）

```java
@EController("/tarn/boot")
public class BootTarnController extends Controller {

    @EAutowired
    private BootGreetService bootGreetService;

    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() {
        return JsonResult.ok("pong-tarn-in-boot");
    }

    @Json
    @ERequestMapping(value = "/hello/{name}", method = RequestMethod.GET,
            isPathVariable = true, regex = "[A-Za-z0-9_-]+")
    public JsonResult hello(@EPathVariable("name") String name) {
        return JsonResult.ok(bootGreetService.greet(name));
    }
}
```

原来的 Spring MVC 控制器**一行都不用改**：

```java
@RestController
@RequestMapping("/spring")
public class SpringHelloController {
    @GetMapping("/hello")
    public Map<String, Object> hello() { /* ... */ }
}
```

---

## 4. starter 配置项（`tarn.spring.*`）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `tarn.spring.enabled` | `true` | 是否启用 Tarn；`false` 时完全不注册过滤器，Spring MVC 独占 |
| `tarn.spring.url-patterns` | `/*` | Tarn 拦截的 URL。想彻底隔离可收敛成 `/tarn/*`（此时 Tarn 控制器根路径也要在 `/tarn` 下） |
| `tarn.spring.order` | `10` | 过滤器顺序，数字越小越先执行 |
| `tarn.spring.config-class` | 空 | `TarnConfig` 实现类全名（等同老 web.xml 的 `configClass` 参数），可选 |

Tarn 自身的配置（`tarn.datasource.*`、`tarn.mvc.*`、`tarn.system.*`、`tarn.redis.*`、`tarn.mybatis.*`）
与 web.xml 时代完全一致，只是从 `dev.properties` 搬到了 `application.yml`。

### 4.1 全局拦截：`tarn.spring.config-class`

老的 `FilterInterface` + `Filters` 机制在 Spring Boot 下不变，只需一个 `TarnConfig` 实现类：

```java
public class BootTarnConfig implements TarnConfig {
    @Override
    public void configFilter(Filters filters) {
        filters.add(new CorsFilter()).add(new AuthFilter());
    }
}
```

```yaml
tarn:
  spring:
    config-class: samples.tarn.boot.BootTarnConfig
```

要点：

- 该拦截器**在路由匹配之前**执行，`return false` 即短路（不会进入 Tarn 控制器，也不会继续到 Spring MVC）；
- 需要**后置 / 环绕**（耗时统计、包装 response）时，用标准 `javax.servlet.Filter` 并注册 `FilterRegistrationBean`，
  `order` 设为小于 `tarn.spring.order`（默认 10）；
- `ExtendFilterImpl` 在 2.x 不再实例化；`expand-filter.xml` 声明式过滤器会先于 `configFilter` 加载；
- 完整的三层拦截模型见《全书》§8。

---

## 5. 自动装配是怎么生效的

`tarn-spring-boot-starter` 内含两个注册文件，同时兼容 Boot 2.6 及以下与 2.7+：

```text
src/main/resources/META-INF/spring.factories
    org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    com.dfrdev.tarn.springboot.TarnAutoConfiguration

src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    com.dfrdev.tarn.springboot.TarnAutoConfiguration
```

`TarnAutoConfiguration` 的生效条件：

| 注解 | 含义 |
| --- | --- |
| `@ConditionalOnWebApplication(type = SERVLET)` | 仅在 Servlet Web 应用生效（WebFlux 不生效） |
| `@ConditionalOnClass(name = "javax.servlet.Filter")` | classpath 有 Servlet API |
| `@ConditionalOnProperty("tarn.spring.enabled", matchIfMissing = true)` | 默认开启，可用配置关闭 |
| `@ConditionalOnMissingBean(name = "tarnDispatcherFilterRegistration")` | 业务工程可自行覆盖注册 Bean |

> 不要直接把 `TarnDispatcherFilter` 暴露成 `@Bean`：那样 Boot 会把它注册成全局过滤器
> （`urlPatterns=/*`、order 最低），与 `FilterRegistrationBean` 的配置可能冲突。

---

## 6. 已验证的示例

`samples/tarn-springboot-demo` 打包后可执行：

```bash
cd tarn-framework-2026-beta
mvn -DskipTests install                 # 框架与 starter 装进本地仓库

cd samples/tarn-springboot-demo
mvn clean package
java -jar target/tarn-springboot-demo-1.0.0.jar
```

实测结果（本机）：

| 请求 | 结果 |
| --- | --- |
| `GET /tarn/boot/ping` | `{"code":200,"data":"pong-tarn-in-boot",...}` |
| `GET /tarn/boot/hello/dfr` | `{"code":200,"data":"Hello, dfr (from Tarn MVC in Spring Boot)",...}` |
| `GET /tarn/boot/add?x=3&y=4` | `{"code":200,"data":{"x":3,"y":4,"sum":7},...}` |
| `GET /tarn/boot/hello/中文` | 正则不匹配 → 未进入 Tarn 控制器 |
| `GET /spring/hello` | `{"msg":"spring-dispatcher-servlet","framework":"Spring MVC"}` |

日志中可见 `Started BootApplication` 且无过滤器启动异常。

---

## 7. 限制与注意事项

| 项 | 说明 |
| --- | --- |
| Spring Boot 版本 | **2.x**（示例 2.7.18）。Boot 3.x 使用 `jakarta.servlet` 且要求 JDK 17，当前框架基于 `javax.servlet` + JDK 8，**不兼容 Boot 3** |
| JDK | 8（框架字节码 target 1.8） |
| 资源文件 | `context-mapping.xml` 必须存在，否则 Tarn 路由表为空（所有 Tarn 接口 404） |
| `dev.properties` | 2.0 起可省略；缺失时框架改用 `application.yml`，不再抛 `Resource of path [dev.properties] not exist` |
| `tarn.plugins` 未配置 | 已修复：为空时不再 NPE（旧版本需显式写 `tarn.plugins: []`） |
| 数据库不可用 | 不影响 MVC 启动（Druid 懒连接）；访问数据库路由时才会因连接超时报错 |
| 静态资源 | 静态资源（js/css/图片）由 Spring Boot 默认的 `ResourceHttpRequestHandler` 处理；Tarn 只在 `intercept-suffix` 白名单内接管带后缀的请求 |
| 日志 | 框架内部仍用 `org.apache.log4j.Logger` API，但通过 `log4j-over-slf4j` 桥接到 Boot 的 logback，不会引入 log4j 1.x 实现 |
| 会话/视图 | 使用 Tarn 视图（freemarker/jsp）时需自行引入对应依赖；纯 JSON 接口无需 |

---

## 8. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 启动报 `Exception starting filter [tarnDispatcherFilter]` + `dev.properties not exist` | 使用了未修复的旧 jar | 升级到 1.0.0 并用 `mvn clean package` 重新打包业务工程 |
| 启动报 `PluginAutoConfig` NPE | `application.yml` 没有 `tarn.plugins` 且使用旧 jar | 同上；或临时加 `tarn.plugins: []` |
| Tarn 接口 404，Spring 接口正常 | `context-mapping.xml` 缺失/包名不对 | 检查资源文件与 `<mapping package>` |
| Spring 接口被 Tarn 抢走 | `url-patterns` 太宽且 Tarn 有同名路由 | 收敛 `url-patterns: /tarn/*`，或调整 Tarn 路由前缀 |
| 想临时关掉 Tarn | — | `tarn.spring.enabled: false` |
| 自定义过滤器顺序 | — | `tarn.spring.order: -100`（比 Boot 编码过滤器更早） |
| 全局拦截器没生效 | `tarn.spring.config-class` 未配置 / 类名错误 / 未实现 `TarnConfig` | 见 §4.1，确认包名与类名完全一致 |
| 启动报「数据源密码解密失败」 | `decrypt: true` 但密码是明文或密文与 `public-key` 不配对 | 明文场景置 `false`；加密场景私钥加密、公钥解密 |

---

## 9. 文件索引

| 文件 | 说明 |
| --- | --- |
| [tarn-spring-boot-starter/pom.xml](../tarn-spring-boot-starter/pom.xml) | starter 依赖定义 |
| [.../TarnAutoConfiguration.java](../tarn-spring-boot-starter/src/main/java/cc/ruixing/framework/tarn/springboot/TarnAutoConfiguration.java) | 过滤器自动注册 |
| [.../TarnSpringProperties.java](../tarn-spring-boot-starter/src/main/java/cc/ruixing/framework/tarn/springboot/TarnSpringProperties.java) | `tarn.spring.*` 配置绑定 |
| [samples/tarn-springboot-demo/pom.xml](../samples/tarn-springboot-demo/pom.xml) | Boot 工程依赖示例 |
| [.../BootApplication.java](../samples/tarn-springboot-demo/src/main/java/samples/tarn/boot/BootApplication.java) | 启动类 |
| [.../BootTarnController.java](../samples/tarn-springboot-demo/src/main/java/samples/tarn/boot/controller/BootTarnController.java) | Tarn 控制器（含动态路由） |
| [.../SpringHelloController.java](../samples/tarn-springboot-demo/src/main/java/samples/tarn/boot/controller/SpringHelloController.java) | Spring MVC 控制器（共存验证） |
| [.../application.yml](../samples/tarn-springboot-demo/src/main/resources/application.yml) | 配置示例 |
| [.../context-mapping.xml](../samples/tarn-springboot-demo/src/main/resources/context-mapping.xml) | 控制器/服务扫描 |
