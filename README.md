# Tarn Framework

Tarn 是一套轻量级 Java Web 全栈框架，覆盖 MVC 控制器、静态/动态路由、ORM、参数化 SQL、多数据库方言、依赖注入、JSON 前后端分离、插件体系与内嵌 Jetty 独立运行。

- **Maven 版本**：`1.0.0`（功能代际见 `docs/` 中的 2.x 文档）
- **JDK**：1.8+
- **构建**：Maven 3.6+
- **包名**：`com.dfrdev.tarn`
- **部署方式**：Tomcat / 内嵌 Jetty / Spring Boot

---

## 特性总览

| 能力 | 说明 |
|------|------|
| MVC | 控制器扫描、参数绑定、视图渲染（JSP / FreeMarker / Velocity / JSON） |
| 路由 | 静态路由 O(1) 命中 + 动态路由（`{id}`、正则、`${mapping}` 动态根路径） |
| 依赖注入 | 自研轻量 IoC 容器：`@EComponent` / `@EService` / `@ERepository`、`@EAutowired` / `@EResource`、生命周期回调、三级缓存解决循环依赖 |
| ORM | `Q` 老继承链与 `OrmTemplate` 组合引擎并存，参数化 SQL，多数据库方言 |
| AOP | `@Inject`、`@Before` 声明式注入与拦截 |
| JSON | `@Json` + `JsonResult`，前后端分离 |
| 插件 | Redis / MyBatis / Druid / Ehcache / SqlScript 可插拔 |
| 安全 | 路径穿越防护、上传白名单与魔数校验、安全响应头、Cookie HttpOnly/SameSite、重定向白名单 |
| 独立运行 | 内嵌 Jetty 9.4，`StandaloneLauncher.run()` |
| Spring Boot | `tarn-spring-boot-starter` 自动装配，可与 Spring MVC 共存 |

## 工程结构

```
tarn-parent
├── tarn-framework/               框架核心
├── tarn-spring-boot-starter/     Spring Boot 自动装配
├── tarn-demo/                    示例工程（路由 / ORM / 依赖注入 / 独立运行 / 过滤器）
├── samples/
│   ├── tarn-maven-demo/          Maven 集成最小样例
│   └── tarn-springboot-demo/     Spring Boot 共存样例
├── docs/                         全套文档
└── libs/                         工程内本地仓库
```

## 安装

### Maven 依赖

已完成 `mvn install` 或使用工程内 `libs/` 仓库时：

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot 集成：

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 源码构建

```bash
git clone https://github.com/wzdfr/tarn.git
cd tarn
mvn clean install -DskipTests
```

只构建核心与 Starter：

```bash
mvn clean install -pl tarn-framework,tarn-spring-boot-starter -am -DskipTests
```

## 快速开始

### 1. 声明扫描包

`src/main/resources/context-mapping.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<framework>
    <controllers>
        <mapping package="com.example.controller" name="控制器"/>
    </controllers>
    <services>
        <mapping package="com.example.service.impl" name="服务实现"/>
    </services>
    <repositorys>
        <mapping package="com.example.repository.impl" name="数据层实现"/>
    </repositorys>
    <components>
        <mapping package="com.example.component" name="通用组件"/>
    </components>
</framework>
```

`controllers` 与 `services` 必填；`repositorys`、`components` 可选。

### 2. 编写控制器

```java
package com.example.controller;

import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.json.JsonResult;

@EController("/hello")
public class HelloController {

    @Json
    @ERequestMapping(value = "/hi", method = RequestMethod.GET)
    public JsonResult hi(@ERequestParam("name") String name) {
        return JsonResult.ok("Hello " + (name == null ? "Tarn" : name));
    }
}
```

请求 `GET /hello/hi?name=Tarn` 返回：

```json
{"code":200,"message":"success","data":"Hello Tarn","timestamp":1730000000000}
```

### 3. 启动

**方式一：内嵌 Jetty 独立运行**

```java
import com.dfrdev.tarn.standalone.StandaloneLauncher;

public class App {
    public static void main(String[] args) {
        StandaloneLauncher.run();
    }
}
```

`application.yml`：

```yaml
tarn:
  server:
    port: 8080
    context-path: /
    webapp-dir: standalone-web          # 含 WEB-INF 的 Web 目录
    config-class: com.example.AppConfig # 可选，TarnConfig 实现类
```

```bash
# IDE 直接运行 main，或：
mvn exec:java -Dexec.mainClass=com.example.App
```

**方式二：Tomcat（web.xml）**

```xml
<filter>
    <filter-name>TarnDispatcher</filter-name>
    <filter-class>com.dfrdev.tarn.core.TarnDispatcherFilter</filter-class>
    <init-param>
        <param-name>configClass</param-name>
        <param-value>com.example.AppConfig</param-value>
    </init-param>
    <init-param>
        <param-name>encoding</param-name>
        <param-value>UTF-8</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>TarnDispatcher</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**方式三：Spring Boot**

```yaml
tarn:
  spring:
    enabled: true
    url-patterns:
      - /tarn/*        # 与 Spring MVC 共存时建议限定前缀
    order: 10
```

```java
@SpringBootApplication
public class BootApplication {
    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }
}
```

`context-mapping.xml` 与 Tarn 自身的 `tarn.datasource` / `tarn.mvc` 配置照常使用；`tarn.spring.*` 只负责过滤器注册。

## 依赖注入

Tarn 内置轻量 IoC 容器（`TarnApplicationContext`），容器管理的 Bean 自动完成字段注入与生命周期回调，无需再手写 `ServiceInject.inject(this)`。完整说明见 [依赖注入使用文档](docs/Tarn-2.2-依赖注入使用文档.md)。

### 注解

| 注解 | 目标 | 作用 |
|------|------|------|
| `@EService("name")` | 类 | 注册为单例服务，默认名称首字母小写类名 |
| `@ERepository("name")` | 类 | 注册为单例数据层组件 |
| `@EComponent("name")` | 类 | 注册为单例通用组件 |
| `@EAutowired("name")` | 字段 | 按名称注入，名称未命中时按类型回退 |
| `@EResource("name")` | 字段 | 注入数据层组件（受 `limitPackage` 约束） |
| `@EPostConstruct` | 方法 | 初始化回调（无参） |
| `@EPreDestroy` | 方法 | 销毁回调（容器关闭时逆序执行） |

注解包路径：`com.dfrdev.tarn.aop.annotation`。同时兼容 `javax.annotation.PostConstruct` / `PreDestroy`（含 `jakarta`）。

### 服务与组件

```java
@EService("bookService")
public class BookServiceImpl implements BookService {

    @EAutowired
    private UserService userService;
}

@EComponent
public class BookFacade {

    @EAutowired
    private BookService bookService;

    @EPostConstruct
    public void init() {
        // 此时 bookService 已注入完成
    }

    @EPreDestroy
    public void close() {
        // 容器 close() 时调用
    }
}
```

### 控制器注入

Controller 由容器以**原型作用域**创建：每个请求一个新实例，并完整执行注入与生命周期。

```java
@EController("/book")
public class BookController {

    @EAutowired
    private BookFacade bookFacade;

    @Json
    @ERequestMapping(value = "/detail", method = RequestMethod.GET)
    public JsonResult detail(@ERequestParam("id") Long id) {
        return JsonResult.ok(bookFacade.detail(id));
    }
}
```

### 生命周期与循环依赖

```text
实例化 → 暴露早期引用 → 字段注入 → Aware 回调
  → BeanPostProcessor.before → @EPostConstruct / @PostConstruct
  → InitializingBean.afterPropertiesSet → BeanPostProcessor.after → 放入单例池
```

单例之间的字段循环依赖由三级缓存自动解决；构造方法或静态块中调用 `getBean` 形成的循环会抛异常。原型 Bean 之间不支持循环依赖。

### 容器 API

```java
TarnApplicationContext context = TarnApplicationContext.getInstance();

BookFacade facade = context.getBean(BookFacade.class);          // 按类型
BookService service = context.getBean("bookService", BookService.class);
boolean exists = context.containsBean("bookService");
Map<String, BookService> all = context.getBeansOfType(BookService.class);

Object draft = context.createBean(OrderDraft.class);            // 新建原型实例

// 容器外 new 出来的对象需显式处理
BookReportHelper helper = new BookReportHelper();
context.injectBean(helper);
```

### 使用边界

- 仅支持字段注入，Bean 需有无参构造方法，注入字段不能是 `final`；
- 只管理容器内的 Bean，自己 `new` 的对象不会自动注入；
- 不做 `@Value` 配置注入、构造器注入、AOP 代理与声明式事务；
- 依赖未注册时注入 `null` 并输出告警，不阻塞启动；
- Spring Boot 环境下 Spring 不识别 `@EAutowired`，可在 Spring Bean 中通过 `TarnApplicationContext.getInstance().getBean(...)` 获取 Tarn 服务。

## 核心功能

### 路由与参数

| 注解 / 写法 | 说明 |
|-------------|------|
| `@EController("/api")` | 控制器根路径，静态路径直接注册路由表 |
| `@EController("/api/${mapping}")` | 动态根路径，由 `controller-type=auto` 启用 |
| `@ERequestMapping(value, method, regex, isPathVariable)` | 方法级路由，支持 HTTP 方法限定、正则约束与路径变量 |
| `@ERequestParam("name")` | 绑定请求参数 / 表单字段 |
| `@EPathVariable("id")` | 绑定 `{id}` 路径变量 |
| `@ERequestBody` | 绑定 JSON 请求体 |
| `@Json` | 返回值序列化为 JSON |

```java
@Json
@ERequestMapping(value = "/detail/{id}", method = RequestMethod.GET,
        isPathVariable = true, regex = "[0-9]+")
public JsonResult detail(@EPathVariable("id") Long id) {
    return JsonResult.ok(bookService.findById(id));
}
```

### 视图渲染

控制器返回字符串即视图名，按 `default-view-type` 选择渲染器：

```properties
default-view-type=FREE_MARKER   # JSP / FREE_MARKER / VELOCITY / OTHER
template-base-directory=/WEB-INF/views
```

```java
@ViewRootPath("/WEB-INF/views/user/")
public class UserController extends Controller {
    public String detail() {
        return "detail.jsp";
    }
}
```

### ORM 与参数化 SQL

```java
SqlBuilder builder = SqlBuilder
        .select("ID, TITLE, AUTHOR, STOCK")
        .from("demo_book")
        .where("TITLE LIKE #{keyword}")
        .bind("keyword", "%" + keyword + "%");

Q q = new Q();
List<Book> books = (List<Book>) (List<?>) q.query(builder.build(), Book.class);

Object key = q.insertReturnKey(book);      // 主键同时回填到实体
int rows = q.updateRows(book);             // 默认跳过 null 字段
```

所有条件值通过 `#{name}` + `bind(...)` 参数绑定，不拼接字符串。

### 插件

```yaml
tarn:
  redis:
    enabled: true
    host: 127.0.0.1
    port: 6379
  mybatis:
    enabled: true
  ehcache:
    enabled: true
  plugins:
    - com.example.plugin.MyPlugin
```

## 配置说明

Tarn 优先读取 classpath 下的 `application.yml`，兼容老项目 `dev.properties`；老 key（如 `DBType`、`controller-type`）会自动映射。

```yaml
tarn:
  datasource:
    type: MYSQL                 # MYSQL / ORACLE / MSSQL / POSTGRESQL / DM / KINGBASE / GAUSSDB / VASTBASE
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8
    username: root
    password: "${TARN_DB_PASSWORD:}"
    driver-class-name: com.mysql.cj.jdbc.Driver

  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    template-base-directory: /WEB-INF/views
    default-view-type: FREE_MARKER
    dev-mode: true

  system:
    debug: true
    encoding: utf-8
```

`controller-type` 取值：

| 值 | 说明 |
|----|------|
| `auto`（默认） | 兼容调度：`${mapping}` 动态根路径与 `@ERequestMapping` 注解共同生效 |
| `requestMapping` | 仅按 `@ERequestMapping` 注解匹配 |
| `mapping` | 同 `requestMapping`（历史别名） |
| `param` / `method` | 已废弃，启动时告警并降级为 `auto` |

## 安全配置

### 外链重定向白名单

只有视图地址以 `http://` / `https://` 开头、且主机命中白名单时才执行 302 跳转；未配置白名单时**拒绝所有外链重定向**。

```properties
# dev.properties：多个主机用英文逗号分隔，*. 前缀表示匹配子域
redirectAllowedHosts=example.com,*.trusted.com
```

```yaml
# application.yml（顶层键）
redirectAllowedHosts: example.com,*.trusted.com
```

```java
public class LoginController extends Controller {
    public String goHome() {
        return "https://example.com/home";   // 命中白名单 → 302
    }

    public String unsafe() {
        return "https://evil.com/x";         // 未命中 → 抛出 RenderException
    }
}

// 也可以在启动时用代码覆盖
RenderFactory.setRedirectAllowedHosts("example.com", "*.trusted.com");
```

- 相对路径（如 `/login`、`/WEB-INF/views/a.jsp`）不受影响，仍走内部渲染。
- 白名单为空 = 只允许内部跳转，禁止外链。

### JSP 视图目录限定

JSP 视图必须位于 `templateBaseDirectory` 内；未配置时默认 `/WEB-INF/views`。

```properties
defaultViewType=JSP
templateBaseDirectory=/WEB-INF/views
```

```java
@ViewRootPath("/WEB-INF/views/user/")
public class UserController extends Controller {
    public String detail() {
        return "detail.jsp";      // 实际渲染 /WEB-INF/views/user/detail.jsp
    }

    public String unsafe() {
        return "/index.jsp";      // 不在模板根内 → 抛出 RenderException
    }
}
```

- 含 `..`、反斜杠、`\0` 的视图路径一律拒绝；FreeMarker / Velocity 同样禁止 `..`。
- 视图前缀请使用 `@ViewRootPath` 注解设置；`viewRootPath` 属于框架自有属性，不接受请求参数注入。

### 环境变量占位符

`application.yml` 的字符串值支持 `${ENV_NAME}` 与 `${ENV_NAME:默认值}`，解析顺序：环境变量 → JVM 系统属性（`-D`）→ 默认值。

```yaml
tarn:
  datasource:
    password: "${TARN_DB_PASSWORD:}"
```

```bash
export TARN_DB_PASSWORD='your-password'
java -jar app.jar
```

`tarn-demo` 与 `samples/tarn-maven-demo` 的数据库口令已改为该方式：未设置 `TARN_DB_PASSWORD` 时为空串，请按实际环境注入，不要提交明文口令。

### 上传目录与文件限制

1. 保存目录必须是**相对路径**，由框架解析到上传根目录内：

```java
UploadFile f1 = getFile("file");                    // 上传根目录
UploadFile f2 = getFile("file2", "avatar/2026/");    // 根目录下的子目录
// getFile("file", "/opt/upload/") 或含 ".." → IllegalArgumentException
```

2. 默认上传根目录位于 **Web 根之外**：`<WebRoot>_upload/`（jar 模式为 `<contextRealPath>_upload/`），启动时自动创建；可在初始化前覆盖：

```java
Config.setUploadedFileSaveDirectory("/data/tarn-upload/");
```

3. 默认启用扩展名白名单：`jpg/jpeg/png/gif/bmp/webp/pdf/txt/csv/doc/docx/xls/xlsx/ppt/pptx/zip/rar/7z`，并对图片、PDF、ZIP 类文件做魔数校验；如需收窄：

```java
MultipartRequest.setAllowedExtensions(new String[]{"jpg", "png", "pdf"});
```

4. 落盘文件名随机化为 UUID，原始文件名通过 `UploadFile.getOriginalFileName()` 获取。

### 安全响应头

框架默认输出 `X-Content-Type-Options: nosniff`、`X-Frame-Options: SAMEORIGIN`、`Referrer-Policy: no-referrer`，HTTPS 请求自动追加 HSTS。

```properties
# application.yml 顶层键 / dev.properties
securityHeaders=true
# 可选：自定义 CSP，不配置则不输出该响应头
contentSecurityPolicy=default-src 'self'; img-src 'self' data:
```

### Cookie 属性

框架写入的 Cookie 默认带 `HttpOnly`、`SameSite=Lax`，HTTPS 下自动加 `Secure`：

```properties
# 可选值：Lax（默认）/ Strict / None
cookieSameSite=Lax
```

## 示例工程

`tarn-demo` 启动后可访问：

| 路径 | 说明 |
|------|------|
| `/standalone/book/list` | ORM 列表查询（内嵌 Jetty 示例） |
| `/standalone/book/detail/1` | 路径变量 + 正则约束 |
| `/di/order?sku=book-1&qty=2` | 依赖注入 + 循环依赖 |
| `/di/lifecycle` | `@EPostConstruct` / Aware 回调 |
| `/di/post-processor` | `BeanPostProcessor` 统计 |
| `/di/context` | 容器 API 查询 Bean |
| `/di/draft` | `createBean` 创建原型实例 |

## 文档

| 文档 | 说明 |
|------|------|
| [依赖注入使用文档](docs/Tarn-2.2-依赖注入使用文档.md) | IoC 容器、注解、生命周期、循环依赖与边界 |
| [白皮书与升级迁移指南](docs/Tarn-2.0-白皮书与升级迁移指南.md) | 框架设计理念与迁移说明 |
| [框架项目说明书](docs/Tarn-2.0-框架项目说明书.md) | 工程结构与核心 API |
| [项目方案与使用全书](docs/Tarn-2.1-项目方案与使用全书.md) | 一站式完整手册 |
| [路由与独立运行指南](docs/Tarn-2.0-路由与独立运行使用指南.md) | 路由配置与 Standalone 模式 |
| [ORM 使用指南](docs/Tarn-2.0-ORM使用指南.md) | ORM 引擎与方言 |
| [Spring Boot 集成指南](docs/Tarn-2.0-SpringBoot集成指南.md) | Starter 接入 |
| [Maven 集成指南](docs/Tarn-2.0-Maven集成指南.md) | Maven 项目接入 |
