# Tarn Framework

Tarn 是一套轻量级 Java Web 全栈框架，覆盖 MVC 控制器、静态/动态路由、ORM、参数化 SQL、多数据库方言、声明式插件、JSON 前后端分离、内嵌容器独立运行全链路。

- **版本**：2.0.0
- **JDK**：1.8+
- **构建**：Maven 3
- **包名**：`com.dfrdev.tarn`

---

## 工程结构

```
tarn-parent
├── tarn-framework/               框架核心
├── tarn-spring-boot-starter/     Spring Boot 自动装配
├── tarn-demo/                    示例工程（路由 / ORM / 独立运行 / 过滤器等）
├── samples/
│   ├── tarn-maven-demo/          Maven 集成最小样例
│   └── tarn-springboot-demo/     Spring Boot 共存样例
├── docs/                         全套文档
└── libs/                         工程内本地仓库
```

## Maven 坐标

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>2.0.0</version>
</dependency>
```

Spring Boot 集成：

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

## 核心入口

| 入口 | 场景 |
|------|------|
| `TarnDispatcherFilter` | Web 请求总入口（Tomcat / 内嵌 Jetty 共用） |
| `Router` / `RouteDispatcher` | 静态路由表 O(1) 命中 + 动态路由（`{id}` / 正则） |
| `Orm.template()` | 组合式 ORM 引擎，无需继承任何类 |
| `StandaloneLauncher.run()` | 无 Tomcat 独立运行（内嵌 Jetty 9.4） |

## 核心能力

- **MVC**：控制器扫描、参数注入、视图渲染（JSP / FreeMarker / Velocity / JSON）
- **路由**：静态路由表 + 动态路径参数 + 正则匹配
- **ORM**：多数据库方言（MySQL / PostgreSQL / Oracle / SQLServer / DM / Kingbase / GaussDB / Vastbase / Oracle12c）、级联映射、参数化 SQL
- **AOP 注入**：`@Inject` / `@Before` / `@EController` / `@Etable` 声明式注入与拦截
- **插件体系**：Redis / MyBatis / Druid / Ehcache / SqlScript 可插拔
- **缓存**：Ehcache / Redis 双支持
- **独立运行**：内嵌 Jetty，无需外部容器
- **Spring Boot 集成**：`tarn-spring-boot-starter` 自动装配

## 安全配置

> 配置键可写在 `dev.properties`（老方式）或 `application.yml` 顶层（新方式，优先级更高）。

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

## 快速开始

```bash
# 编译框架
mvn clean install -pl tarn-framework,tarn-spring-boot-starter

# 运行示例
cd tarn-demo
mvn jetty:run
```

## 文档

| 文档 | 说明 |
|------|------|
| [白皮书与升级迁移指南](docs/Tarn-2.0-白皮书与升级迁移指南.md) | 框架设计理念与迁移说明 |
| [框架项目说明书](docs/Tarn-2.0-框架项目说明书.md) | 工程结构与核心 API |
| [项目方案与使用全书](docs/Tarn-2.1-项目方案与使用全书.md) | 一站式完整手册 |
| [路由与独立运行指南](docs/Tarn-2.0-路由与独立运行使用指南.md) | 路由配置与 Standalone 模式 |
| [ORM 使用指南](docs/Tarn-2.0-ORM使用指南.md) | ORM 引擎与方言 |
| [Spring Boot 集成指南](docs/Tarn-2.0-SpringBoot集成指南.md) | Starter 接入 |
| [Maven 集成指南](docs/Tarn-2.0-Maven集成指南.md) | Maven 项目接入 |


