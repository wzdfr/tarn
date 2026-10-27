# Tarn 2.0 路由与独立运行使用指南

> 配套文档：[Tarn-2.0-白皮书与升级迁移指南.md](./Tarn-2.0-白皮书与升级迁移指南.md) ｜ [Tarn-2.0-ORM使用指南.md](./Tarn-2.0-ORM使用指南.md) ｜ **合并版总手册**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md)（路由 §7、全局拦截 §8、独立运行 §13）
> 可运行示例：`tarn-demo/src/main/java/examples/standalone/**`

本文回答两件事：

1. **Router 怎么用**：老的 `@EController` / `@ERequestMapping` 会不会自动进路由表？`/delete/{id}` 这类正则路由怎么写？
2. **`StandaloneLauncher.run()` 怎么用**：脱离 Tomcat 独立运行时，路由与全链路（entity → dao → service → controller）该怎么落地。

---

## 1. 路由体系总览

Tarn 2.0 的路由由 [Router](../tarn-framework/src/main/java/cc/ruixing/framework/tarn/route/Router.java) 统一维护，内部是**两张表**：

| 表 | 存储结构 | 适用路径 | 复杂度 |
| --- | --- | --- | --- |
| 静态路由表 | `Map<String, RouteEntry>` | 完全字面量，如 `/standalone/book/list` | O(1) 精确命中 |
| 动态路由表 | `List<RouteEntry>` + 预编译 `Pattern` | 含 `{id}` 等变量，如 `/standalone/book/detail/{id}` | 按“字面量长度降序”遍历正则 |

### 1.1 请求链路

```
HTTP 请求
   │
   ▼
TarnDispatcherFilter#doFilter
   ├─► Router.lookup(vpath, httpMethod)
   │        ├─ 静态表精确命中  ─────────────► RouteDispatcher.dispatch
   │        └─ 静态未命中 → 动态表正则匹配 ──► RouteDispatcher.dispatch
   │                 └─ 仍未命中 ───────────► 老 RequestInterceptor 链（行为与 1.x 完全一致）
   ▼
finally: MybatisKit.close()（释放请求级 MyBatis 会话）
```

`RouteDispatcher` 的执行步骤与老 `RequestMappingInterceptor` 一致：实例化控制器 → `ServiceInject` 注入 → `AttrInject` 注入请求属性 → 执行 `execute()` → 执行映射方法（带参数元数据，`@ERequestParam` / `@ERequestBody` / `@EPathVariable` 全部可用）→ 都没有则 `filterChain.doFilter` 放行。

### 1.2 注册时机与日志

路由表在 `TarnDispatcherFilter` 初始化时（`Router.build(...)`）扫描 **`context-mapping.xml` 中 `<controllers>` 声明的包**，一次性构建完成。启动日志会打印：

```
[Router] 路由表构建完成, 静态 14 条, 动态 2 条
[Router] 动态路由 /standalone/book/delete/{id} -> examples.standalone.controller.BookController#delete
[Router] 动态路由 /standalone/book/detail/{id} -> examples.standalone.controller.BookController#detail
```

没有这两行日志，说明控制器包没被扫到（检查 `context-mapping.xml`）。

---

## 2. 老注解会自动注册吗？

**会。不需要任何额外配置。**

只要控制器类被 `context-mapping.xml` 的 `<controllers>` 包扫描到，`@EController` + `@ERequestMapping` 就会被自动注册：

```xml
<controllers>
    <mapping package="examples.standalone.controller" name="独立运行示例"/>
</controllers>
```

映射规则：

```
完整路由 = @EController 的 value + @ERequestMapping 的 value
```

| `@EController` | `@ERequestMapping` | 注册结果 |
| --- | --- | --- |
| `/standalone/book` | `/list` | 静态表：`/standalone/book/list` |
| `/standalone/book` | `/detail/{id}` | 动态表：`/standalone/book/detail/{id}` |
| `/Tarn/${mapping}` | `/${method}` | 不注册，走老拦截器链（老项目兼容） |

> 补充：老链路存在一个历史限制——控制器根路径是**静态**时，方法上的 `{id}` 在老 `RequestMappingUtils` 中不会被解析。Router 的静态路由注册正好补上了这个缺口，因此**静态根路径 + 方法级路径变量**只能通过 Router 生效（老项目不改也照旧走老链路，不受影响）。

---

## 3. 静态路由

控制器根路径、方法映射**都不含** `${` 与 `{` 时，进入静态表：

```java
@EController("/standalone/book")
public class BookController extends Controller {

    @Json
    @ERequestMapping(value = "/list", method = RequestMethod.GET)
    public JsonResult list(@ERequestParam("keyword") String keyword) { ... }

    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() { ... }
}
```

- `GET /standalone/book/list?keyword=Java` → 命中；
- `POST /standalone/book/list` → 方法不匹配 → 不命中（不会报错，最终走老链路/404）；
- `method = RequestMethod.AUTO`（默认）表示不限请求方法。

---

## 4. 动态路由（`/delete/{id}`）

### 4.1 语法表

| 模板片段 | 含义 | 编译出的正则 |
| --- | --- | --- |
| `{id}` | 方法级路径变量 | `@ERequestMapping(regex="...")`，缺省 `[^/]+`（单段） |
| `${mapping}` | 控制器级通配（老写法） | `@EController(regex="...")`，缺省 `[^/]+` |
| `{*}` | 贪婪匹配剩余路径 | `.+` |
| 其他字面量 | 普通路径片段 | `Pattern.quote` 转义，原样匹配 |

整个模板会被加上 `^...$`，即**全路径匹配**，所以 `regex` 里**不要再写 `^` / `$`**。

### 4.2 `/delete/{id}` 写法

```java
@EController("/standalone/book")
public class BookController extends Controller {

    // 只匹配数字 ID，/delete/abc 直接 404，脏参数进不来
    @Json
    @ERequestMapping(value = "/delete/{id}", method = RequestMethod.POST,
                      isPathVariable = true, regex = "[0-9]+")
    public JsonResult delete(@EPathVariable("id") Long id) { ... }
}
```

对应的实际正则：

```
^/standalone/book/delete/([0-9]+)$
```

| 请求 | 结果 |
| --- | --- |
| `POST /standalone/book/delete/1` | 命中，`id = 1L` |
| `POST /standalone/book/delete/abc` | 不命中 → 404 |

### 4.3 多变量与自定义正则

```java
@Json
@ERequestMapping(value = "/user/{uid}/order/{orderNo}", method = RequestMethod.GET,
                  isPathVariable = true, regex = "[0-9]+")
public JsonResult orderDetail(@EPathVariable("uid") Long uid,
                              @EPathVariable("orderNo") Long orderNo) { ... }
```

编译结果：`^/user/([0-9]+)/order/([0-9]+)$`。变量注入按**出现顺序**与 `@EPathVariable("名称")` 对应，框架内部用“完整模板 + 实际路径”做前缀截取，因此：

- `@EPathVariable` 的名称要与模板中的变量名一致（如 `id`）；
- 模板必须是**完整路径**（控制器根 + 方法映射），这条已由 Router 自动拼好。

`{*}` 用于匹配剩余任意路径，如 `/file/{*}` → `^/file/(.+)$`。

### 4.4 匹配顺序

动态路由按**字面量长度降序**排序（占位符不计长度），越“具体”的模板越先尝试：

```
/user/list/{status}        ← 字面量更长，先匹配
/user/{id}/detail          ← 后匹配
```

相同字面量的歧义模板请避免（例如 `/user/{id}` 与 `/user/{name}`），否则结果取决于注册顺序。

### 4.5 与老链路的边界

| 场景 | 走哪条 |
| --- | --- |
| 根路径静态 + 方法静态 | 静态路由表 |
| 根路径静态 + 方法含 `{id}` | 动态路由表（**这是 2.0 新增能力**） |
| 根路径含 `${mapping}` | 老拦截器链（不做任何改变） |
| 根路径、方法都静态，但 HTTP 方法不匹配 | 不命中，回退老链路 |

### 4.6 404 排查清单

1. 启动日志有没有 `[Router] 路由表构建完成`？没有 → `context-mapping.xml` 没配 `<controllers>` 包。
2. 有没有 `[Router] 动态路由 ...` 对应你那条路径？没有 → 模板写法/HTTP 方法不匹配（`GET` 写成了 `POST` 等）。
3. 请求路径是否等于“上下文路径 + 完整模板”？`context-path` 不是 `/` 时，URL 要带上上下文前缀。
4. `regex` 是否过于严格（如写了 `^[0-9]+$`）？去掉 `^$`，框架已整体锚定。

---

## 5. 独立运行（StandaloneLauncher）

### 5.1 适用场景

- 本地开发/演示不想装 Tomcat；
- 内网小工具、微服务里跑 Tarn 控制器；
- 与老 Tomcat 部署并存（本类只有显式调用才生效，不影响老项目）。

依赖：`jetty-server` / `jetty-webapp` 9.4.x（框架中为 `optional`），以及运行期 `javax.servlet-api`。

### 5.2 目录结构

```
tarn-demo/
├── src/main/java/examples/standalone/
│   ├── StandaloneApp.java          # main，调用 StandaloneLauncher.run()
│   ├── StandaloneAppConfig.java    # TarnConfig 实现（等同 web.xml 的 configClass）
│   ├── entity/Book.java
│   ├── dao/BookDao.java
│   ├── service/BookService.java
│   ├── service/impl/BookServiceImpl.java
│   └── controller/BookController.java
├── src/main/resources/
│   ├── application.yml
│   └── context-mapping.xml
└── standalone-web/                 # 内嵌 Jetty 的 webapp 根目录
    ├── WEB-INF/web.xml             # 极简，不需要声明 TarnDispatcherFilter
    └── index.html
```

### 5.3 application.yml

```yaml
tarn:
  datasource:
    type: MYSQL
    pool-type: druid
    # 直连配置优先于 connection-config-file（druid.properties）
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password:
    driver-class-name: com.mysql.cj.jdbc.Driver
    decrypt: false        # 2.1：true 时建池前解密 password
    # public-key: "..."   # 自定义公钥，留空用内置默认公钥

  server:
    port: 8080
    context-path: /
    webapp-dir: standalone-web          # 相对 tarn-demo 模块根目录；也可写绝对路径
    config-class: examples.standalone.StandaloneAppConfig
```

> `tarn.datasource.url/username/password/driver-class-name` 会覆盖 `druid.properties` 中同名项；未写的项保持配置文件原值，写空值则覆盖为空（例如空密码）。

### 5.4 web.xml

```xml
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee" ...>
    <display-name>tarn standalone demo</display-name>
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>
</web-app>
```

`StandaloneLauncher` 会用代码注册 `TarnDispatcherFilter`（`/*`）。`configClass` 取值优先级：**`run(...)` 入参 > `tarn.server.config-class` > 空**。如果你的 web.xml 里**已经**声明了该过滤器，启动器会检测到并跳过代码注册，避免重复执行。

### 5.5 启动类

```java
public class StandaloneApp {
    public static void main(String[] args) {
        // 读 application.yml 的 tarn.server.*（含 port / context-path / webapp-dir / config-class）
        StandaloneLauncher.run();

        // 显式指定三参（不依赖 YAML；configClass 仍从 tarn.server.config-class 读）
        // StandaloneLauncher.run(8080, "/", "standalone-web");

        // 显式指定四参（2.1 新增：直接传入装配类，优先级高于 YAML）
        // StandaloneLauncher.run(8081, "/", "standalone-web", "examples.filter.GlobalFilterConfig");
    }
}
```

| 重载 | 说明 |
| --- | --- |
| `run()` | 全部取 `tarn.server.*` |
| `run(port, contextPath, webappDir)` | 端口/路径/webapp 走入参，`configClass` 仍取 YAML |
| `run(port, contextPath, webappDir, configClass)` | 2.1 新增，四个参数全部显式，适合多环境切换与联调 |

### 5.6 三种启动方式

```bash
# 1) IDE：直接运行 StandaloneApp#main（工作目录设为 tarn-demo）

# 2) Maven exec（工作目录 tarn-demo）
mvn exec:java -Dexec.mainClass=examples.standalone.StandaloneApp

# 3) 打包后 java -cp（工作目录 tarn-demo，target/lib 由 dependency-plugin 生成）
mvn -q package
java -cp "target/classes:target/lib/*" examples.standalone.StandaloneApp
```

启动成功日志：

```
ruixing framework.Tarn Successfully Start ...
[Router] 路由表构建完成, 静态 14 条, 动态 2 条
[StandaloneLauncher] Tarn 独立运行已启动: http://127.0.0.1:8080/ (webapp=.../standalone-web)
```

> 从父工程根目录启动时，若 `standalone-web` 不存在，启动器会自动回退查找 `tarn-demo/standalone-web`。

---

## 6. 全链路示例（entity → dao → service → controller）

### 6.1 建表

```sql
CREATE TABLE demo_book (
    ID     BIGINT PRIMARY KEY AUTO_INCREMENT,
    TITLE  VARCHAR(128) NOT NULL,
    AUTHOR VARCHAR(64),
    STOCK  INT DEFAULT 0
);
```

> ORM 列名规则：**字段名大写**（`title` → `TITLE`），不做驼峰转下划线。

### 6.2 entity

```java
@Etable("demo_book")
public class Book {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String title;
    private String author;
    private Integer stock;

    // getter / setter 略
}
```

### 6.3 dao

```java
public class BookDao {

    private final Q q = new Q();

    /** 新增：自增主键由数据库生成，回填到 book.id */
    public void save(Book book) throws DaoException {
        q.save(book);
    }

    /** 列表：条件值一律走参数绑定 */
    @SuppressWarnings("unchecked")
    public List<Book> list(String keyword) throws DaoException {
        SqlBuilder builder = SqlBuilder.select("ID, TITLE, AUTHOR, STOCK").from("demo_book");
        if (StringUtil.isNotEmpty(keyword)) {
            builder.where("TITLE LIKE #{keyword}").bind("keyword", "%" + keyword + "%");
        }
        builder.orderBy("ID", false);
        return (List<Book>) (List<?>) q.query(builder.build(), Book.class);
    }

    public Book findById(Long id) throws DaoException {
        ParameterizedSql ps = SqlBuilder.select("ID, TITLE, AUTHOR, STOCK")
                .from("demo_book").where("ID = #{id}").bind("id", id).build();
        List<?> list = q.query(ps, Book.class);
        return list.isEmpty() ? null : (Book) list.get(0);
    }

    public int deleteById(Long id) throws DaoException {
        ParameterizedSql ps = SqlBuilder.create("DELETE FROM demo_book WHERE ID = #{id}")
                .bind("id", id).build();
        return q.exec(ps);
    }
}
```

注意：`DaoException` 继承自 `Throwable`（历史设计），方法必须显式声明或捕获。

### 6.4 service

```java
public interface BookService {
    List<Book> list(String keyword) throws DaoException;
    Book findById(Long id) throws DaoException;
    void create(Book book) throws DaoException;
    boolean delete(Long id) throws DaoException;
}
```

```java
@EService("bookService")          // 名称必须与控制器字段名一致
public class BookServiceImpl implements BookService {

    private final BookDao bookDao = new BookDao();

    public List<Book> list(String keyword) throws DaoException { return bookDao.list(keyword); }

    public Book findById(Long id) throws DaoException { return bookDao.findById(id); }

    public void create(Book book) throws DaoException { bookDao.save(book); }

    public boolean delete(Long id) throws DaoException { return bookDao.deleteById(id) > 0; }
}
```

### 6.5 controller

```java
@EController("/standalone/book")
public class BookController extends Controller {

    @EAutowired
    private BookService bookService;

    // ServiceInject 通过 setter/字段名匹配服务实例
    public void setBookService(BookService bookService) {
        this.bookService = bookService;
    }

    @Json
    @ERequestMapping(value = "/list", method = RequestMethod.GET)
    public JsonResult list(@ERequestParam("keyword") String keyword) {
        try {
            return JsonResult.ok(bookService.list(keyword));
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/detail/{id}", method = RequestMethod.GET,
                      isPathVariable = true, regex = "[0-9]+")
    public JsonResult detail(@EPathVariable("id") Long id) {
        try {
            Book book = bookService.findById(id);
            return book == null ? JsonResult.fail(404, "图书不存在") : JsonResult.ok(book);
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    @Json
    @ERequestMapping(value = "/save", method = RequestMethod.POST)
    public JsonResult save(@ERequestBody Book book) { ... }

    @Json
    @ERequestMapping(value = "/delete/{id}", method = RequestMethod.POST,
                      isPathVariable = true, regex = "[0-9]+")
    public JsonResult delete(@EPathVariable("id") Long id) { ... }

    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public JsonResult ping() { return JsonResult.ok("pong"); }
}
```

### 6.6 注册包（context-mapping.xml）

```xml
<framework>
    <controllers>
        <mapping package="examples.standalone.controller" name="独立运行示例"/>
    </controllers>
    <services>
        <mapping package="examples.standalone.service.impl" name="独立运行示例服务实现"/>
    </services>
</framework>
```

`<services>` 用于 `@EService` 实例注册，`<controllers>` 用于扫描控制器并构建路由表。

### 6.7 验证

```bash
# 健康检查（不依赖数据库）
curl http://127.0.0.1:8080/standalone/book/ping
# {"code":200,"data":"pong","message":"success","success":true,...}

# 静态路由 + 查询参数
curl "http://127.0.0.1:8080/standalone/book/list?keyword=Tarn"

# 动态路由（正则只认数字，/detail/abc → 404）
curl http://127.0.0.1:8080/standalone/book/detail/1
curl -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/standalone/book/detail/abc   # 404

# JSON 请求体
curl -X POST http://127.0.0.1:8080/standalone/book/save \
     -H "Content-Type: application/json" \
     -d '{"title":"Tarn 实战","author":"dfr","stock":10}'

# 动态路由 + POST
curl -X POST http://127.0.0.1:8080/standalone/book/delete/1
```

---

## 7. 全局拦截（Standalone 下如何配置）

`FilterInterface` + `Filters` 是框架的**路由前拦截**机制，在独立运行时同样可用，装配入口就是 `configClass`：

```java
public class GlobalFilterConfig implements TarnConfig {
    @Override
    public void configFilter(Filters filters) {
        filters.add(new CorsFilter())        // 统一 CORS，OPTIONS 直接 204 并 return false
               .add(new AuthFilter())        // X-Token 校验，失败 401 并 return false
               .add(new AccessLogFilter());  // 写 X-Trace-Id、记录请求开始时间
    }
}
```

```java
public class GlobalFilterDemo {
    public static void main(String[] args) {
        StandaloneLauncher.run(8081, "/", "standalone-web",
                "examples.filter.GlobalFilterConfig");
    }
}
```

要点：

- `FilterInterface.filter(...)` 返回 `false` 即**中断请求**，后续过滤器与控制器都不会执行（示例中无 token 时 401 且不打印 AccessLog 已验证）；
- 该接口**只有前置钩子**，无法做后置/环绕（耗时统计、包装 response）——这类需求请用标准 `javax.servlet.Filter`，并在 Jetty 中通过 `FilterHolder` 排在 `TarnDispatcherFilter` 之前（Spring Boot 用 `FilterRegistrationBean` 的 `order` 小于 `tarn.spring.order`）；
- `expand-filter.xml` 里的声明式过滤器会先于 `configFilter` 加载；`ExtendFilterImpl` 在 2.x **不再被实例化**（仅保留配置映射）；
- 示例代码：`tarn-demo/src/main/java/examples/filter/**`（`CorsFilter` / `AuthFilter` / `AccessLogFilter` / `GlobalFilterConfig` / `GlobalFilterDemo`）。

> 三层拦截模型（Servlet Filter / `FilterInterface` / `@Before`）的完整对比与迁移建议见《全书》§8。

---

## 8. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 启动日志没有 `[Router]` | `context-mapping.xml` 未登记控制器包 | 补 `<controllers><mapping package="..."/>` |
| `/delete/1` 404，`/delete/abc` 也 404 | 路由没注册（根路径含 `${mapping}`？）或方法不匹配 | 看动态路由日志；确认 `method` |
| `@EPathVariable` 注入为 null | 模板与实际路径前缀不一致 | 用完整模板（Router 已自动拼接），不要手写半截路径 |
| 参数注入抛 NPE（`methodParam.setIndex`） | 控制器方法参数没加注解 | 每个参数必须带 `@ERequestParam` / `@ERequestBody` / `@EPathVariable` 等 |
| `@EService("x")` 注入不到 | 名称与字段名不一致，或 impl 包未登记 | 名称对齐 + `<services>` 加包 |
| Standalone 端口冲突 | 8080 被占用 | `tarn.server.port` 换端口，或 `run(port, ...)` 显式指定 |
| Standalone 启动报 webapp 不存在 | `webapp-dir` 配错/启动目录不同 | 用相对 `tarn-demo` 的路径或绝对路径 |
| `Access denied for user` | YAML 直连账号不对 | 改 `tarn.datasource.*`，或确认 MySQL 授权 |
| 启动报「数据源密码解密失败」 | `decrypt: true` 但密码是明文，或密文与 `public-key` 不配对 | 明文改 `decrypt: false`；加密场景用私钥加密、公钥解密 |
| 全局过滤器没生效 | `configClass` 未指定 / 类没实现 `TarnConfig` / 包名拼错 | 见 §7；`run(...)` 四参可直接传类名 |
