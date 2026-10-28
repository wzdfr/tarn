# Tarn Framework 框架项目说明书

> 版本：1.0.0（ORM 架构重构等新增能力以「2.1」为代号，`Tarn.version()` 返回 1.0.0）　|　配套文档见文末索引　|　验证环境：JDK 8 / Maven 3 / MySQL 5.7
>
> **合并版总手册**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md)
> （框架说明 / 配置 / MVC / 注入 / 路由 / 全局拦截 / ORM / 插件 / Spring Boot / Maven / 迁移 / FAQ 一站式查阅）

---

## 1. 项目定位

Tarn 是一套轻量级 Java Web 框架，覆盖 **MVC 控制器、静态/动态路由、ORM、参数化 SQL、
多数据库方言、声明式插件、JSON 前后端分离、内嵌容器独立运行** 全链路。
设计目标：**存量 1.x 项目零改动升级；新增能力以增量 API 提供，不与老 API 冲突。**

核心入口：

| 入口 | 场景 |
| --- | --- |
| `TarnDispatcherFilter` | Web 请求总入口（Tomcat / 内嵌 Jetty 共用） |
| `Router` / `RouteDispatcher` | 静态路由表 O(1) 命中 + 动态路由（`{id}` / 正则） |
| `Orm.template()` | **ORM 新入口（2.1 推荐）**，组合式引擎，无需继承任何类 |
| `new Q()` / `new Tarn()` | ORM 老入口（全部保留，内部委托同一引擎） |
| `StandaloneLauncher.run()` | 无 Tomcat 独立运行（内嵌 Jetty 9.4） |

---

## 2. 工程结构（Maven 多模块）

```
tarn-framework-2026-beta/
├── pom.xml                      父 POM（统一版本 / 依赖管理 / 插件管理）
├── docs/                        全套文档（白皮书 / ORM 指南 / 路由指南 / Maven / SpringBoot）
├── libs/                        工程内本地仓库（中央仓库缺失的历史 jar）
├── tarn-framework/               框架核心（发布物：tarn-framework-1.0.0.jar）
├── tarn-spring-boot-starter/     Spring Boot 自动装配（发布物，注册 Tarn 过滤器）
├── tarn-demo/                   示例工程（demo / test / standalone / orm / filter 五套示例，不随框架发布）
└── samples/
    ├── tarn-maven-demo/          团队 Maven 集成最小样例（独立构建，不进父模块）
    └── tarn-springboot-demo/     Spring Boot 共存样例（tarn-spring-boot-starter）
```

框架核心包结构（`com.dfrdev.tarn`）：

| 包 | 职责 |
| --- | --- |
| `core.boot` | 框架启动与配置：`Tarn` / `Config` / `ConfigLoader` / `TarnConfig` / `PluginInit` / `Plugins` |
| `core.web` | Web/MVC 层：`Controller` / `PageContext` / `Pager` / `Model` / `TarnDispatcherFilter` / `Filters` |
| `core.db` | 数据库/ORM 继承链（@Deprecated）：`Q` / `DB` / `DAOAbstract` / `DAO` / `Service` / `AbstractService` |
| `core.connection` | 连接池抽象与线程事务（`ConnectionFactory` / `ConnectionInit` / Druid） |
| `core.sql` | 参数化 SQL：`SqlBuilder` / `QueryBuilder` / `ParameterizedSql` |
| `core`（保留） | 异常基类：`TarnException` / `TarnDataException` |
| `route` | 路由：`Router`（静态表）/ `RouteEntry` / `RouteDispatcher` |
| `orm` | **组合式 ORM 引擎（推荐）**：`OrmTemplate` / `Orm` / `TransactionCallback` / `EntitySql` / `EntityMapper` |
| `orm.dialect` | 9 种方言分页：MySQL / PostgreSQL / Oracle / Oracle12c / SQLServer / DM / Kingbase / GaussDB / Vastbase |
| `aop` | 注入与调度：`Inject` / `ServiceInject` / `RepositoryInject` / `ExecuteInject` / `BeforeInject` / `AttrInject` / `TarnInject`；注解体系（`EController` / `Etable` / `Ekey` ...） |
| `aop.inject` | **2.1 注入基础设施**：`BeanProperty` / `BeanProperties` / `ComponentRegistry` / `ComponentScanner` / `ComponentInjector` |
| `json` | `JsonKit` 统一门面（可替换引擎）/ `JsonResult` |
| `render` | 视图渲染：JSP / FreeMarker / Velocity / Json / Text / File / Redirect |
| `plugin` | 插件体系：Redis / MyBatis / Druid / Ehcache / sqlScript |
| `config` | `YamlConfigKit`（application.yml 扁平化 + 老 key 自动桥接） |
| `standalone` | `StandaloneLauncher`（内嵌 Jetty） |

---

## 3. 核心链路

### 3.1 Web 请求链路

```
HTTP 请求
   │
   ▼
TarnDispatcherFilter#doFilter（编码 / PageContext / 后缀白名单拦截）
   ├──► Router.lookup(path, method) ── 命中 ──► RouteDispatcher.dispatch
   │                                              └► ExecuteInject（参数注入 + @Json 输出）
   ├──► 未命中（${mapping} 老写法）──► 老 RequestInterceptor 链（行为不变）
   ▼
finally: MybatisKit.close()
```

路由判定：不含 `${`/`{` 的映射进**静态表**（O(1)）；`{id}` 路径变量编译为**动态 Pattern**，
静态未命中时按字面量长度降序匹配；控制器根路径含 `${mapping}` 的老写法不进路由表，走老链路。

### 3.2 ORM 调用链（2.1：组合 + 工厂，双入口同一实现）

```
   新代码（推荐）          老代码（零改动兼容）
        │                        │
        ▼                        ▼
  Orm.template()          Tarn → Q → DB → DAOAbstract
  （工厂入口）              （老继承链，全部签名保留）
        │                        │
        └────────┬───────────────┘
                 ▼
        OrmTemplate.shared()      ← 唯一实现（组合 ConnectionFactory，不继承任何类）
                 │
      ┌──────────┼──────────────┬─────────────┐
      ▼          ▼              ▼             ▼
  EntitySql   EntityMapper   SqlBuilder/   ConnectionFactory
  (实体→SQL)  (注解元数据缓存)  QueryBuilder  (Druid 池 / 线程事务)
```

- **职责分离**：`EntitySql` 只做实体→参数化 SQL 的纯函数翻译；`OrmTemplate` 负责连接、事务、结果映射；
  老链 `Q/DB/DAOAbstract` 退化为薄门面（逐方法一行委托）。
- **为什么不用继承**：老链 4 层 `extends` 使 `Tarn` 暴露 100+ 方法（API 面污染）、写语义多层重复实现；
  组合式引擎把「门面（能做什么）」与「引擎（怎么做）」分离，并让声明式事务、异常统一有了单一落点。
- `DAO` 是查询结果**行容器**（CaseInsensitiveMap 语义），不是数据访问入口，2.1 未做改动。

### 3.3 异常体系

| 异常 | 性质 | 场景 |
| --- | --- | --- |
| `TarnDataException` | **RuntimeException（2.1 统一）** | 新引擎 / 新 API 的所有 SQL 失败，业务可直接捕获或交给全局处理 |
| `DaoException` | extends **Throwable**（历史遗留） | 老 API 签名保留所需；catch 时不能只写 `catch (Exception)` |
| `IllegalArgumentException` | 运行时 | 前置校验：无主键删除、非法标识符、`#{name}` 未绑定等 |

### 3.4 依赖注入体系（2.1 重构）

初始化顺序（框架启动时静态执行一次）：

```
Inject（扫描 <controllers>，建 ControllerInfo + 路由表）
   └─► new ServiceInject()（扫描 <services>，注册 @EService 实现）
          └─► <repositorys> 非空 → new RepositoryInject()（扫描 + limitPackage 过滤）
```

2.1 把重复的「反射找 setter / 扫包 / 存实现 / 写清单」抽到 `aop.inject` 五组件：

| 层 | 组件 | 职责 |
| --- | --- | --- |
| 属性层 | `BeanProperty` / `BeanProperties` | 字段 + public setter/getter 模型，按 Class 缓存 |
| 注册层 | `ComponentRegistry` | bean 名/接口名 → 实现类 → 单例；双重检查锁 |
| 扫描层 | `ComponentScanner` | jar/IDE 双路径扫包 + 生成 `framework-mapping-*.xml` 清单 |
| 注入层 | `ComponentInjector` | 注解命中 → 取实例 → setter 优先回填；bean 名策略与创建回调可插拔 |

各 `*Inject` 退化为配置壳：`ServiceInject`（`@EAutowired`）、`RepositoryInject`（`@ERepository`）、
`TarnInject`（`Tarn` / `PageContext` 内置对象）、`BeforeInject`（`@Before` 拦截器，缓存改为静态）、
`AttrInject`（请求属性级联路径按下标遍历）、`ExecuteInject`（纯编排，参数解析交给 `MethodParamResolver`）。
**老 API 方法名/入参/返回类型全部保留，老项目零改动。**

### 3.5 全局拦截体系（2.1 规范）

| 层 | 位置 | 能力 | 配置 |
| --- | --- | --- | --- |
| Servlet Filter | Tarn 过滤器之前 | around（耗时/响应包装） | web.xml / `FilterRegistrationBean` / Jetty `addFilter` |
| `FilterInterface` + `Filters` | Tarn 过滤器内部、路由之前 | 前置处理、短路、写响应 | `TarnConfig#configFilter` 或 `expand-filter.xml` |
| `@Before(Interceptor[])` | 映射方法之前 | 业务级鉴权/埋点 | 控制器类或方法 |

`FilterInterface.doFilter` 返回 `false` 即中断（不进入路由）；它**没有后置回调**，
耗时统计与响应包装必须用 Servlet Filter。三种运行环境的 `configClass` 来源：

| 环境 | 配置方式 |
| --- | --- |
| Tomcat | `TarnDispatcherFilter` 的 `<init-param>configClass</init-param>` |
| 内嵌 Jetty | `StandaloneLauncher.run(..., configClass)` 或 `tarn.server.config-class` |
| Spring Boot | `tarn.spring.config-class` |

可运行示例：`tarn-demo/src/main/java/examples/filter/**`（CORS → Auth → AccessLog + `GlobalFilterDemo`）。
详见《全书》§8。

---

## 4. 配置体系

**优先级**：`application.yml`（`YamlConfigKit` 扁平化）＞ `dev.properties`（老 key 自动桥接）。

```yaml
tarn:
  datasource:
    type: MYSQL                    # 方言，缺省按 JDBC URL 自动识别
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: "xxx"
    driver-class-name: com.mysql.cj.jdbc.Driver
    decrypt: false                 # 2.1：true 时启动阶段用 Druid ConfigTools 解密 password
    # public-key: "<ConfigTools 生成的公钥>"   # 留空用 Druid 内置默认公钥
  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    dev-mode: true
  system:
    debug: true                    # 生产必须 false
  server:                          # 仅独立运行模式
    port: 9090
    context-path: /
    config-class: examples.standalone.StandaloneAppConfig   # 全局拦截/插件装配入口
```

- YAML 直连配置（url/username/password）优先于 `connection-config-file` 指定的连接池配置文件；
- **数据源密码加密（2.1）**：`decrypt: true` + `password: 密文` + `public-key`，
  解密在建池前完成，失败直接启动报错；密钥约定为 `genKeyPair` 返回 `[私钥, 公钥]`、私钥加密公钥解密
  （完整步骤见《全书》§4.3）；
- 老 `dev.properties` 的 `DBType` / `controller-type` / `isDebug` 等无需改名即可读取（完整映射见《白皮书与升级迁移指南》§5.3，含 FreeMarker 三项与扩展 Filter 两项）；
- `tarn.plugins` 未配置时插件自动装配为空（不会 NPE）；
- 关键配置缺失有安全默认值（如 FreeMarker `template-update-delay` 缺省 3600 秒），只迁移 yml 不会启动失败。

---

## 5. ORM 能力速览

### 5.1 实体绑定（注解语义与 1.x 完全一致）

| 规则 | 说明 |
| --- | --- |
| 表名 | `@Etable("demo_book")`；缺省类名大写 |
| 列名 | **字段名大写**，不做驼峰转下划线（`orderNo` → `ORDERNO`） |
| 主键 | `@Ekey(key="id", isAutoIncrement=true)`；缺省列名 `ID` |
| 复杂类型 | `List` / `Map` / `Set` / 自定义类 → 自动 JSON 序列化入库，**查询自动反序列化回原类型（2.1 修复）** |
| 跳过 | `@ORMFilter`（完全不映射）、`@EFilter(true)`（写跳过）、`static` / `transient` |
| null 更新 | 默认跳过；`@EUpdateNull(true)` 字段 null 也写入（显式清空） |

### 5.2 写 API（老链 `Q` 与新引擎 `Orm.template()` 同名同义）

| API | 语义 |
| --- | --- |
| `insert(entity)` | 新增，自增主键不写入 |
| `insertReturnKey(entity)` | 新增并**回填数据库生成的主键**到实体 |
| `updateRows(entity)` | 按主键更新（WHERE 只认主键），返回受影响行数 |
| `deleteRows(entity)` | 严格按主键删除（杜绝老版“非空字段拼 WHERE”误删全表） |
| `batchInsert(list)` | 单事务批量，任一失败整体回滚 |
| `inTransaction(callback)` | **声明式事务**：正常返回提交，抛异常自动回滚 |

### 5.3 查询 API（全部参数绑定，杜绝注入）

| API | 场景 |
| --- | --- |
| `query(ps, Class)` / `queryOne(ps, Class)` | 实体列表 / 单条（列标签忽略大小写匹配字段） |
| `query(ps, RowMapper)` | 自定义行映射 |
| `queryMaps(ps)` | 任意结果集（JOIN / 自由列 / 聚合） |
| `count(ps)` / `exec(ps)` | 参数化统计 / 更新 |
| `QueryBuilder.page(pageNo, size)` | 跨库方言分页（`SqlBuilder.limit` 是 MySQL 字面量，跨库勿用） |

### 5.4 快速上手（新代码标准写法）

```java
OrmTemplate orm = Orm.template();

// 增（主键回填）
Book b = new Book(); b.setTitle("Tarn 实战"); b.setStock(10);
orm.insertReturnKey(b);

// 改（null 字段跳过）
b.setStock(99); orm.updateRows(b);

// 查（动态条件 + 分页）
List<Book> list = orm.query(
        SqlBuilder.select("*").from("demo_book")
                .where("STOCK >= ?", 3)
                .and("TITLE LIKE ?", "%Tarn%")
                .orderBy("ID", true).limit(0, 10).build(),
        Book.class);

// 事务（下单扣库存，条件更新防超卖）
orm.inTransaction(new TransactionCallback<Void>() {
    public Void doInTransaction(OrmTemplate orm) {
        int rows = orm.exec(SqlBuilder.create(
                "UPDATE demo_book SET STOCK = STOCK - #{q} WHERE ID = #{i} AND STOCK >= #{q}")
                .bind("q", 2).bind("i", 1L).build());
        if (rows == 0) throw new IllegalStateException("库存不足");   // 自动回滚
        orm.insertReturnKey(order);
        return null;                                                  // 正常提交
    }
});
```

---

## 6. 运行模式

| 模式 | 关键点 |
| --- | --- |
| 传统 Tomcat | `web.xml` 注册 `TarnDispatcherFilter`（老部署零感知） |
| 独立运行 | `StandaloneLauncher.run()`，filter 编程式注册，配置走 `tarn.server.*`，无需 web.xml |
| Spring Boot 共存 | `tarn-spring-boot-starter` 注册 Tarn filter，未命中请求回退 Spring MVC |
| Maven 集成 | 团队项目依赖 `tarn-framework`（`mvn install` 后可用），参见 Maven 集成指南 |

---

## 7. 质量与安全

### 7.1 已落地的修复与加固（2.1）

| 类别 | 修复项 |
| --- | --- |
| SQL 安全 | 全部新 API 参数绑定；列名 / 表名 / orderBy 标识符白名单校验；老拼接 API 标记 `@Deprecated` 并给出替代 |
| 误删除防护 | `deleteRows` 严格按主键；主键为空直接拒绝（老实现可能按非空字段批量删） |
| 异常治理 | 写失败不再 `LogUtil` 后静默吞掉，统一抛 `TarnDataException`；连接异常路径全部释放 |
| 线程安全 | `ConnectionFactory.init()` 加锁；`Tarn.pageUrl` 改局部变量；`pageContext` ThreadLocal 收敛并提供 `clearPageContext()` |
| 泛型 bug | `ActionInvocation.invokeSetORM` JSON 反序列化：`List<String>` 泛型实参误传 `java.lang.Class` 导致回读恒为 null——已修复，`Map`/`Set` 改用正确解析路线 |
| 资源管理 | CLOB 转 String 收口；executable JAR 下 `context-mapping.xml` 走 classloader 流读取 |
| 数据源密码加密 | `tarn.datasource.decrypt` / `public-key` 触发建池前解密，失败直接抛 `TarnException`；解密后移除相关配置项，避免 Druid 二次处理 |
| 注入体系重构 | `aop.inject` 五组件（`BeanProperty` / `BeanProperties` / `ComponentRegistry` / `ComponentScanner` / `ComponentInjector`）统一扫描与注入；原注入类仅保留配置职责，老 API 签名不变 |
| 拦截器缓存 | `BeforeInject` 拦截器实例缓存改为 `static`，避免每次请求重建导致缓存失效 |
| 级联注入 | `AttrInject` 修复多级路径（`a.b.c`）解析隐患，空路径按当前属性处理 |

### 7.2 生产检查清单

- [ ] `tarn.system.debug: false`、`save-sql-log` 关闭（后者写 `SQLEXECLOG` 表，严重拖慢）
- [ ] 数据源账号最小权限；密码不落代码仓库（用环境变量 / 配置中心）
- [ ] 全局异常处理器捕获 `TarnDataException`（避免 500 白页）
- [ ] 老拼接式 API（`exec(String)` / `find(String)`）逐步替换为 `SqlBuilder`

---

## 8. 验证报告（真实 MySQL）

- 环境：MySQL 5.7，`jdbc:mysql://127.0.0.1:3306/tarn_demo`，Druid 连接池，表 `demo_book`
- 验证程序与断言明细：ORM 使用指南 §11.3（46 项全 PASS），业务场景 `OrmUsageExamples` E1~E8 全通过
- 老 API 兼容：`OrmLegacyApiVerification` 21 项断言全 PASS（`Q` / `Tarn` 入口的 save / saveRK / update / delete / find / querys / findById / pager）
- 全局拦截：`examples.filter.GlobalFilterDemo`（8081）联调通过——`ping` 返回 200 且带 CORS 头与 `X-Trace-Id`；无 `X-Token` 返回 401 且日志未打印 AccessLog（短路生效）；带 `X-Token` 返回 200；`OPTIONS` 返回 204

```bash
# 全量 API 验证（断言式，46 项）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmApiVerification

# 老 API 兼容验证（断言式，21 项）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmLegacyApiVerification

# 业务场景示例（教程式输出：JSON 字段 / 动态条件 / 分页 / 事务提交与回滚 / JOIN / 聚合 / 手动事务）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmUsageExamples

# 全局拦截示例（内嵌 Jetty，8081）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.filter.GlobalFilterDemo
```

覆盖矩阵：实体↔表绑定（表名/主键/自增/大写列名）→ 全部写 API（含主键回填、受影响行数、
批量回滚）→ 事务（提交/回滚/手动事务）→ 参数化查询（RowMapper / 实体 / Map / COUNT / 分页 /
注入拦截）→ 老 API 兼容（`Q` save/saveRK/update/delete/find/querys/findById、`Tarn` 入口与版本）→
全局拦截（CORS / 鉴权短路 / 访问日志 / traceId）。

---

## 9. 构建与发布

```bash
mvn -DskipTests install              # 全模块构建并安装到本地仓库
mvn -DskipTests -pl tarn-framework deploy   # 发布团队私服（配置见父 POM distributionManagement）
```

- 版本号统一在父 POM 管理；`tarn-demo` / `samples/**` 不属于框架发布物；
- 中央仓库缺失的历史依赖（如 cos）通过工程内 `libs/` 本地仓库解析。

---

## 10. 文档索引

| 文档 | 内容 |
| --- | --- |
| [Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md) | **合并版总手册**：框架 / 配置 / MVC / 注入 / 路由 / 全局拦截 / ORM / 插件 / Spring Boot / Maven / 独立运行 / 迁移 / 排障 |
| [Tarn-1.0-白皮书与升级迁移指南.md](./Tarn-1.0-白皮书与升级迁移指南.md) | 架构总览、新能力清单、老项目五步迁移、回滚方案 |
| [Tarn-1.0-ORM使用指南.md](./Tarn-1.0-ORM使用指南.md) | ORM 全量 API、注解速查、多表关联、方言分页、事务、MySQL 验证报告 |
| [Tarn-1.0-路由与独立运行使用指南.md](./Tarn-1.0-路由与独立运行使用指南.md) | 静态/动态路由语法、StandaloneLauncher |
| [Tarn-1.0-Maven集成指南.md](./Tarn-1.0-Maven集成指南.md) | 团队 Maven 集成与私服发布 |
| [Tarn-1.0-SpringBoot集成指南.md](./Tarn-1.0-SpringBoot集成指南.md) | tarn-spring-boot-starter 与 Spring MVC 共存 |
