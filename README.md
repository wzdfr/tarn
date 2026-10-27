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

## 许可证

本项目为内部框架，仅供授权项目使用。
