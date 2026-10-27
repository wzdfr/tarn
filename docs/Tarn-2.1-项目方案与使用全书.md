# Tarn 2.1 项目方案与使用全书

> 适用版本：`cc.ruixing.framework:tarn-framework:2.0.0`（2.1 为能力代号，`Tarn.version()` 仍返回 `2.0.0`）
> 运行环境：JDK 8、Servlet 3.1、Maven 3.6+，验证数据库 MySQL 5.7
> 读者：平台架构师、业务开发、老项目维护负责人
> 核心承诺：**存量 1.x 项目替换 jar 即可运行，零代码改动；新能力按需增量启用。**

本文是把框架说明书、白皮书、路由、ORM、Maven、Spring Boot 六份文档合并后的**单一权威手册**，
并包含 2.1 新增的**数据源密码加密、AOP 注入体系重构、全局拦截方案**。
细粒度的 ORM API 全量与断言明细仍可参考 [Tarn-2.0-ORM使用指南.md](./Tarn-2.0-ORM使用指南.md)。

---

## 目录

1. [框架概览](#1-框架概览)
2. [工程结构](#2-工程结构)
3. [快速开始](#3-快速开始)
4. [配置体系](#4-配置体系)
5. [MVC 与请求链路](#5-mvc-与请求链路)
6. [依赖注入与 AOP 注入体系](#6-依赖注入与-aop-注入体系)
7. [路由系统](#7-路由系统)
8. [全局拦截](#8-全局拦截)
9. [ORM](#9-orm)
10. [插件机制](#10-插件机制)
11. [Spring Boot 集成](#11-spring-boot-集成)
12. [Maven 集成与发布](#12-maven-集成与发布)
13. [独立运行（非 Tomcat）](#13-独立运行非-tomcat)
14. [安全与性能](#14-安全与性能)
15. [升级迁移](#15-升级迁移)
16. [故障排查](#16-故障排查)
17. [附录](#17-附录)

---

## 1. 框架概览

### 1.1 定位

Tarn 是自研的轻量级 Java Web 框架，覆盖 **MVC 控制器、静态/动态路由、ORM、参数化 SQL、
多数据库方言、声明式插件、JSON 前后端分离、内嵌容器独立运行** 全链路。

设计目标是「**老系统不动，新能力可选**」：

| 目标 | 落地方式 |
| --- | --- |
| 老项目零改动升级 | 老类、老方法、老注解、老配置 key 全部保留，内部委托新实现 |
| 修复历史质量缺陷 | 去掉静默吞异常、修反射全量扫描、修会话泄漏、补参数化 API |
| 支持前后端分离 | `@Json` + `JsonResult` + `JsonKit` |
| 支持主流工程化 | Maven 多模块、YAML 主配置、声明式插件装配 |
| 数据层现代化 | `EntityMapper` 元数据缓存、`SqlBuilder` 参数化、9 种方言分页 |
| 脱离 Tomcat | 内嵌 Jetty 的 `StandaloneLauncher` |
| 生态扩展 | `Plugin` 新接口 + `PluginAdapter` 兼容老 `IPlugin` |

### 1.2 核心入口

| 入口 | 场景 |
| --- | --- |
| `TarnDispatcherFilter` | Web 请求总入口（Tomcat / Jetty / Spring Boot 共用） |
| `Router` / `RouteDispatcher` | 静态路由 O(1) 命中 + 动态路由（`{id}` / 正则） |
| `Orm.template()` | **ORM 新入口（推荐）**，组合式引擎，无需继承 |
| `new Q()` / `new Tarn()` | ORM 老入口（签名保留，内部委托同一引擎） |
| `TarnConfig` | 框架装配入口：`configFilter` / `configPlugin` |
| `StandaloneLauncher.run()` | 无 Tomcat 独立运行（内嵌 Jetty 9.4） |

### 1.3 版本与兼容承诺

| 项 | 说明 |
| --- | --- |
| 版本号 | 父 POM 统一管理，当前 `2.0.0` |
| 2.1 代号 | ORM 组合式引擎、注入体系重构、数据源解密、全局拦截规范等增量能力 |
| 二进制兼容 | 2.x 未删除/未改签名任何 1.x 公开 API |
| 配置兼容 | `dev.properties` 老 key 全部可读；`application.yml` 优先 |
| 回滚 | 替换回旧 jar + 保留 `dev.properties` 即可，无表结构变更 |

---

## 2. 工程结构

### 2.1 Maven 多模块

```
tarn-framework-2026-beta/
├── pom.xml                         父 POM（版本 / 依赖 / 插件管理）
├── docs/                           文档（本手册 + 分册 + 迁移说明）
├── libs/                           工程内本地仓库（中央仓库缺失的历史 jar，如 cos）
├── tarn-framework/                  框架核心（发布物 tarn-framework-2.0.0.jar）
├── tarn-spring-boot-starter/        Spring Boot 自动装配（发布物）
├── tarn-demo/                       示例工程（不随框架发布）
│   ├── src/main/java/examples/     demo / test / standalone / orm / filter 示例
│   ├── src/main/resources/         application.yml / dev.properties / context-mapping.xml
│   ├── standalone-web/             内嵌 Jetty 的 webapp（极简 web.xml）
│   └── web/                        传统 Tomcat 部署演示
└── samples/
    ├── tarn-maven-demo/             外部工程 Maven 引用最小样例（不进父模块）
    └── tarn-springboot-demo/        Spring Boot 共存样例
```

> `samples/**` 与 `tarn-demo` 都不是框架发布物，可独立构建，用来模拟「同事电脑上的业务工程」。

### 2.2 框架包结构（`cc.ruixing.framework.tarn`）

| 包 | 职责 |
| --- | --- |
| `core` | 内核：`TarnDispatcherFilter` / `Config` / `Tarn` / `PageContext` / `Pager`；`Q`/`DB`/`DAOAbstract` 老门面 |
| `core.connection` | 连接池抽象与线程事务：`ConnectionFactory` / `ConnectionInit` / `DruidPlugin` |
| `core.sql` | 参数化 SQL：`SqlBuilder` / `QueryBuilder` / `ParameterizedSql` |
| `route` | 路由：`Router`（静态表 + 动态表）/ `RouteEntry` / `RouteDispatcher` |
| `orm` | 组合式引擎：`OrmTemplate` / `Orm` / `TransactionCallback` / `EntitySql` / `EntityMapper` |
| `orm.dialect` | 9 种方言分页：MySQL / PostgreSQL / Oracle / Oracle12c / SQLServer / DM / Kingbase / GaussDB / Vastbase |
| `aop` | 注入与调度：`Inject` / `ServiceInject` / `RepositoryInject` / `ExecuteInject` / `BeforeInject` / `AttrInject` / `TarnInject` |
| `aop.inject` | 2.1 抽取的注入基础设施：`BeanProperty` / `BeanProperties` / `ComponentRegistry` / `ComponentScanner` / `ComponentInjector` |
| `aop.annotation` | 注解体系：`EController` / `ERequestMapping` / `EService` / `EAutowired` / `Before` / `Etable` / `Ekey` / `Json` ... |
| `json` | `JsonKit` 统一门面 / `JsonResult` |
| `render` | 视图渲染：JSP / FreeMarker / Velocity / Json / Text / File / Redirect |
| `plugin` | 插件体系：`Plugin` / `IPlugin` / `PluginAdapter` / `PluginManager` / `PluginAutoConfig` + Redis / MyBatis / Druid / Ehcache / sqlScript |
| `config` | `YamlConfigKit`（yml 扁平化 + 老 key 桥接） |
| `standalone` | `StandaloneLauncher`（内嵌 Jetty） |

---

## 3. 快速开始

### 3.1 三条落地路线

| 路线 | 适用 | 启动入口 | 配置 |
| --- | --- | --- | --- |
| 传统 Tomcat | 存量老项目、war 部署 | `web.xml` 的 `TarnDispatcherFilter` | `configClass` + `application.yml`/`dev.properties` |
| 内嵌 Jetty | 本地开发、小工具、无容器环境 | `StandaloneLauncher.run()` | `tarn.server.*` |
| Spring Boot | 新系统 / 已有 Boot 工程共存 | `tarn-spring-boot-starter` 自动装配 | `tarn.spring.*` |

### 3.2 最小可运行步骤（以独立运行为例）

**① 依赖**

```xml
<dependency>
    <groupId>cc.ruixing.framework</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>2.0.0</version>
</dependency>
<!-- 独立运行需要：jetty-server / jetty-webapp / javax.servlet-api -->
```

**② `context-mapping.xml`（必须）**

```xml
<framework>
    <controllers>
        <mapping package="samples.tarn.maven.controller" name="示例控制器"/>
    </controllers>
    <services>
        <mapping package="samples.tarn.maven.service.impl" name="示例服务实现"/>
    </services>
    <repositorys/>
</framework>
```

**③ `application.yml`**

```yaml
tarn:
  datasource:
    type: MYSQL
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: "***"
    driver-class-name: com.mysql.cj.jdbc.Driver
  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    default-view-type: OTHER
  system:
    debug: true
  server:
    port: 9090
    context-path: /
    webapp-dir: web
    # config-class: samples.tarn.maven.AppConfig   # 需要全局拦截/插件装配时填 TarnConfig 实现类
```

**④ 控制器 + 服务 + 实体**

```java
@EController("/book")
public class BookController extends Controller {

    @EAutowired
    private BookService bookService;

    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() { return JsonResult.ok("pong"); }

    @Json
    @ERequestMapping(value = "/detail/{id}", method = RequestMethod.GET,
            isPathVariable = true, regex = "[0-9]+")
    public JsonResult detail(@EPathVariable("id") Long id) { ... }
}
```

**⑤ 启动**

```java
public class DemoApplication {
    public static void main(String[] args) {
        StandaloneLauncher.run();   // 读 tarn.server.*；也可 run(port, contextPath, webappDir[, configClass])
    }
}
```

**⑥ 验证**

```bash
curl http://127.0.0.1:9090/book/ping
# {"code":200,"data":"pong","message":"success","success":true,...}
```

启动成功的日志特征：

```
ruixing framework.Tarn Successfully Start ...
[Router] 路由表构建完成, 静态 N 条, 动态 M 条
[StandaloneLauncher] Tarn 独立运行已启动: http://127.0.0.1:9090/
```

---

## 4. 配置体系

### 4.1 优先级

```
application.yml（YamlConfigKit 扁平化 + 老 key 桥接）
        ▲ 优先
dev.properties（老配置，全部 key 仍可读）
        ▲ 兜底
框架内置安全默认值
```

- yml 里的 `tarn.datasource.url/username/password/driver-class-name` 覆盖 `druid.properties` 同名项；
- **写空值也视为「已配置」**（可覆盖为空的密码），未写的项保持连接池配置文件原值；
- 关键配置缺失有安全默认值（如 FreeMarker `template-update-delay` 缺省 3600 秒），只迁移 yml 不会启动失败。

### 4.2 `tarn.datasource.*`（数据源）

| key | 说明 |
| --- | --- |
| `type` | 方言：`MYSQL`/`ORACLE`/`ORACLE12C`/`MSSQL`/`POSTGRESQL`/`DM`/`KINGBASE`/`GAUSSDB`/`VASTBASE`，缺省按 URL 自动识别 |
| `db-version` | 数据库版本（老 `dbTypeVersion`，如 MSSQL 2008） |
| `pool-type` | 连接池：`druid`（推荐，默认）/ `system` |
| `config-file` | 连接池配置文件（classpath，默认 `druid.properties`） |
| `url` / `username` / `password` / `driver-class-name` | 直连配置，优先级最高 |
| `decrypt` | **2.1 新增**：`true` 时启动阶段用 Druid `ConfigTools` 解密 `password` |
| `public-key` | **2.1 新增**：解密公钥；留空则使用 Druid 内置默认公钥 |

### 4.3 数据源密码加密（2.1）

**生成密钥与密文**（Druid 自带工具，输出 `privateKey` / `publicKey` / `password` 三段）：

```bash
java -cp druid-1.2.x.jar com.alibaba.druid.filter.config.ConfigTools "明文密码"
```

**配置**：

```yaml
tarn:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo
    username: root
    password: "<上一步输出的 password 密文>"
    driver-class-name: com.mysql.cj.jdbc.Driver
    decrypt: true
    public-key: "<上一步输出的 publicKey>"   # 不填 = 用 Druid 内置默认公钥
```

**实现要点**：

1. `ConnectionInit` 把 `decrypt` / `public-key` 透传进 `DruidPlugin` 的 `Properties`；
2. `DruidPlugin` 在建池前完成解密，把明文写回 `password`，并移除 `decrypt` / `public-key`
   （避免 Druid 二次解密）；
3. 解密失败**直接启动报错**：`[Tarn] 数据源密码解密失败，请检查 decrypt / public-key 与密文是否成对`；
4. 密钥方向约定：`ConfigTools.genKeyPair(512)` 返回 **`[私钥, 公钥]`**，
   **私钥加密、公钥解密**。把 `pair[0]` 当公钥传给 `decrypt` 会报
   `InvalidKeySpecException: algid parse error, not a sequence`。

已验证场景：自定义公钥、Druid 默认公钥、明文不加密（`decrypt: false`）、错误密文（启动拒绝）四组全部符合预期。

### 4.4 `tarn.mvc.*`（MVC）

| key | 说明 |
| --- | --- |
| `controller-type` | 老链路调度方式：`param` / `method` / `mapping` / `auto` |
| `intercept-suffix` | Tarn 接管的后缀白名单（`jspx\|jsp\|do\|u\|shtml\|rx`）；不在名单内的带后缀请求直接放行 |
| `template-base-directory` | 视图模板目录（默认 `/WEB-INF/views`） |
| `default-view-type` | 默认视图：`JSP` / `FREE_MARKER` / `VELOCITY` / `OTHER` |
| `dev-mode` | 开发模式（模板热加载） |
| `freemarker.template-update-delay` | 模板更新检测间隔（秒），缺省 3600 |
| `freemarker.string-template-cache` | 字符串模板缓存刷新间隔（秒），缺省 300 |
| `freemarker.multi-dir` | 多路径模板加载 |
| `extend-filter.enabled` / `extend-filter.impl` | 老 `ExtendFilter` / `ExtendFilterImpl` 的配置映射（见 §8.6 迁移提示） |

### 4.5 `tarn.system.*` / `tarn.session.*`

| key | 说明 |
| --- | --- |
| `system.debug` | 输出 SQL 与反射信息；**生产必须 false** |
| `system.save-sql-log` | SQL 持久化到 `SQLEXECLOG` 表（严重拖慢）；生产关闭 |
| `system.encoding` | 全局编码（默认 utf-8） |
| `system.run-mode` | `jar` / 空（IDE 开发态） |
| `session.store-type` | 变量存储：`session` / `cookie` |
| `session.expires` | 会话过期时间 |

### 4.6 `tarn.server.*`（独立运行）/ `tarn.spring.*`（Boot）

| key | 说明 |
| --- | --- |
| `server.port` / `server.context-path` / `server.webapp-dir` | 内嵌 Jetty 监听与 webapp 目录 |
| `server.config-class` | `TarnConfig` 实现类全名（等同 web.xml 的 `configClass`） |
| `spring.enabled` | 是否启用 Tarn 过滤器（默认 true） |
| `spring.url-patterns` | Tarn 拦截的 URL（默认 `/*`） |
| `spring.order` | 过滤器顺序（默认 10） |
| `spring.config-class` | `TarnConfig` 实现类全名 |

### 4.7 老 key 映射表（`YamlConfigKit.LEGACY_KEY_MAP`）

| 老 key（dev.properties） | 新 key（application.yml） |
| --- | --- |
| `DBType` | `tarn.datasource.type` |
| `dbTypeVersion` | `tarn.datasource.db-version` |
| `connectionType` | `tarn.datasource.pool-type` |
| `connection-config-file` | `tarn.datasource.config-file` |
| `jdbcUrl` | `tarn.datasource.url` |
| `jdbcUser` | `tarn.datasource.username` |
| `jdbcPassword` | `tarn.datasource.password` |
| `jdbcDriverClass` | `tarn.datasource.driver-class-name` |
| `controller-type` | `tarn.mvc.controller-type` |
| `InterceptSuffix` | `tarn.mvc.intercept-suffix` |
| `templateBaseDirectory` | `tarn.mvc.template-base-directory` |
| `defaultViewType` | `tarn.mvc.default-view-type` |
| `devMode` | `tarn.mvc.dev-mode` |
| `DEFAULT_FREEMARKER_TEMPLATE_UPDATE_DELAY` | `tarn.mvc.freemarker.template-update-delay` |
| `freemarker_stringLoadTemplate_cache` | `tarn.mvc.freemarker.string-template-cache` |
| `isFreeMarkerMultiDir` | `tarn.mvc.freemarker.multi-dir` |
| `ExtendFilter` | `tarn.mvc.extend-filter.enabled` |
| `ExtendFilterImpl` | `tarn.mvc.extend-filter.impl` |
| `isDebug` | `tarn.system.debug` |
| `isSaveSQLLOGDB` | `tarn.system.save-sql-log` |
| `encoding` | `tarn.system.encoding` |
| `runMode` | `tarn.system.run-mode` |
| `ksrType` | `tarn.session.store-type` |
| `varExpires` | `tarn.session.expires` |
| `OpenEhcache` | `tarn.ehcache.enabled` |

> 映射表必须与框架**实际读取的 key** 一致。历史上文档曾误写 `isBug` / `isSavaSQLexecLog`，
> 导致 yml 的 `system.debug` 不生效，2.1 已修正并补齐 FreeMarker / ExtendFilter / dbTypeVersion 等映射。

---

## 5. MVC 与请求链路

### 5.1 请求链路（2.1）

```
HTTP 请求
   │
   ▼
TarnDispatcherFilter#doFilter
   │  1) 设置全局编码
   │  2) 构造 PageContext（HttpServletRequest / HttpServletResponse）
   │  3) ExpandFilterFactory.doFilter → 全局拦截器链（返回 false 则中断）
   │  4) shouldIntercept(vpath)：后缀白名单（无后缀 或 命中白名单）
   ├──► Router.lookup(vpath, method) ── 命中 ──► RouteDispatcher.dispatch
   │                                              └► ExecuteInject（参数注入 + @Json 输出）
   ├──► 未命中 ──────────────────────────────────► 老 RequestInterceptor 链
   ▼
finally: MybatisKit.close()（释放请求级 MyBatis 会话）
```

### 5.2 注解速查（MVC / JSON）

| 注解 | 位置 | 说明 |
| --- | --- | --- |
| `@EController` | 类 | 控制器根路径 |
| `@ERequestMapping` | 方法 | `value` 相对路径；`method` 限定 HTTP 方法；`isPathVariable` / `regex` 开启动态匹配 |
| `@ERequestParam` | 参数 | query / form 参数 |
| `@ERequestBody` | 参数 | JSON 请求体（反序列化为参数类型） |
| `@ERequestHeader` | 参数 | 请求头 |
| `@ECookieValue` | 参数 | Cookie |
| `@EPathVariable` | 参数 | 路径变量 |
| `@Json` | 方法 | 以 JSON 输出返回值 |
| `@EResponseType` | 方法 | 控制响应类型（如 `false` 表示非 JSON） |
| `@EViewType` | 方法/类 | 指定视图类型 |
| `@ViewRootPath` | 类 | 视图根路径 |
| `@Before` | 类/方法 | 方法执行前的拦截器（见 §6.5） |

**硬约束**：控制器方法参数必须带上述参数注解之一，否则框架取不到注解元数据，注入会失败。

### 5.3 JSON 输出

```java
@Json
@ERequestMapping(value = "/list", method = RequestMethod.GET)
public JsonResult list(@ERequestParam("keyword") String keyword) {
    return JsonResult.ok(service.list(keyword));
}
```

输出统一结构：

```json
{"code":200,"message":"success","data":...,"success":true,"timestamp":1793008814476}
```

`JsonResult` 已内置 `CODE_SUCCESS/CODE_ERROR/CODE_UNAUTHORIZED/CODE_FORBIDDEN/CODE_NOT_FOUND`。

### 5.4 视图渲染

`render` 包内按 `default-view-type` 选择 JSP / FreeMarker / Velocity / Json / Text / File / Redirect。
2.1 修复了 `RenderFactory` 读取 FreeMarker 配置时的 `NumberFormatException`（缺失/非法值回退 3600 秒）。

---

## 6. 依赖注入与 AOP 注入体系

### 6.1 两条注入线

| 线 | 触发 | 注解 | 扫描配置 |
| --- | --- | --- | --- |
| 服务注入 | 控制器实例化后 | `@EAutowired`（字段/方法） | `context-mapping.xml` 的 `<services>` |
| 仓储注入 | 服务/仓储实例化后 | `@ERepository` / `@EResource` | `<repositorys>` |

初始化顺序（静态块，框架启动时执行一次）：

```
Inject（扫描 <controllers>，建 ControllerInfo + 路由表）
   └─► new ServiceInject()（扫描 <services>，注册实现）
          └─► 若 <repositorys> 非空 → new RepositoryInject()（扫描 + limitPackage 过滤）
```

### 6.2 注解速查（注入）

| 注解 | 位置 | 属性 | 说明 |
| --- | --- | --- | --- |
| `@EService` | 类 | `value`（服务名） | 服务实现注册；名称要与控制器字段名一致 |
| `@EAutowired` | 字段 | `value`（可空） | 按名称注入；`value` 为空时用字段名 |
| `@ERepository` | 类 | `value`（仓储名） | 仓储实现注册 |
| `@EResource` | 字段 | `value`（可空） | 注入仓储；`value` 为空时用字段名 |
| `@Before` | 类/方法 | `Class<? extends Interceptor>[]` | 控制器/方法级拦截器 |

```java
@EService("bookService")               // 名字 = 控制器里的字段名
public class BookServiceImpl implements BookService { ... }

@EController("/standalone/book")
public class BookController extends Controller {

    @EAutowired
    private BookService bookService;   // 按字段名 bookService 匹配

    public void setBookService(BookService bookService) {  // 有 setter 优先走 setter
        this.bookService = bookService;
    }
}
```

### 6.3 `@Before` 拦截器

```java
public class LogInterceptor implements Interceptor {
    @Override
    public void intercept(ActionInfo ai) {
        // 控制器方法执行前：鉴权、埋点、上下文准备
    }
}

@Before({LogInterceptor.class})
@EController("/admin/user")
public class AdminUserController extends Controller { ... }
```

- 类上的 `@Before` 对该控制器所有方法生效；方法上的 `@Before` 只对该方法生效；
- 拦截器实例**按类缓存**（2.1 修复：原实现每次请求 `new BeforeInject()` 导致缓存失效）；
- 执行顺序与 `receiveBefore` 链一致，抛出异常即中断方法执行。

### 6.4 请求参数与内置对象注入

| 注入目标 | 实现 |
| --- | --- |
| 请求参数 / 路径变量 / 请求体 / Header / Cookie | `MethodParamResolver` 按注解分派 + `CastorService.toType` 类型转换 |
| 控制器字段 | `AttrInject`（请求属性，支持 `user.role.id` 级联路径） |
| `Tarn` / `PageContext` 字段 | `TarnInject`（`Tarn` 新实例 / 当前线程 `PageContext`） |
| Service / Repository | `ComponentInjector`（按注解 + bean 名） |

### 6.5 2.1 注入体系重构（设计说明）

原实现的问题：每个 `*Inject` 各自重复「反射找 setter / 扫包 / 存实现 / 写清单 XML」，职责混杂，
`BeforeInject` 缓存还是实例字段（每次请求失效），`AttrInject` 用 `indexOf` 定位路径段。

2.1 按四层拆开，`aop.inject` 包成为可复用基础设施：

| 层 | 组件 | 职责 |
| --- | --- | --- |
| 属性层 | `BeanProperty` / `BeanProperties` | 字段 + public setter/getter 的属性模型，`Class → List<BeanProperty>` 缓存（跳过 static/synthetic，getter 兼容 `isXxx`） |
| 注册层 | `ComponentRegistry` | 接口名/bean 名 → 实现类名 → 单例 的双 Map；双重检查锁创建；`Creator` 可插拔 |
| 扫描层 | `ComponentScanner` | 统一扫包（jar / IDE 双路径）+ 生成 `framework-mapping-*.xml` 清单 |
| 注入层 | `ComponentInjector` | 遍历属性 → 命中注解 → 取实例 → setter 优先回填；`BeanNameParser` / `CreatedCallback` 可插拔 |

改造后的类职责：

| 类 | 现在只负责 |
| --- | --- |
| `Inject` | 扫 Controller 包、维护 `ControllerInfo` 列表、触发 `ServiceInject` |
| `ServiceInject` | `@EAutowired` + bean 名策略（注解 value 为空回退字段名）+ 服务创建后按需触发仓储注入 |
| `RepositoryInject` | `@ERepository` + `limitPackage` 白名单过滤 |
| `TarnInject` | 内置对象 `Tarn` / `PageContext` |
| `BeforeInject` | 拦截器缓存（静态）+ 类级/方法级收集 + 请求属性回填 |
| `AttrInject` | 请求属性回填，级联路径按下标遍历，父对象回写并缓存 |
| `ExecuteInject` | 纯编排：准备上下文 → 方法拦截器 → 反射执行 → JSON/视图输出 |
| `MethodParamResolver` | 参数注解分派 + 类型转换 + 请求体读取 |

**老 API 全部保留**：`inject(Object)`、`setServiceImpl(String,String)`、`setRepositoryImpl(String,String)`、
`newInstance(String)`、`getControllers()` 等签名与返回类型不变，老项目零改动。
新增 `getRegistry()` 便于诊断。

**扩展方式**：需要新注解注入时，直接复用 `ComponentInjector`：

```java
ComponentRegistry registry = new ComponentRegistry("cache");
ComponentInjector injector = new ComponentInjector(
        ECache.class,                      // 你的注解
        registry,
        new ComponentInjector.BeanNameParser() {
            public String parse(java.lang.annotation.Annotation ann, String fieldName) {
                String v = ((ECache) ann).value();
                return StringUtil.isEmpty(v) ? fieldName : v;
            }
        },
        null);
injector.inject(target);
```

### 6.6 Service / Repository 注册失败排查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `@EAutowired` 注入为 null | 实现类包未登记 `<services>`，或 `@EService` 名称与字段名不一致 | 补包 + 名称对齐 |
| 仓储注入为 null | `<repositorys>` 为空，或实现类被 `limitPackage` 排除 | 检查 `<mapping package>` 与 `limitPackage` |
| 启动日志无 `framework-mapping-services.xml` | 扫包失败（包名错 / jar 未打包该类） | 检查生成的清单文件与包路径 |

---

## 7. 路由系统

### 7.1 两张表

| 表 | 结构 | 适用 | 复杂度 |
| --- | --- | --- | --- |
| 静态路由表 | `Map<String, RouteEntry>` | 完全字面量，如 `/standalone/book/list` | O(1) |
| 动态路由表 | `List<RouteEntry>` + 预编译 `Pattern` | 含 `{id}` / `${mapping}` / 正则 | 按字面量长度降序遍历 |

### 7.2 老注解会自动注册吗

**会，不需要额外配置。** 只要控制器类所在包写进 `context-mapping.xml` 的 `<controllers>`，
`@EController` + `@ERequestMapping` 就会自动注册：

```
完整路由 = @EController.value + @ERequestMapping.value
```

| `@EController` | `@ERequestMapping` | 注册结果 |
| --- | --- | --- |
| `/standalone/book` | `/list` | 静态表 `/standalone/book/list` |
| `/standalone/book` | `/detail/{id}` | 动态表 `/standalone/book/detail/{id}` |
| `/Tarn/${mapping}` | `/${method}` | 不注册，走老拦截器链（老项目兼容） |

### 7.3 动态路由语法

| 模板片段 | 含义 | 编译结果 |
| --- | --- | --- |
| `{id}` | 方法级路径变量 | `@ERequestMapping(regex=...)`，缺省 `[^/]+` |
| `${mapping}` | 控制器级通配（老写法） | `@EController(regex=...)`，缺省 `[^/]+` |
| `{*}` | 贪婪匹配剩余路径 | `.+` |
| 其他字面量 | 普通片段 | `Pattern.quote` 转义 |

整个模板自动加 `^...$`，**regex 里不要再写 `^` / `$`**。

```java
@Json
@ERequestMapping(value = "/delete/{id}", method = RequestMethod.POST,
        isPathVariable = true, regex = "[0-9]+")
public JsonResult delete(@EPathVariable("id") Long id) { ... }
// 实际正则：^/standalone/book/delete/([0-9]+)$
```

多变量：

```java
@ERequestMapping(value = "/user/{uid}/order/{orderNo}", isPathVariable = true, regex = "[0-9]+")
public JsonResult orderDetail(@EPathVariable("uid") Long uid,
                              @EPathVariable("orderNo") Long orderNo) { ... }
// ^/user/([0-9]+)/order/([0-9]+)$
```

### 7.4 匹配顺序与边界

- 静态表先命中；未命中再按「字面量长度降序」尝试动态表；
- 相同字面量的歧义模板（`/user/{id}` 与 `/user/{name}`）避免同时存在；
- 根路径含 `${mapping}` → 不进路由表，老链路行为不变；
- 静态根路径 + 方法级 `{id}` 是 **2.0 新增能力**（老链路历史上不支持该组合）。

### 7.5 404 排查

1. 启动日志有没有 `[Router] 路由表构建完成`？没有 → `<controllers>` 没配；
2. 有没有对应 `[Router] 动态路由 ...` 日志？没有 → 模板写法 / HTTP 方法不匹配；
3. 请求 URL 是否包含 context-path？
4. `regex` 是否过于严格（写了 `^[0-9]+$`）？

---

## 8. 全局拦截

### 8.1 三层拦截模型

| 层 | 执行位置 | 能力 | 配置方式 |
| --- | --- | --- | --- |
| ① Servlet Filter | `TarnDispatcherFilter` **之前**（容器级） | **around**：可统计耗时、包装 response、修改请求 | web.xml / `FilterRegistrationBean` / Jetty `addFilter` |
| ② Tarn 扩展拦截 | `TarnDispatcherFilter` 内部、**路由之前** | 前置处理、短路、写响应、写请求属性 | `TarnConfig.configFilter(Filters)` / `expand-filter.xml` |
| ③ 控制器/方法拦截 | 映射方法执行前 | 业务级鉴权/埋点，能拿到 `ActionInfo` | `@Before(Interceptor[])` |

> ② 只有**前置钩子**：`FilterInterface.doFilter` 返回 `false` 即中断，返回 `true` 后框架继续路由。
> 它不会拿到「下游执行完」的回调，因此**耗时统计、响应包装必须用 ①**。

### 8.2 `FilterInterface` 契约

```java
public interface FilterInterface {
    void init(FilterConfig filterConfig);                                  // 启动执行一次
    boolean doFilter(ServletRequest req, ServletResponse res, FilterChain chain); // false = 中断
    void destroy();                                                        // 容器销毁时
}
```

执行顺序 = 装配顺序；`ExpandFilterFactory.doFilter` 顺序调用，遇到 `false` 立即 `break`。

### 8.3 三种装配方式

**① 编程式（推荐）**：由 `configClass` 指定的 `TarnConfig` 子类装配。

```java
public class GlobalFilterConfig extends TarnConfig {

    @Override
    public void configFilter(Filters filters) {
        filters.add(new CorsFilter())        // 预检直接短路
               .add(new AuthFilter())        // 鉴权失败短路
               .add(new AccessLogFilter());  // 日志 + 链路 ID
    }

    @Override
    public void configPlugin(Plugins me) { }
}
```

**② 声明式**：classpath 下 `expand-filter.xml`（固定拦截器、无需条件判断时用）。

```xml
<filters>
    <expand-impl class="examples.filter.AuthFilter" name="auth"/>
</filters>
```

> 声明式在 `ExpandFilterFactory` 静态块加载，**先于** `configFilter`，因此顺序是「XML 拦截器 → 编程式拦截器」。

**③ 注解式**：只作用于某个控制器 / 方法，见 §6.3。

### 8.4 非 Tomcat 环境怎么接

| 环境 | 做法 | configClass 来源 |
| --- | --- | --- |
| 内嵌 Jetty | `StandaloneLauncher.run(port, contextPath, webappDir, configClass)` 代码注册过滤器 | 方法入参（覆盖 yml） |
| 内嵌 Jetty（yml 方式） | `StandaloneLauncher.run()` | `tarn.server.config-class` |
| Spring Boot | starter 自动注册 `FilterRegistrationBean` | `tarn.spring.config-class` |
| Tomcat | web.xml 的 `TarnDispatcherFilter` | `<init-param>configClass</init-param>` |

三条路线最终都走同一入口：`TarnDispatcherFilter.init → Config.configTarn → tarnConfig.configFilter`，
所以 `FilterInterface` 机制可原样复用，不需要为容器差异写两套拦截器。

**Tomcat web.xml 示例**：

```xml
<filter>
    <filter-name>TarnDispatcherFilter</filter-name>
    <filter-class>cc.ruixing.framework.tarn.core.TarnDispatcherFilter</filter-class>
    <init-param>
        <param-name>configClass</param-name>
        <param-value>examples.filter.GlobalFilterConfig</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>TarnDispatcherFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**Spring Boot 配置**：

```yaml
tarn:
  spring:
    config-class: examples.filter.GlobalFilterConfig
    order: 10
```

**Jetty 显式指定**：

```java
StandaloneLauncher.run(8081, "/", "standalone-web", GlobalFilterConfig.class.getName());
```

### 8.5 可运行示例

| 文件 | 作用 |
| --- | --- |
| `tarn-demo/src/main/java/examples/filter/GlobalFilterConfig.java` | 装配 CORS → Auth → AccessLog |
| `.../CorsFilter.java` | 写 CORS 头；`OPTIONS` 返回 204 并中断 |
| `.../AuthFilter.java` | 校验 `X-Token`，失败返回 401 JSON 并中断 |
| `.../AccessLogFilter.java` | 记录请求行/IP，注入 `X-Trace-Id` 与请求属性 |
| `.../GlobalFilterDemo.java` | 入口，内嵌 Jetty 8081 启动 |

```bash
mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.filter.GlobalFilterDemo
```

实测结果：

| 请求 | 结果 |
| --- | --- |
| `GET /standalone/book/ping`（白名单） | `200` + CORS 头 + `X-Trace-Id` |
| `GET /standalone/book/list`（无 token） | `401` `{"code":401,"message":"缺少或无效的 X-Token"}`，且无 AccessLog 日志（已短路） |
| 同上 + `X-Token: tarn-demo-token` | `200` 返回列表 + `X-Trace-Id` |
| `OPTIONS /standalone/book/list` | `204` + CORS 头，未进路由 |

### 8.6 老 `ExtendFilter` 配置的迁移提示

老 `dev.properties` 的 `ExtendFilter=true` + `ExtendFilterImpl=xxx.CheckLoginFilter` 写法在新链路中**不再实例化单例**：
`TarnDispatcherFilter` 只保留了 `ExtendFilter` 布尔读取（用于 destroy 分支），`ExtendFilterImpl` 仅保留 yml 映射。
迁移做法：把该类直接注册进 `configFilter`：

```yaml
tarn:
  mvc:
    extend-filter:
      enabled: true
      impl: gov.wenzhou.haiguan.admin.CheckLoginFilter
```

```java
@Override
public void configFilter(Filters filters) {
    // 等价于老 ExtendFilterImpl 的单例注册
    filters.add(new gov.wenzhou.haiguan.admin.CheckLoginFilter());
}
```

### 8.7 拦截顺序与常见坑

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 拦截器没执行 | `configClass` 未配置 / 类未实现 `TarnConfig` / 返回 false 但没写响应 | 看启动日志 `configFilter` 输出；补 `configClass` |
| 401 页面空白 | 只 `return false` 没写状态码/响应体 | 写 status + content-type + body 后再返回 false |
| 想统计 Controller 耗时拿不到 | `FilterInterface` 只有前置钩子 | 用 Servlet Filter（order 早于 Tarn 过滤器）包装 |
| 过滤器执行了两次 | web.xml 与代码注册重复（Jetty 场景） | `StandaloneLauncher` 已检测 web.xml，勿重复声明 |
| 静态资源也被 401 | 白名单没覆盖静态后缀 | 拦截器内按后缀/路径白名单放行 |

---

## 9. ORM

### 9.1 双入口、单一实现

```
   新代码（推荐）                    老代码（零改动）
   Orm.template()                   new Q() / new Tarn()
        │                                │
        └──────────┬─────────────────────┘
                   ▼
        OrmTemplate.shared()   ← 唯一实现（组合 ConnectionFactory，不继承任何类）
                   │
     EntitySql / EntityMapper / SqlBuilder / ConnectionFactory
```

- `EntitySql` 只做实体 → 参数化 SQL 的纯函数翻译；
- `OrmTemplate` 负责连接、事务、结果映射；
- `Q` / `DB` / `DAOAbstract` 退化为薄门面，逐方法委托，**不存在两套行为**；
- `DAO` 是查询结果行容器（大小写不敏感 Map），不是访问入口。

### 9.2 实体注解

| 注解 | 目标 | 属性 | 作用 |
| --- | --- | --- | --- |
| `@Etable` | 类 | `value` | 表名 |
| `@Ekey` | 字段 | `key`（默认 `id`）、`isAutoIncrement` | 主键与自增 |
| `@EFilter` | 字段 | `value`（默认 true） | 保存/更新跳过（读得到、写不进） |
| `@EUpdateNull` | 字段 | `value`（默认 true） | 更新时 null 也写入 |
| `@ORMFilter` | 字段 | — | 完全解除映射（读不到也写不进） |
| `@EOrderBy` | 字段 | `name`、`order` | 老查询链路默认排序 |

**命名规则：列名 = 字段名大写，不做驼峰转下划线**（`orderNo` → `ORDERNO`）。
字段建议单段小写（`deptid`），必须驼峰时用 `@ORMFilter` + 列别名。

### 9.3 写 API

| API | 说明 | 返回 |
| --- | --- | --- |
| `save(entity)` | 老 API，新增（自增主键不写入） | void |
| `insert(entity)` | 新增 | 受影响行数 |
| `insertReturnKey(entity)` | 新增并**回填主键到实体** | 主键对象 |
| `updateRows(entity)` | 按主键更新，null 跳过 | 受影响行数 |
| `deleteRows(entity)` | **严格按主键**删除 | 受影响行数 |
| `batchInsert(list)` | 单事务批量，失败整体回滚 | 成功条数 |
| `inTransaction(callback)` | 声明式事务 | 回调返回值 |

行为差异（升级须知）：

| 场景 | 1.x | 2.x |
| --- | --- | --- |
| SQL 失败 | 打印后静默忽略 | 抛 `TarnDataException`（RuntimeException） |
| `delete(entity)` | 用所有非空字段拼 WHERE，可能误删全表 | 严格按主键，主键为空直接拒绝 |
| 自增主键 | 不回填 | `insertReturnKey` 回填 |
| 更新/删除 | 无返回值 | 返回受影响行数 |
| 写 SQL 生成 | 反射拼串 | `EntitySql` 纯函数 + 全参数绑定 |

### 9.4 查询 API

| 方法 | 说明 |
| --- | --- |
| `query(ps, Class)` | 实体列表（列标签忽略大小写匹配字段） |
| `queryOne(ps, Class)` | 单条，无结果返回 null |
| `query(ps, RowMapper)` | 自定义行映射 |
| `queryMaps(ps)` | 任意结果集 → `List<Map>`（JOIN / 聚合） |
| `queryEntityList(sql, params, Class)` | 原生 `?` SQL + 实体映射（支持参数绑定） |
| `count(ps)` | 统计（SQL 需 `SELECT COUNT(0) ...`） |
| `exec(ps)` | INSERT/UPDATE/DELETE |

老 API（`find` / `querys` / `findById` / `dao` / `daos` / `pager`）全部保留。
注意：老 API 多数「失败返回 0/null」，新 API「失败抛异常」。

### 9.5 `SqlBuilder` / `QueryBuilder`

```java
// 单表 + 参数绑定
ParameterizedSql ps = SqlBuilder.select("ID, TITLE, AUTHOR, STOCK")
        .from("demo_book")
        .where("STOCK >= ?", 3)
        .and("TITLE LIKE ?", "%Tarn%")
        .orderBy("ID", true)
        .limit(0, 10)          // 字面量 LIMIT，跨库勿用
        .build();

// 联表 + 方言分页
ParameterizedSql join = QueryBuilder
        .select("u.ID, u.NAME, d.NAME AS DEPTNAME")
        .from("demo_user u")
        .leftJoin("demo_dept d").on("u.DEPTID = d.ID")
        .where("u.STATUS = #{status}").bind("status", 1)
        .orderBy("u.ID", false)
        .page(1, 10)           // 按当前方言生成分页
        .build();
```

两种占位符：

| 占位 | 配套方法 | 特点 |
| --- | --- | --- |
| `?` | `where/and/or/append/having` 可变参数 | 按位置绑定 |
| `#{name}` | `bind(name, value)` | 按名绑定，未绑定抛 `IllegalArgumentException` |

> `SqlBuilder.from()` 只接受单个标识符（白名单）；带别名/JOIN 的 SQL 必须从 `SqlBuilder.create()` 或 `QueryBuilder` 开始。

### 9.6 方言与分页

| 配置值 | 数据库 | 分页 |
| --- | --- | --- |
| `MYSQL` | MySQL / MariaDB | `LIMIT offset, size` |
| `POSTGRESQL` | PostgreSQL | `LIMIT size OFFSET offset` |
| `ORACLE` | Oracle 11g 及以下 | `ROWNUM` 嵌套 |
| `ORACLE12C` | Oracle 12c+ | `OFFSET ... FETCH` |
| `MSSQL` | SQL Server | `ROW_NUMBER()` / `TOP` |
| `DM` | 达梦 DM8 | `LIMIT` / `ROWNUM` 适配 |
| `KINGBASE` | 人大金仓 | `LIMIT ... OFFSET` |
| `GAUSSDB` | openGauss / GaussDB | `LIMIT ... OFFSET` |
| `VASTBASE` | 海量 Vastbase | `LIMIT ... OFFSET` |

未配置 `type` 时按 JDBC URL 自动识别（`jdbc:dm` / `jdbc:kingbase` / `jdbc:opengauss` / `jdbc:vastbase`），
都识别不到兜底 MySQL。自定义方言：实现 `Dialect` 后 `DialectFactory.registerDialect(...)`。

### 9.7 事务

```java
Orm.template().inTransaction(new TransactionCallback<Void>() {
    public Void doInTransaction(OrmTemplate orm) {
        int rows = orm.exec(SqlBuilder
                .create("UPDATE demo_book SET STOCK = STOCK - #{qty}"
                        + " WHERE ID = #{id} AND STOCK >= #{qty}")
                .bind("qty", 2).bind("id", 1L).build());
        if (rows == 0) throw new IllegalStateException("库存不足");  // 自动回滚
        orm.insertReturnKey(order);
        return null;                                                // 正常返回 → 提交
    }
});
```

编程式：`beginTransaction() / commit() / rollback()`；老 API：`transOpen() / transCommit() / transRollback()`。
三条路线共用**同一线程连接**，不要交叉嵌套；MyBatis 事务走 `MybatisKit.openSession(false)`，也不要混用。

### 9.8 复杂字段 JSON

`List` / `Map` / `Set` / 自定义类字段自动 JSON 序列化入库，查询时按声明类型反序列化回原类型（2.1 修复）。
2.1 修复的老 bug：`List<String>` 回读时把 `java.lang.Class` 当元素类型，异常被静默吞掉导致字段恒为 null。

### 9.9 异常体系

| 异常 | 性质 | 场景 |
| --- | --- | --- |
| `TarnDataException` | RuntimeException | 新引擎 / 新 API 的 SQL 失败 |
| `DaoException` | extends **Throwable** | 老 API 签名保留所需；不能只 `catch (Exception)` |
| `IllegalArgumentException` | RuntimeException | 前置校验：无主键删除、非法标识符、`#{name}` 未绑定 |

### 9.10 迁移对照

| 老代码 | 建议替换 |
| --- | --- |
| `exec("update t set a='" + v + "'")` | `exec(SqlBuilder.create("UPDATE t SET A=#{a}").bind("a", v).build())` |
| `find(User.class, "id=" + id)` | `query(SqlBuilder...build(), User.class)` |
| `querys(User.class, sql)` | `queryEntityList(sql, params, User.class)` |
| `pager(sql, size, index)` | `QueryBuilder...page()` |
| 手写多表 SQL | `QueryBuilder.join().on()` |
| `q.save / q.update` | 保持（内部已用元数据缓存） |
| `new Q()` / `new Tarn()` | `Orm.template()`（可选，同一实现） |

---

## 10. 插件机制

### 10.1 双接口并存

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `Plugin`（新） | `name()` / `order()` / `onStart()` / `onStop()` | 推荐新插件实现 |
| `IPlugin`（老） | `start()` / `stop()` / `start(Properties)` | 由 `PluginAdapter` 纳入新生命周期 |

`PluginManager.me().register(...)` 注册，`startAll()` 按 `order` 升序启动，`stopAll()` 逆序停止。
`Config.configTarn` 中的启动顺序：

```
ConnectionFactory.init() → Inject（扫描）→ tarnConfig.configFilter
→ PluginInit.init → configPlugin → startPlugins
→ PluginAutoConfig.configure() → PluginManager.startAll()
→ ExpandFilterFactory.init → initOreillyCos → TarnCacheKit.init
```

### 10.2 声明式装配

```yaml
tarn:
  redis:
    enabled: true
    host: 127.0.0.1
    port: 6379
    password:
    database: 0
    timeout: 2000
    pool:
      max-total: 32
      max-idle: 16
      min-idle: 2
      max-wait-millis: 3000
      test-on-borrow: true
  mybatis:
    enabled: true
    config-location:        # 留空 = 纯编程式构建（复用 Tarn 数据源、开启驼峰）
  ehcache:
    enabled: false
  plugins: []               # 任意 IPlugin / Plugin 实现类全名列表
  #  - com.example.platform.AuditPlugin
```

### 10.3 自定义插件

```java
public class AuditPlugin implements Plugin {
    public String name()   { return "audit"; }
    public int order()     { return 10; }          // 越小越先启动
    public void onStart() throws Exception { /* 初始化 */ }
    public void onStop()  { /* 释放 */ }
}
```

注册二选一：`tarn.plugins: [com.example.platform.AuditPlugin]`（需无参构造）
或 `TarnConfig#configPlugin` 中 `PluginManager.me().register(new AuditPlugin())`。

### 10.4 内置插件

| 插件 | 启用方式 | 说明 |
| --- | --- | --- |
| `DruidPlugin` | 默认（`pool-type: druid`） | 连接池 + 监控页；支持密码解密（§4.3） |
| `MybatisPlugin` | `tarn.mybatis.enabled: true` | 复用 Tarn 数据源；`MybatisKit.getMapper()` 线程会话，请求结束自动关闭 |
| `RedisPlugin` | `tarn.redis.enabled: true` | `RedisKit` 覆盖 String/Hash/List/Set/PubSub；复杂命令取 `getJedis()` |
| `EhCachePlugin` | `tarn.ehcache.enabled: true` | 本地缓存 |
| `ScriptsPlugin` | 代码注册 | 启动执行 SQL 脚本 |
| `C3p0Plugin` | `connectionType=c3p0` | **已废弃**，仅兼容存量，建议迁 Druid |

### 10.5 Redis 用法

```java
RedisKit.set("user:1", "zhangsan", 3600);
String v = RedisKit.get("user:1");
RedisKit.hset("user:profile:1", "name", "张三");
if (RedisKit.setnx("lock:order:88", "1", 30)) {
    try { /* 业务 */ } finally { RedisKit.del("lock:order:88"); }
}
```

未启用时会抛 `IllegalStateException`（含明确提示），不会静默失败。

### 10.6 MyBatis 用法

```java
// 注解式 Mapper（无需 XML）
public interface DemoUserMapper {
    @Select("SELECT ID AS id, NAME AS name, DEPTID AS deptid, STATUS AS status "
          + "FROM demo_user WHERE ID = #{id}")
    DemoUser selectById(@Param("id") Long id);

    @Insert("INSERT INTO demo_user (NAME, DEPTID, STATUS) VALUES (#{name}, #{deptid}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DemoUser user);
}

DemoUserMapper mapper = MybatisKit.getMapper(DemoUserMapper.class);
DemoUser u = mapper.selectById(1L);
```

- 首次 `getMapper` 自动注册接口，无需 `mybatis-config.xml`；
- 会话绑定当前线程，Web 请求结束由 `TarnDispatcherFilter` 自动 `close()`；
- 非 Web 线程（定时任务）用完必须手动 `MybatisKit.close()`。

---

## 11. Spring Boot 集成

### 11.1 原理

starter 只做一件事：把 `TarnDispatcherFilter` 注册成 `FilterRegistrationBean`。
Tarn **未命中的请求会放行**，所以两套 MVC 可共存，迁移可按接口粒度逐步进行。

```
请求 → TarnDispatcherFilter(order=10)
          ├─ 命中 Tarn 路由 → Tarn 控制器输出，结束
          └─ 未命中 → filterChain.doFilter → Spring DispatcherServlet
```

### 11.2 接入三步

1. 依赖：

```xml
<dependency>
    <groupId>cc.ruixing.framework</groupId>
    <artifactId>tarn-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

2. 资源文件：`context-mapping.xml`（必须）+ `application.yml`：

```yaml
tarn:
  spring:
    enabled: true
    url-patterns:
      - /*
    order: 10
    config-class: examples.filter.GlobalFilterConfig   # 需要全局拦截时填
  mvc:
    controller-type: auto
    default-view-type: OTHER
  datasource:
    type: MYSQL
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo
    username: root
    password: "<密文或明文>"
    decrypt: false
```

3. 控制器照旧写 `@EController` + `@Json`；Spring 的 `@RestController` 一行不用改。

### 11.3 配置项

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `tarn.spring.enabled` | `true` | `false` 时完全不注册过滤器 |
| `tarn.spring.url-patterns` | `/*` | 收敛成 `/tarn/*` 可彻底隔离 |
| `tarn.spring.order` | `10` | 数字越小越先执行 |
| `tarn.spring.config-class` | 空 | `TarnConfig` 实现类（全局拦截/插件装配入口） |

### 11.4 限制

| 项 | 说明 |
| --- | --- |
| Boot 版本 | **2.x**（示例 2.7.18）；Boot 3 用 `jakarta.servlet` + JDK 17，**不兼容** |
| JDK | 8 |
| `context-mapping.xml` | 缺失 → 路由表为空，Tarn 接口全 404 |
| 静态资源 | 由 Boot 的 `ResourceHttpRequestHandler` 处理；Tarn 只接管后缀白名单内的请求 |
| 日志 | 框架用 log4j API，经 `log4j-over-slf4j` 桥接到 logback，不引入 log4j 1.x 实现 |
| 过滤器顺序 | 自定义 Servlet Filter 的 order 要小于 `tarn.spring.order` 才能包住 Tarn |

---

## 12. Maven 集成与发布

### 12.1 安装到仓库

```bash
# 全部模块（parent + framework + starter + demo）
mvn clean install -DskipTests

# 只装核心框架
mvn -pl tarn-framework -am clean install -DskipTests
```

安装后 `~/.m2` 会出现：

```
cc/ruixing/framework/tarn-parent/2.0.0/
cc/ruixing/framework/tarn-framework/2.0.0/
cc/ruixing/framework/tarn-spring-boot-starter/2.0.0/
com/oreilly/servlet/cos/05Nov2002/
```

> `cos:05Nov2002` 中央仓库没有，工程内 `libs/` 已兜底，`install` 会一并装入 `~/.m2`。

### 12.2 坐标与依赖

| 构件 | 坐标 | 用途 |
| --- | --- | --- |
| 核心框架 | `cc.ruixing.framework:tarn-framework:2.0.0` | MVC + ORM + 路由 + 连接池 + YAML |
| Boot starter | `cc.ruixing.framework:tarn-spring-boot-starter:2.0.0` | Spring Boot 自动装配 |
| 父 POM | `cc.ruixing.framework:tarn-parent:2.0.0` | 框架工程内部用，业务工程不需要继承 |

已传递引入（无需再声明）：fastjson、druid、snakeyaml、slf4j + logback、dom4j、
commons-fileupload/io/beanutils/codec/collections4、cos、log4j-over-slf4j。

可选（用到才引）：javax.mail、mybatis、jedis + commons-pool2、freemarker、velocity、
ehcache、c3p0（已淘汰）、各数据库驱动、jetty-server/jetty-webapp。

### 12.3 发布私服

```xml
<distributionManagement>
    <repository>
        <id>company-releases</id>
        <url>http://nexus.company.com/repository/maven-releases/</url>
    </repository>
</distributionManagement>
```

```bash
mvn clean deploy -DskipTests
```

业务工程只需声明 `<repositories>`（或公司统一 `<mirrors>`），不必拿到框架源码。

### 12.4 外部工程独立构建

`samples/tarn-maven-demo` 不在父 POM `<modules>` 中：

```bash
mvn -f samples/tarn-maven-demo/pom.xml clean package
```

它模拟「同事电脑上的业务工程」：只依赖坐标、`context-mapping.xml`、`application.yml` 三样东西。

---

## 13. 独立运行（非 Tomcat）

### 13.1 启动方式

```java
StandaloneLauncher.run();                                                   // 读 tarn.server.*
StandaloneLauncher.run(8080, "/", "standalone-web");                         // 显式参数
StandaloneLauncher.run(8081, "/", "standalone-web", "examples.filter.GlobalFilterConfig"); // 显式 configClass
```

| 重载 | 说明 |
| --- | --- |
| `run()` | 读 `tarn.server.port/context-path/webapp-dir/config-class` |
| `run(port, contextPath, webappDir)` | configClass 仍读 yml |
| `run(port, contextPath, webappDir, configClass)` | **2.1 新增**，显式覆盖 yml 的 `config-class` |

启动细节：

- 用代码注册 `TarnDispatcherFilter`（`/*`，REQUEST）；
- 若 webapp 的 `WEB-INF/web.xml` 已声明该过滤器，则跳过代码注册（先剥离注释再判断，避免注释误判）；
- `webapp-dir` 不存在时自动回退查找 `tarn-demo/<webapp-dir>`。

### 13.2 目录与 web.xml

```
standalone-web/
├── WEB-INF/web.xml     # 极简，不需要声明 TarnDispatcherFilter
└── index.html
```

```xml
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee" ...>
    <display-name>tarn standalone demo</display-name>
    <welcome-file-list><welcome-file>index.html</welcome-file></welcome-file-list>
</web-app>
```

### 13.3 运行与打包

```bash
# IDE：运行 StandaloneApp#main（工作目录 tarn-demo）
# Maven：
mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.standalone.StandaloneApp
# 打包：
mvn -q package
java -cp "target/classes:target/lib/*" examples.standalone.StandaloneApp
```

> `mvn exec:java` 不带 `-am` 时会从 `~/.m2` 解析同工程模块，改了框架代码请先 `mvn install`（或加 `-am`）。

---

## 14. 安全与性能

### 14.1 安全加固

| 风险 | 1.x | 2.x |
| --- | --- | --- |
| SQL 注入 | 大量字符串拼接 | `SqlBuilder` / `QueryBuilder` 全参数绑定 |
| 标识符注入 | 表名/排序可拼接 | 标识符白名单校验 |
| 反序列化/XSS | 无统一收口 | `JsonKit`（fastjson safeMode）统一出入 |
| 异常信息泄露 | `printStackTrace` | 统一 `LogUtil`；路由异常转 `ServletException` |
| 连接/会话泄漏 | 手工关闭易遗漏 | 请求 `finally` 自动关闭 MyBatis；Druid 托管 |
| 敏感配置 | 明文密码 | `decrypt` + `public-key` 密文配置；`debug` / `save-sql-log` 默认关闭 |
| 弱依赖 | log4j 1.x、老 dom4j、老 fileupload | logback 1.2.13、dom4j 2.1.4（XXE 修复）、commons-fileupload 1.5、commons-io 2.15.1 |

强制约定：任何用户输入进入 SQL，一律走 `SqlBuilder.bind` 或 Mapper `#{}`，禁止拼接。

### 14.2 性能

| 优化点 | 1.x | 2.x |
| --- | --- | --- |
| 路由查找 | 每请求遍历 + 正则 O(n) | 静态表 O(1)，动态表按字面量排序 |
| 实体反射 | 每次 CRUD 全量解析 | `EntityMapper` 按 Class 缓存 |
| 注入反射 | 每次请求重建拦截器（`BeforeInject` 实例字段） | 静态缓存 + `BeanProperties` 类缓存 |
| 分页 | 硬编码 LIMIT / 游标 | `QueryBuilder.page()` 走方言 |
| 连接池 | c3p0 可选 | Druid 默认 + 监控 |

### 14.3 生产检查清单

- [ ] `tarn.system.debug: false`、`save-sql-log: false`
- [ ] 数据源账号最小权限，密码用 `decrypt: true` 或环境变量，不落仓库
- [ ] 全局异常处理捕获 `TarnDataException`
- [ ] 老拼接 API（`exec(String)` / `find(String)`）逐步替换为 `SqlBuilder`
- [ ] 全局拦截器的白名单覆盖静态资源与健康检查
- [ ] 自定义 Servlet Filter 的 order 早于 Tarn 过滤器

---

## 15. 升级迁移

### 15.1 成本分级

| 档位 | 改动 | 代码改动 | 收益 |
| --- | --- | --- | --- |
| **L0 仅换 jar** | 替换 `WEB-INF/lib` / 改 Maven 版本 | 0 | 安全修复、路由/ORM 缓存性能 |
| **L1 配置迁移** | 新增 `application.yml`（可保留 `dev.properties`） | 0 | 集中配置、声明式插件 |
| **L2 增量新功能** | 新页面用 `Router + @Json + SqlBuilder` | 仅新代码 | 前后端分离、防注入 |
| **L3 深度改造** | 国产库、MyBatis、Redis、独立运行、多表 | 局部 | 技术栈统一 |

推荐：先 L0 上线观察 → L1 → 按业务 L2/L3。

### 15.2 五步迁移

**① 依赖替换**

```xml
<dependency>
    <groupId>cc.ruixing.framework</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>2.0.0</version>
</dependency>
```

非 Maven 工程：替换 jar + 按需补 snakeyaml / druid / mybatis / jedis / jetty。

**② 配置迁移**：按 §4.7 映射表逐项搬到 `application.yml`；未搬的继续读 `dev.properties`。

**③ 控制器包注册**：`context-mapping.xml` 的 `<controllers>` / `<services>` / `<repositorys>`。

**④ 主引导**：Tomcat 继续用 web.xml（不动）；或改 `StandaloneLauncher`（`tarn.server.*`）。

**⑤ 老代码 → 新代码**：见 §9.10；界面/接口可保持老写法，新接口用新 API。

### 15.3 2.1 需要留意的行为变化

| 变化 | 影响 | 应对 |
| --- | --- | --- |
| 写失败抛 `TarnDataException` | 原来「业务以为成功」的路径会抛异常 | 接全局异常处理 |
| `delete(entity)` 严格按主键 | 原来按非空字段批量删的用法不再生效 | 需要批量删请显式写 SQL |
| 复杂字段 JSON 完整往返 | 原来恒为 null 的 `List/Map/Set` 字段开始有值 | 前端/下游确认字段格式 |
| FreeMarker 配置缺失有默认值 | 不再启动失败 | 无需处理 |
| `ExtendFilterImpl` 不再实例化 | 老登录过滤器可能失效 | 改用 `configFilter` 注册（§8.6） |
| 注入体系重构 | 仅内部实现变化，API 不变 | 无需处理 |
| `Application.yml` 的 `decrypt` | 新增可选能力 | 默认 false，不影响存量 |

### 15.4 回滚

- 代码层：2.x 未改老签名，替换回旧 jar 即可；
- 配置层：旧版本不读 `application.yml`，保留 `dev.properties` 即可；
- 数据层：SQL 语义与 1.x 一致（列名 = 字段名大写），无表结构变更。

### 15.5 验收清单

- [ ] 启动日志出现 `[Router] 路由表构建完成`
- [ ] 老页面（`.jspx` / `.do` / 无后缀）访问正常，视图渲染正常
- [ ] 老 `Q` 实体 CRUD 冒烟（增/改/删/查各 1 条）
- [ ] 分页结果与升级前一致
- [ ] 新增一个 `@Json` 接口返回 `{"code":200,...}`
- [ ] `@EAutowired` / `@ERepository` 注入正常
- [ ] 全局拦截器生效（401 / CORS / 日志）
- [ ] 生产 `debug=false`、`save-sql-log=false`
- [ ] ORM 断言：`examples.orm.OrmApiVerification`（46 项全 PASS）、`OrmLegacyApiVerification`（21 项全 PASS）

---

## 16. 故障排查

### 16.1 启动

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `dev.properties not exist` | 使用未修复旧 jar | 升级 2.0.0 并重新打包 |
| `RenderFactory` `NumberFormatException` | FreeMarker 配置缺失/非法（旧版本） | 升级 2.1（安全默认值 3600） |
| `PluginAutoConfig` NPE | `tarn.plugins` 未配置（旧版本） | 升级；或临时 `tarn.plugins: []` |
| `Resource not found: context-mapping.xml`（jar 内） | 旧版本用文件系统路径读取 | 升级（改为 classpath 流） |
| `[Tarn] 数据源密码解密失败` | `decrypt` / `public-key` / 密文不匹配 | 按 §4.3 重新生成 |

### 16.2 路由与 MVC

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| Tarn 接口全 404 | `context-mapping.xml` 缺失/包名错 | 检查资源与 `<mapping package>` |
| 新增接口 404 | 路径含 `${` / 方法不匹配 / 后缀被过滤 | 看 `[Router]` 日志；对齐 `method` |
| Spring 接口被 Tarn 抢走 | `url-patterns` 太宽且路由同名 | 收敛 `url-patterns: /tarn/*` |
| 参数注入 NPE | 方法参数缺注解 | 每个参数加 `@ERequestParam` / `@ERequestBody` / `@EPathVariable` |
| `@EPathVariable` 为 null | 模板与实际路径不匹配 | 用完整模板，交给 Router 拼接 |
| JSON 返回空白 | 方法缺 `@Json` | 加 `@Json` 或返回 `JsonResult` |

### 16.3 ORM

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 字段全为 null | 列名规则是「字段名大写」，或 `SELECT *` 大写标签（旧版本精确匹配） | 升级；或显式列别名 |
| `delete` 删太多 | 1.x 用非空字段拼 WHERE | 升级并使用 `deleteRows(entity)` |
| 写库失败却“成功” | 1.x 吞异常 | 升级并接 `TarnDataException` |
| `DaoException` 编译报未捕获 | `extends Throwable` | 方法声明 `throws DaoException` |
| `count(SqlBuffer)` 返回 0 | `QueryBuffer` 未调用 `countField/countSql`（设计如此） | 补 count 定义 |

### 16.4 全局拦截

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 拦截器未执行 | `configClass` 未配 / 类非 `TarnConfig` | 检查 web.xml / `tarn.server.config-class` / `tarn.spring.config-class` |
| 短路后页面空白 | 未写状态码与响应体 | 写 status + JSON 后再 `return false` |
| 耗时拿不到 | `FilterInterface` 无 around 能力 | 用 Servlet Filter 包在 Tarn 之前 |
| 拦截器重复执行 | web.xml 与代码重复注册 | Jetty 场景交给 `StandaloneLauncher` 注册 |

---

## 17. 附录

### 17.1 注解速查（全量）

| 分类 | 注解 |
| --- | --- |
| MVC | `@EController`、`@ERequestMapping`、`@ERequestParam`、`@ERequestBody`、`@ERequestHeader`、`@ECookieValue`、`@EPathVariable`、`@EResponseType`、`@EViewType`、`@ViewRootPath` |
| JSON | `@Json` |
| 注入 | `@EService`、`@EAutowired`、`@ERepository`、`@EResource`、`@Before` |
| ORM | `@Etable`、`@Ekey`、`@EFilter`、`@EUpdateNull`、`@ORMFilter`、`@EOrderBy`、`@Alias` |

### 17.2 配置速查

| 段 | 关键项 |
| --- | --- |
| `tarn.datasource` | `type`、`pool-type`、`config-file`、`url`、`username`、`password`、`driver-class-name`、`decrypt`、`public-key` |
| `tarn.mvc` | `controller-type`、`intercept-suffix`、`template-base-directory`、`default-view-type`、`dev-mode`、`freemarker.*`、`extend-filter.*` |
| `tarn.system` | `debug`、`save-sql-log`、`encoding`、`run-mode` |
| `tarn.session` | `store-type`、`expires` |
| `tarn.server` | `port`、`context-path`、`webapp-dir`、`config-class` |
| `tarn.spring` | `enabled`、`url-patterns`、`order`、`config-class` |
| `tarn.redis` / `tarn.mybatis` / `tarn.ehcache` / `tarn.plugins` | 插件装配 |

### 17.3 示例代码索引

| 文件 | 说明 |
| --- | --- |
| `tarn-demo/src/main/java/examples/standalone/**` | entity → dao → service → controller 全链路 + 动态路由 |
| `.../examples/demo/**` | 路由 / JSON / ORM / SqlBuilder / MyBatis 综合演示 |
| `.../examples/filter/**` | 全局拦截三件套 + 内嵌 Jetty 入口 |
| `.../examples/orm/OrmApiVerification.java` | 46 项 ORM API 断言 |
| `.../examples/orm/OrmLegacyApiVerification.java` | 21 项老 API 兼容断言 |
| `.../examples/orm/OrmUsageExamples.java` | E1~E8 业务场景（JSON 字段 / 事务 / 联表 / 聚合） |
| `samples/tarn-maven-demo/**` | 外部工程 Maven 引用最小样例 |
| `samples/tarn-springboot-demo/**` | Spring Boot 共存样例 |

### 17.4 验证报告（本机实测）

| 项 | 结果 |
| --- | --- |
| `OrmApiVerification` | PASS=46 / FAIL=0 |
| `OrmLegacyApiVerification` | PASS=21 / FAIL=0 |
| 数据源解密 | 自定义公钥 / 默认公钥 / 明文 / 错误密文 四组符合预期 |
| 全局拦截 | ping 200、无 token 401、带 token 200、OPTIONS 204，日志与响应头正确 |
| Spring Boot 共存 | Tarn 路由 + Spring `@RestController` 同时可用 |
| 完整构建 | `mvn -DskipTests install` 通过 |

```bash
mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.orm.OrmApiVerification
mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.orm.OrmLegacyApiVerification
mvn -pl tarn-demo exec:java -Dexec.mainClass=examples.filter.GlobalFilterDemo
```

### 17.5 变更记录

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| 2.0.0 | 2026-10 | 路由 / JSON / 参数化 SQL / 方言 / YAML / 插件体系 / 独立运行 |
| 2.1 | 2026-10 | ORM 组合式引擎（`OrmTemplate`）、老 ORM 链全量委托、`TarnDataException`、复杂字段 JSON 往返、注入体系重构（`aop.inject` 五组件）、数据源密码解密（`decrypt` / `public-key`）、全局拦截规范与示例 |
