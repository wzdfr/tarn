# Tarn Framework 2.0 项目方案白皮书与升级迁移指南

> 适用版本：`cc.ruixing.framework:tarn-framework:2.0.0`（2.1 为能力代号，`Tarn.version()` 仍返回 `2.0.0`）
> 目标读者：平台架构师、老项目维护负责人、新业务开发
> 核心承诺：**存量项目替换 jar 即可运行，零代码改动；新能力按需增量启用。**
> **合并版总手册（推荐首选）**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md) —— 已把本白皮书与路由 / ORM / Maven / Spring Boot 文档合并，并补充 2.1 的数据源密码加密、AOP 注入体系重构、全局拦截方案。

---

## 1. 背景与目标

Tarn 是自研的轻量级 Java MVC / ORM 框架（`javax.servlet` 3.1、JDK 8），大量存量业务系统直接依赖它：

- 控制器通过 `@EController` + `config` 中的 `controller-type`（param / method / mapping / auto）调度；
- 数据访问通过 `Q` / `DB` / `DAOAbstract` 的实体方法（`save` / `update` / `delete` / `querys`）完成；
- 配置走 `dev.properties`，主引导是 `web.xml` 中的 `TarnDispatcherFilter`。

这些系统仍在生产运行，**不能推倒重写**。Tarn 2.0 的目标是：

| 目标 | 落地方式 |
| --- | --- |
| 老项目零改动升级 | 全部老类、老方法、老注解、老配置 key 原样保留，内部委托新实现 |
| 修复历史质量缺陷 | 去掉静默吞异常、修掉反射全量扫描、修掉会话泄漏、补参数化 API |
| 支持前后端分离 | 新增 `@Json` + `JsonResult` + `JsonKit`（统一收口 fastjson） |
| 支持主流工程化 | Maven 管理、YAML 主配置（Spring Boot 风格）、声明式插件装配 |
| 数据层现代化 | `EntityMapper` 元数据缓存、`SqlBuilder` 参数化、`QueryBuilder` 多表 JOIN、8 种方言（含国产库） |
| 脱离 Tomcat | 内嵌 Jetty 的 `StandaloneLauncher` |
| 生态扩展 | `Plugin` 新接口 + `PluginAdapter` 兼容老 `IPlugin`，内置 Redis / MyBatis / Ehcache / Druid / SQL 脚本插件 |

---

## 2. 架构总览

### 2.1 分层结构

```
com.ruixing.framework.tarn
├── core              框架内核：TarnDispatcherFilter / Config / Tarn / PageContext / Model / Pager
│   ├── connection    连接池抽象与事务（ConnectionFactory / ConnectionInit / TarnDataSource）
│   ├── sql           参数化 SQL 构建：SqlBuilder / QueryBuilder / ParameterizedSql
│   └── castor        类型转换（含请求体 JSON 反序列化）
├── route             路由层：Router（静态表 O(1)）/ RouteEntry / RouteDispatcher
├── orm               组合式 ORM 引擎：OrmTemplate（单一实现，组合 ConnectionFactory）/ Orm（工厂入口）
│   │                 TransactionCallback（声明式事务）/ EntitySql（实体→参数化 SQL）/ EntityMapper（注解解析 + 缓存）
│   └── dialect       方言：MySQL / PostgreSQL / Oracle / Oracle12c / SQLServer / DM / Kingbase / GaussDB / Vastbase
├── aop               注入与调度：Inject / ControllerAOP / ExecuteInject / ServiceInject / RepositoryInject
│   ├── inject        2.1 注入公共组件：BeanProperty(s) / ComponentRegistry / ComponentScanner / ComponentInjector
│   └── annotation    注解体系（EController / ERequestMapping / Etable / Ekey / EFilter / ORMFilter / Json ...）
├── json              JsonKit / JsonEngine / FastJsonEngine / JsonResult
├── render            视图渲染：JSP / FreeMarker / Velocity / Json / Text / File / Redirect / Error
├── plugin            插件体系：Plugin / IPlugin / PluginManager / PluginAdapter / PluginAutoConfig
│   ├── redis         RedisPlugin / RedisKit
│   ├── mybatis       MybatisPlugin / MybatisKit
│   ├── druid         DruidPlugin（默认连接池）
│   ├── ehcache       Ehcache 插件
│   ├── sqlScript     启动 SQL 脚本
│   └── orm           DbPlugin / SqlBuffer / QueryBuffer
├── config            YamlConfigKit（application.yml 扁平化 + 老 key 桥接）
├── standalone        StandaloneLauncher（内嵌 Jetty）
├── collection / util / validate / upload / token / supports
```

### 2.2 请求链路（2.0）

```
HTTP 请求
   │
   ▼
TarnDispatcherFilter#doFilter
   │  1) 设置编码 / PageContext / 扩展 Filter
   │  2) shouldIntercept(vpath)：拦后缀白名单（jspx|jsp|do|u|shtml|rx 或无后缀）
   ├──► Router.lookup(vpath, method)  ── 命中 ──► RouteDispatcher.dispatch
   │                                               └► ExecuteInject.handle（参数注入 + @Json 输出）
   ├──► 未命中（动态路径 / ${mapping} / {id} / regex）──► 老 RequestInterceptor 链（行为不变）
   │
   ▼
finally: MybatisKit.close()（释放请求级 MyBatis 会话）
```

**路由判定规则（务必理解）**

- 路径中**不含** `${` 与 `{` 的控制器/方法映射 → 进**静态路由表**，命中 O(1)；
- 方法映射含 `{id}` 等路径变量 → 编译为**动态路由**（`Pattern`），静态表未命中时按“字面量长度降序”依次匹配；
- 控制器根路径本身含 `${mapping}`（老写法）→ 不进路由表，仍走老拦截器链（老项目零感知）；
- `@ERequestMapping(method=...)` 与请求方法不匹配时不命中 → 回退老链路/404，不报错。

> 动态路由的完整语法、正则设置与匹配顺序见 [Tarn-2.0-路由与独立运行使用指南.md](./Tarn-2.0-路由与独立运行使用指南.md)。

---

### 2.3 工程结构（多模块）

```
tarn-framework-2026-beta/
├── pom.xml                     父 POM（统一版本、依赖管理、插件管理）
├── docs/                       全书 / 说明书 / 白皮书 / ORM 指南 / 路由指南 / Maven 指南 / Spring Boot 指南
├── libs/                       工程内本地仓库（中央仓库缺失的历史 jar，如 cos）
├── tarn-framework/              框架核心（jar：tarn-framework-2.0.0.jar）
│   └── src/main/java/cc/ruixing/framework/tarn/**
├── tarn-spring-boot-starter/    Spring Boot 自动装配（可选）
├── samples/                    外部工程示例：tarn-maven-demo / tarn-springboot-demo（不在父 POM modules）
└── tarn-demo/                   示例工程（可单独运行，不属于框架发布物）
    ├── src/main/java/examples/  demo / test / standalone / orm / filter 示例
    ├── src/main/resources/      application.yml / dev.properties / context-mapping.xml
    ├── standalone-web/          内嵌 Jetty 用的 webapp（WEB-INF + index.html）
    ├── web/                     传统 Tomcat 部署演示（遗留）
    └── rely-libs/               历史 jar 备份
```

构建与运行：

```bash
mvn -DskipTests package            # 父 POM 聚合构建，产出 tarn-framework-2.0.0.jar
cd tarn-demo && mvn -q package      # 顺带把运行时依赖复制到 target/lib
java -cp "target/classes:target/lib/*" examples.standalone.StandaloneApp
```

---

## 3. 2.0 新能力清单

| # | 能力 | 入口 | 是否影响老代码 |
| --- | --- | --- | --- |
| 1 | 静态路由表 | `Router` / `RouteEntry` / `RouteDispatcher` | 否（老链路保留） |
| 1.1 | 动态路由（`{id}` / 自定义正则） | `Router.compile` / `RouteEntry.pattern` | 否（新增能力） |
| 2 | 前后端分离 JSON | `@Json`、`JsonResult`、`JsonKit` | 否 |
| 3 | 参数化 SQL | `SqlBuilder`、`QueryBuilder`、`ParameterizedSql` | 否（新增 API） |
| 4 | ORM 元数据缓存 | `EntityMapper` | 否（内部替换反射） |
| 5 | 多表关联查询 | `QueryBuilder.leftJoin(...).on(...)` + `queryMaps` / `query(ps, Class)` | 否 |
| 6 | 8 种数据库方言 | `DialectFactory`、`Dialect` | 否（自动识别，可配置） |
| 7 | YAML 配置 | `application.yml` + `YamlConfigKit` | 否（旧 key 自动桥接） |
| 8 | Maven 构建 | `pom.xml` | 否 |
| 9 | 声明式插件装配 | `tarn.redis.*` / `tarn.mybatis.*` / `tarn.plugins` | 否 |
| 10 | Redis 插件 | `RedisPlugin` / `RedisKit` | 否 |
| 11 | MyBatis 集成 | `MybatisPlugin` / `MybatisKit`（线程会话 + 请求级自动关闭） | 否 |
| 12 | 独立运行（非 Tomcat） | `StandaloneLauncher`（Jetty 9.4） | 否 |
| 13 | 新插件接口 | `Plugin` / `PluginManager` / `PluginAdapter` | 否（老 `IPlugin` 自动适配） |
| 14 | 默认连接池收敛 | Druid 为唯一内置推荐；c3p0 分支保留但已废弃 | 否（配置不变仍可用） |

**2.1 追加（ORM 架构重构与健壮性）**

| # | 能力 | 入口 | 是否影响老代码 |
| --- | --- | --- | --- |
| 15 | 组合式 ORM 引擎（工厂模式，替代 4 层 `extends`） | `Orm.template()` → `OrmTemplate`（单一实现，老链全部委托它） | 否（老 `Q`/`Tarn` 签名与行为不变） |
| 16 | 声明式事务 | `OrmTemplate.inTransaction(TransactionCallback)` | 否（新增 API） |
| 17 | 写失败统一非受检异常 | `TarnDataException`（替代静默吞异常） | 否（仅失败路径行为变化，见 ORM 指南 §3.7） |
| 18 | 实体写 SQL 纯函数化 | `EntitySql`（自增主键不写入、UPDATE 只认主键、DELETE 防误删全表） | 否（语义修正见 ORM 指南） |
| 19 | 复杂字段 JSON 完整往返 | `List`/`Map`/`Set`/自定义类 自动序列化 + 反序列化（修复老实现 `getClass()` 泛型 bug） | 否（老数据格式不变） |
| 20 | 主入口健壮性整治 | `Tarn` 2.1（线程安全、配置读取兜底、`version()`、带默认值配置 API） | 否 |
| 21 | 数据源密码加密 | `tarn.datasource.decrypt: true` + `public-key`，建池前用 Druid `ConfigTools` 解密 | 否（默认 `false`） |
| 22 | AOP 注入体系重构 | `aop.inject`：`BeanProperty` / `BeanProperties` / `ComponentRegistry` / `ComponentScanner` / `ComponentInjector`；原注入类仅保留配置 | 否（老注入 API 签名不变） |
| 23 | 全局拦截规范 | `TarnConfig.configFilter(Filters)` + `FilterInterface`（路由前短路）+ `@Before`（方法级），另有 `expand-filter.xml`；非 Tomcat 用 `configClass` 装配 | 否（老 `FilterInterface`/`Filters` 机制原样保留） |

---

## 4. 兼容性设计原则

1. **老方法保留，内部委托新实现**
   例：`Q.save(Object)` 仍存在，内部走 `EntityMapper` 缓存的元数据生成 SQL；`DAOAbstract.exec(String)` 保留，新代码用 `exec(ParameterizedSql)`。
2. **老注解语义一字不改**
   `@Etable` / `@Ekey` / `@EFilter` / `@ORMFilter` / `@EUpdateNull` / `@EOrderBy` 的判定规则与 1.x 完全一致（列名仍为“字段名大写”）。
3. **老配置 key 全部可读**
   `dev.properties` 中的 `DBType` / `controller-type` / `isDebug` 等无需改名；若同时存在 `application.yml`，YAML 优先（见 §5.3 映射表）。
4. **老插件不受影响**
   `IPlugin` 接口与 `PluginInit` 启动流程保留；新插件走 `PluginManager`，两者并行启动。
5. **行为可回退**
   不想用静态路由/JSON/YAML 时，不写相关注解与配置即可回到 1.x 行为；回滚只需换回旧 jar。

---

## 5. 升级迁移指南（老项目如何低成本接入）

### 5.1 迁移成本分级

| 档位 | 改动内容 | 代码改动 | 典型耗时 | 收益 |
| --- | --- | --- | --- | --- |
| **L0 仅换 jar** | `WEB-INF/lib` 替换为 `tarn-framework-2.0.0.jar`（及新增依赖） | 0 | 分钟级 | 安全修复、性能提升（路由/ORM 缓存） |
| **L1 配置迁移** | 新增 `application.yml`（可保留 `dev.properties`） | 0 | 0.5 天 | 集中配置、插件声明式装配 |
| **L2 增量新功能** | 新页面用 `Router + @Json + SqlBuilder`，老页面不动 | 仅新代码 | 按需求 | 前后端分离、防注入 |
| **L3 深度改造** | 国产库、MyBatis、Redis、独立运行、ORM 多表 | 局部 | 按模块 | 技术栈统一 |

> 推荐路线：先 **L0** 上线观察，再 **L1**，最后按业务 **L2/L3**。任何阶段都不需要一次性大改。

### 5.2 步骤一：依赖替换

Maven 工程：

```xml
<dependency>
    <groupId>cc.ruixing.framework</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>2.0.0</version>
</dependency>
```

非 Maven 的老 Web 工程（`WEB-INF/lib` 方式）：

1. 删除旧框架 jar（与 `tarn-framework-2.0.0.jar` 同名/旧版本均可）；
2. 放入 `tarn-framework-2.0.0.jar`；
3. 补齐新增的运行时依赖（如果尚未存在）：
   - `snakeyaml-2.2.jar`（YAML 配置，纯老模式可不加）
   - `druid-1.2.23.jar`（推荐连接池）
   - `mybatis-3.5.17.jar`（仅启用 MyBatis 插件时）
   - `jedis-3.8.0.jar` + `commons-pool2-2.11.1.jar`（仅启用 Redis 时）
   - `jetty-server` / `jetty-webapp-9.4.46`（仅独立运行时）

> **兼容提示**：`web.xml`、`context-mapping.xml`、`dev.properties` 均无需改动即可先跑起来。

### 5.3 步骤二：配置迁移（`dev.properties` → `application.yml`）

优先级：**`application.yml` > `dev.properties`**。未迁移的 key 继续从 `dev.properties` 读取，因此可以逐项迁移。

老 key ↔ 新 YAML key 映射（`YamlConfigKit` 内置）：

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

> **2.1 修正说明**：
> 1. 上表中 `isDebug` / `isSaveSQLLOGDB` 是框架实际读取的 key（`DB.java` 静态字段初始化），此前文档误写为 `isBug` / `isSavaSQLexecLog`，导致 yml 中 `tarn.system.debug` 配置不生效，已修正。
> 2. FreeMarker 三项与扩展 Filter 两项为此前遗漏的映射——老项目只迁移 `application.yml`（删除 `dev.properties`）时，缺失 FreeMarker 配置会导致启动失败，2.1 已补齐映射并增加安全默认值（`template-update-delay` 缺省 3600 秒，配置非法自动回退，不再抛 `NumberFormatException`）。

典型 `application.yml`（最小可用）：

```yaml
tarn:
  datasource:
    type: MYSQL            # MYSQL/ORACLE/MSSQL/POSTGRESQL/DM/KINGBASE/GAUSSDB/VASTBASE/ORACLE12C
    pool-type: druid
    config-file: druid.properties
    decrypt: false         # 2.1：true 时启动阶段解密 password（默认 false，老项目无感知）
    # public-key: "<ConfigTools 生成的公钥>"   # 留空用 Druid 内置默认公钥
  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    template-base-directory: /WEB-INF/views
    default-view-type: FREE_MARKER
    dev-mode: false
    # FreeMarker（对应老 dev.properties 的 DEFAULT_FREEMARKER_TEMPLATE_UPDATE_DELAY /
    # freemarker_stringLoadTemplate_cache；不配置也有安全默认值，不会启动失败）
    freemarker:
      template-update-delay: 3600    # 模板更新检测间隔（秒），仅 dev-mode: false 时生效
      string-template-cache: 60      # 字符串模板缓存刷新间隔（秒），缺省 300
  system:
    debug: false            # 生产必须 false
    save-sql-log: false     # 写库日志，生产关闭
    encoding: utf-8
    run-mode: jar
```

要点：

- `tarn.datasource.url/username/password` 配置后优先于 `config-file`；密码建议用环境变量占位，避免明文入库；
- **数据源密码加密（2.1）**：`decrypt: true` 时框架在建池前用 Druid `ConfigTools.decrypt` 解密 `password`，
  支持自定义 `public-key`（留空用内置默认公钥），解密失败直接抛 `TarnException` 阻断启动；
  密钥约定为 `genKeyPair(512)` 返回 `[私钥, 公钥]`，**私钥加密、公钥解密**；
- `type` 未配置时按 JDBC URL 自动识别（`jdbc:dm` / `jdbc:kingbase` / `jdbc:opengauss` 等）；
- 关键配置缺失时框架有安全默认值（如 FreeMarker `template-update-delay` 缺省 3600 秒），只迁移 yml、删除 `dev.properties` 也不会启动失败。

### 5.4 步骤三：控制器包注册（`context-mapping.xml`）

```xml
<framework>
    <controllers>
        <mapping package="examples.test.controller" name="基础模块控制"/>
        <mapping package="examples.demo.controller" name="Tarn 2.0 示例"/>   <!-- 新增 -->
    </controllers>
    <services>
        <mapping package="examples.test.services.impl" name="服务实现"/>
    </services>
    <repositorys/>
</framework>
```

注意：

- `controllers` 决定 `Inject` 扫描范围，路由表也从这里构建；
- `services` 是 `@EAutowired` 按字段名注入的实现包（`ServiceInject` 依赖该配置）；
- 新模块建议**新建独立包**并在该文件追加一行，不影响老模块。

### 5.5 步骤四：主引导（二选一）

**A. 继续用 Tomcat（推荐老项目）**：`web.xml` 完全不动。

```xml
<filter>
    <filter-name>tarn</filter-name>
    <filter-class>cc.ruixing.framework.tarn.core.TarnDispatcherFilter</filter-class>
    <init-param>
        <param-name>configClass</param-name>
        <param-value>examples.test.AppConfig</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>tarn</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**B. 脱离 Tomcat（内嵌 Jetty）**：新增一个启动类，配置写在 YAML。

```yaml
tarn:
  server:
    port: 8080
    context-path: /
    webapp-dir: web              # 含 WEB-INF 的目录
    config-class: examples.test.AppConfig
```

```java
public static void main(String[] args) {
    // 三参：configClass 取 tarn.server.config-class
    cc.ruixing.framework.tarn.standalone.StandaloneLauncher.run();

    // 或四参：直接指定装配类（优先级高于 YAML），用于测试 / 多环境
    // cc.ruixing.framework.tarn.standalone.StandaloneLauncher.run(8081, "/", "web", "examples.filter.GlobalFilterConfig");
}
```

### 5.6 步骤五：老代码 → 新代码对照

| 场景 | 老写法（保持可用） | 新写法（推荐增量使用） |
| --- | --- | --- |
| 保存 | `q.save(user)` / `q.saveRK(user)` | `q.save(user)`（内部已用 `EntityMapper` 缓存） |
| 更新 | `q.update(user)` | 同上，注意 `@EUpdateNull(true)` 字段 null 也写 |
| 查询单条 | `q.findById(User.class, cond)`、`q.find(cond)` | `q.query(ps, User.class)` 或 MyBatis Mapper |
| 查询列表 | `q.querys(cond)` | `q.queryEntityList(sql, params, User.class)` |
| 手写 SQL | `q.exec("delete from t where id=" + id)` ❌ 注入风险 | `q.exec(SqlBuilder.create("DELETE FROM t WHERE ID = #{id}").bind("id", id).build())` |
| 多表 | 拼字符串 | `QueryBuilder.select(...).from(...).leftJoin(...).on(...).page(...)` |
| 输出 JSON | `response.getWriter().write(json)` | 方法加 `@Json`，返回 `JsonResult` / POJO / Map |
| ORM 入口 | `new Q()` / `new Tarn()`（继承链） | `Orm.template()`（组合式引擎，同一实现，可选替换） |
| 编程式事务 | `q.transOpen() / transCommit() / transRollback()` | `Orm.template().inTransaction(callback)`（声明式，推荐） |
| 控制器路径 | `${mapping}` 动态匹配 | `@EController("/api/x")` + `@ERequestMapping("/y")` 静态路由 |
| 路径变量 | `${mapping}` + 拦截器截取 | `@ERequestMapping("/delete/{id}", isPathVariable=true, regex="[0-9]+")` + `@EPathVariable("id")` |
| 连接池 | `connectionType=c3p0` | `pool-type: druid` |

### 5.7 回滚方案

- 代码层：2.0 未修改任何老签名，回滚只需替换回旧 jar；
- 配置层：`application.yml` 不被旧版本读取，保留 `dev.properties` 即可；
- 数据层：`EntityMapper` 与参数化 API 生成 SQL 与 1.x 语义一致（列名为字段名大写），无表结构变更。

### 5.8 升级验收清单

- [ ] 应用可正常启动，日志出现 `[Router] 静态路由表构建完成`
- [ ] 老页面（`.jspx` / `.do` / 无后缀）访问正常，视图渲染正常
- [ ] 老 `Q` 实体 CRUD 冒烟通过（新增/更新/删除/查询各 1 条）
- [ ] 分页（`Pager`）结果与升级前一致
- [ ] 新增一个 `@Json` 接口返回 `{"code":200,...}`
- [ ] 生产配置 `tarn.system.debug=false`、`save-sql-log=false`
- [ ] 数据源监控（如启用 Druid）可访问，无连接泄漏告警
- [ ] 运行 `mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmApiVerification`（46 项 ORM 断言全 PASS，见 ORM 指南 §11.3）

### 5.9 可选步骤：全局拦截（老 `FilterInterface` + `Filters` 机制保留）

2.1 对全局拦截给出统一规范，**老机制原样保留，不需要改造**：

| 层 | 机制 | 时机 | 能做什么 |
| --- | --- | --- | --- |
| ① | 标准 `javax.servlet.Filter`（排在 `TarnDispatcherFilter` 之前） | 请求最外层 | 环绕 / 后置 / 包装 response / 耗时统计 |
| ② | `FilterInterface` + `Filters`（`TarnConfig.configFilter(...)`、`expand-filter.xml`） | 路由匹配之前 | 鉴权、CORS、日志；`return false` 短路（**无后置钩子**） |
| ③ | `@Before(Interceptor[])` | 控制器方法执行前 | 方法级拦截 |

装配入口 `configClass` 三来源（Tomcat / Jetty / Spring Boot 各不相同）：

```java
public class AppConfig implements TarnConfig {
    @Override
    public void configFilter(Filters filters) {
        filters.add(new CorsFilter()).add(new AuthFilter()).add(new AccessLogFilter());
    }
}
```

| 运行方式 | 指定 `configClass` |
| --- | --- |
| Tomcat | `web.xml` 的 `<init-param>` `configClass` |
| 内嵌 Jetty | `StandaloneLauncher.run(port, "/", "web", "AppConfig")` 或 `tarn.server.config-class` |
| Spring Boot | `tarn.spring.config-class` |

> 完整的三层模型、`expand-filter.xml` 优先级、`ExtendFilterImpl` 在 2.x 不再实例化的说明、以及
> 可运行的 CORS / 鉴权 / 访问日志示例（`examples/filter/**`）见《全书》§8 与《路由与独立运行指南》§7。

---

## 6. 运行模式

| 模式 | 说明 | 关键配置 |
| --- | --- | --- |
| Tomcat / 传统容器 | `web.xml` 注册过滤器，行为与 1.x 一致 | `configClass` |
| 内嵌 Jetty | `StandaloneLauncher.run()`，`StandaloneLauncher` 自动把 `TarnDispatcherFilter` 挂到 `/*` | `tarn.server.*` |
| 开发态（IDE） | `runMode` 留空或非 `jar`，模板热加载 | `tarn.mvc.dev-mode: true` |

> `StandaloneLauncher` 在 `pom.xml` 中对应 Jetty 依赖为 `optional`，只有独立运行的项目才需要显式引入。

---

## 7. 插件体系

### 7.1 双接口并存

- **新接口** `Plugin`：`name()` / `order()` / `onStart()` / `onStop()`；
- **老接口** `IPlugin`：`start()` / `stop()` / `start(Properties)`，由 `PluginAdapter`（`order=100`）自动纳入新生命周期；
- `PluginManager.me().register(...)` 注册，`startAll()` 按 `order` 升序启动，`stopAll()` 逆序停止。

### 7.2 声明式装配（YAML）

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
    config-location:          # 留空 = 纯编程式构建（复用 Tarn 数据源、开启驼峰）
  plugins:
    - com.example.platform.AuditPlugin
```

### 7.3 自定义插件（新接口）

```java
package com.example.platform;

import cc.ruixing.framework.tarn.plugin.Plugin;

public class AuditPlugin implements Plugin {
    @Override
    public String name() {
        return "audit";
    }

    @Override
    public int order() {
        return 10;                       // 越小越先启动
    }

    @Override
    public void onStart() throws Exception {
        // 初始化线程池 / 注册定时任务等
    }

    @Override
    public void onStop() {
        // 释放资源
    }
}
```

注册方式二选一：

1. YAML：`tarn.plugins: [com.example.platform.AuditPlugin]`（需无参构造）；
2. 代码：在 `TarnConfig#configPlugin` 中 `PluginManager.me().register(new AuditPlugin())`。

### 7.4 内置插件

| 插件 | 启用 | 说明 |
| --- | --- | --- |
| `DruidPlugin` | 默认（`pool-type: druid`） | 连接池 + 监控页 |
| `MybatisPlugin` | `tarn.mybatis.enabled: true` | 复用 Tarn 数据源；`MybatisKit.getMapper()` 线程会话，请求结束自动关闭 |
| `RedisPlugin` | `tarn.redis.enabled: true` | `RedisKit` 覆盖 String/Hash/List/Set/PubSub；复杂命令取 `getJedis()` |
| `EhCachePlugin` | `tarn.ehcache.enabled: true` | 本地缓存 |
| `ScriptsPlugin` | 代码注册 | 启动执行 SQL 脚本 |
| `C3p0Plugin` | `connectionType=c3p0` | **已废弃**，仅兼容存量，建议迁移 Druid |

### 7.5 Redis 使用示例

```java
// 启用后即可直接使用（未启用会抛 IllegalStateException，含明确提示）
RedisKit.set("user:1", "zhangsan", 3600);
String v = RedisKit.get("user:1");
RedisKit.hset("user:profile:1", "name", "张三");
if (RedisKit.setnx("lock:order:88", "1", 30)) {
    try { /* 业务 */ } finally { RedisKit.del("lock:order:88"); }
}
```

### 7.6 MyBatis 使用示例

```java
// 1) 注解式 Mapper（推荐，无需 XML）
@Select("SELECT ID, NAME, DEPTID, STATUS FROM demo_user WHERE ID = #{id}")
DemoUser selectById(@Param("id") Long id);

// 2) 获取代理（绑定当前请求线程会话）
DemoUserMapper mapper = MybatisKit.getMapper(DemoUserMapper.class);
DemoUser user = mapper.selectById(1L);

// 3) 跨请求事务：手动会话
SqlSession session = MybatisKit.openSession(false);
try {
    session.getMapper(DemoUserMapper.class).insert(user);
    session.commit();
} catch (Exception e) {
    session.rollback();
    throw e;
} finally {
    session.close();
}
```

> 非 Web 线程（定时任务）使用 `getMapper` 后必须自行 `MybatisKit.close()`。

---

## 8. 安全加固清单

| 风险 | 1.x 状况 | 2.0 措施 |
| --- | --- | --- |
| SQL 注入 | 大量字符串拼接（`exec` / `find` / `pager`） | 新增 `SqlBuilder` / `QueryBuilder` / `ParameterizedSql`，值全部 `PreparedStatement` 绑定 |
| SQL 标识符注入 | 表名/排序字段可拼接 | `SqlBuilder` 标识符白名单；`QueryBuilder` 表引用与 ON 条件白名单 |
| 反射/XSS | 无统一收口 | `JsonKit`（fastjson safeMode）统一序列化/反序列化 |
| 异常信息泄露 | `printStackTrace` 直出 | 统一 `LogUtil`；路由异常转 `ServletException` |
| 连接/会话泄漏 | 手工关闭易遗漏 | 请求 `finally` 自动关闭 MyBatis 会话；Druid 托管连接 |
| 弱依赖 | log4j 1.x、老 dom4j、老 fileupload | logback 1.2.13、dom4j 2.1.4（XXE 修复）、commons-fileupload 1.5、commons-io 2.15.1 |
| 敏感配置 | 明文密码 | 支持环境变量占位；`debug` / `save-sql-log` 默认关闭 |
| 调试信息 | `isDebug=true` 输出 SQL | 生产必须 `tarn.system.debug: false` |

**新代码强制约定**：任何用户输入进入 SQL 的地方，一律走 `SqlBuilder.bind` 或 Mapper `#{}`，禁止字符串拼接。

---

## 9. 性能优化说明

| 优化点 | 1.x | 2.0 |
| --- | --- | --- |
| 路由查找 | 每请求遍历全部 Controller + 正则匹配 O(n) | 启动构建哈希表，命中 O(1)，未命中回退老链路 |
| 实体反射 | 每次 CRUD 全量解析字段/注解 | `EntityMapper` 按 Class 缓存（`ConcurrentHashMap`），只解析一次 |
| 分页 SQL | 硬编码 `LIMIT` / `rs.absolute()` 游标 | `QueryBuilder.page()` 走方言生成；老 `pager` 保留 |
| 连接池 | c3p0 可选 | Druid 默认，支持监控与慢 SQL |
| 日志 | log4j 1.x | slf4j + logback（异步可选） |

---

## 10. 附录

### 10.1 注解速查（MVC / JSON）

| 注解 | 位置 | 说明 |
| --- | --- | --- |
| `@EController` | 类 | 控制器根路径；含 `${`/`{` 时走动态链 |
| `@ERequestMapping` | 方法 | `value` 相对路径；`method` 限定 HTTP 方法；`isPathVariable` / `regex` 动态匹配 |
| `@ERequestParam` | 参数 | 注入 query/form 参数 |
| `@ERequestBody` | 参数 | 注入 JSON 请求体（反序列化为参数类型） |
| `@ERequestHeader` | 参数 | 注入请求头 |
| `@EPathVariable` | 参数 | 注入路径变量 |
| `@Json` | 方法 | 以 `application/json;charset=UTF-8` 输出返回对象 |
| `@EAutowired` | 字段 | 按字段名从 `services` 配置包注入实现 |

> **参数注入硬约束**：控制器方法参数必须带上述注解之一，否则框架不注入（`ControllerAOP` 需要注解元数据）。

### 10.2 注解速查（ORM）

详见 [Tarn-2.0-ORM使用指南.md](./Tarn-2.0-ORM使用指南.md)。

### 10.3 常见问题（FAQ）

1. **新增路由 404？**
   - 包是否已加入 `context-mapping.xml` 的 `controllers`；
   - 路径是否含 `${` 或 `{`（含则走老链路，需老 `controller-type` 支持）；
   - HTTP 方法是否与 `method` 一致；
   - 请求是否被 `InterceptSuffix` 过滤（静态资源后缀不会进入 MVC）。
2. **`@ERequestParam` 为 null 时类型报错？**
   2.0 已修复 `CastorService.toType(null)`：按目标类型返回 `Integer 0` / `Long 0L` / `Float 0F` / `Double 0D` / `Boolean false`。
3. **接口返回 JSON 却是空白页？**
   方法需 `@Json` 或返回类型为 `JsonResult`。
4. **列名大小写不匹配导致字段为 null？**
   ORM 列名规则是“字段名大写”；跨库查询请用 `AS` 别名（如 `d.NAME AS DEPTNAME`），`queryEntityList` 已做忽略大小写匹配。
5. **为什么 `DaoException` 不能直接 `catch (Exception e)`？**
   它是历史遗留的 `extends Throwable`，必须显式声明/捕获；2.0 未改继承关系（避免破坏存量编译）。
6. **老项目还在用 c3p0 会怎样？**
   仍可运行，但已标记废弃；建议切 `pool-type: druid`（配置兼容）。
7. **能否只启用 JSON 不改配置？**
   可以，`@Json` 与 `JsonResult` 不依赖 YAML。
8. **独立运行需要改 web.xml 吗？**
   不需要，`StandaloneLauncher` 会自动注册过滤器；`config-class` 通过 YAML 传入。

### 10.4 示例代码索引

| 文件 | 说明 |
| --- | --- |
| `tarn-demo/src/main/java/examples/demo/controller/DemoUserController.java` | 全链路示例：新路由 + `@Json` + ORM + SqlBuilder + MyBatis |
| `tarn-demo/src/main/java/examples/demo/service/DemoUserService.java` | 四类数据访问姿势集中演示 |
| `tarn-demo/src/main/java/examples/demo/entity/DemoUser.java` | 实体注解示例（`@Etable` / `@Ekey` / `@EUpdateNull` / `@ORMFilter` / `@EFilter`） |
| `tarn-demo/src/main/java/examples/demo/mapper/DemoUserMapper.java` | MyBatis 注解式 Mapper（无 XML） |

---

**变更记录**

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| 2.0.0 | 2026-10 | 路由 / JSON / 参数化 SQL / 方言 / YAML / 插件体系 / 独立运行 |
| 2.1（能力代号） | 2026-10 | 组合式 ORM 引擎（`OrmTemplate` 单一实现）/ 数据源密码加密（`decrypt` + `public-key`）/ AOP 注入体系重构（`aop.inject` 五组件）/ 全局拦截规范与可运行示例 / FreeMarker 配置兜底与 legacy key 修正 |
