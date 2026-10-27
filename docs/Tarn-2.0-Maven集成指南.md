# Tarn 2.0 Maven 集成指南

> 面向“别的同事/别的项目如何引用 Tarn 框架”。
> 示例工程：[samples/tarn-maven-demo](../samples/tarn-maven-demo)（纯 Maven 引用 + 独立运行）
> Spring Boot 接入见：[Tarn-2.0-SpringBoot集成指南.md](./Tarn-2.0-SpringBoot集成指南.md)
> **合并版总手册**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md)（Maven 章节见 §12）

---

## 1. 前置条件

| 项 | 要求 |
| --- | --- |
| JDK | 8（框架与 demo 均以 `maven.compiler.source/target=1.8` 编译） |
| Maven | 3.6+ |
| Servlet 容器 | Tomcat 8/9（Servlet 3.1）；不使用容器时可用内嵌 Jetty 9.4 |
| 数据库 | MySQL / Oracle / MSSQL / PostgreSQL / 达梦 / 金仓 / GaussDB / Vastbase 之一 |

---

## 2. 让框架“可被引用”：安装到仓库

Maven 只能引用**仓库里存在**的构件。新同事电脑第一次使用，需要先把框架装进仓库。

### 2.1 本机仓库（单人开发 / 局域网共用同一台机器）

在框架工程根目录执行：

```bash
# 安装全部模块（parent + tarn-framework + tarn-demo + tarn-spring-boot-starter）
mvn clean install -DskipTests
```

只装核心框架（不含 demo，速度更快）：

```bash
mvn -pl tarn-framework -am clean install -DskipTests
```

安装成功后，本机 `~/.m2/repository` 中会有：

```text
cc/ruixing/framework/tarn-parent/2.0.0/
cc/ruixing/framework/tarn-framework/2.0.0/
cc/ruixing/framework/tarn-spring-boot-starter/2.0.0/
com/oreilly/servlet/cos/05Nov2002/
```

> 注意 `cos:05Nov2002`：这是中央仓库没有的老 jar，框架工程内已有 `libs/` 本地仓库兜底；
> `install` 会一并把它装进 `~/.m2`，同事电脑只要拿到框架工程（或私服）即可正常解析。

### 2.2 公司私服（团队协作推荐）

把构件发布到 Nexus/Artifactory，同事只配一个 `<repository>`，无需安装框架源码。

**框架工程** `pom.xml` 增加发布地址：

```xml
<distributionManagement>
    <repository>
        <id>company-releases</id>
        <url>http://nexus.company.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>company-snapshots</id>
        <url>http://nexus.company.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

```bash
mvn clean deploy -DskipTests
```

`~/.m2/settings.xml` 配置私服认证：

```xml
<servers>
    <server>
        <id>company-releases</id>
        <username>deploy</username>
        <password>******</password>
    </server>
</servers>
```

**业务工程** `pom.xml` 声明仓库（或把私服配到 `settings.xml` 的 `<mirrors>`，全公司共享）：

```xml
<repositories>
    <repository>
        <id>company-releases</id>
        <url>http://nexus.company.com/repository/maven-releases/</url>
    </repository>
</repositories>
```

> `cos:05Nov2002` 也要一并 deploy（`mvn deploy` 从 `~/.m2` 读到的它会在 reactor 依赖中，但更稳妥的做法是单独 `mvn deploy:deploy-file` 上传一次）。

---

## 3. 依赖坐标

| 构件 | 坐标 | 用途 |
| --- | --- | --- |
| 核心框架 | `com.dfrdev:tarn-framework:2.0.0` | MVC + ORM + 路由 + 连接池 + YAML 配置 |
| Spring Boot 插件 | `com.dfrdev:tarn-spring-boot-starter:2.0.0` | Spring Boot 自动装配（可选） |
| 父 POM | `com.dfrdev:tarn-parent:2.0.0` | 仅框架工程内部使用，业务工程**不需要**继承 |

业务工程最小依赖：

```xml
<dependency>
    <groupId>com.dfrdev</groupId>
    <artifactId>tarn-framework</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- 按需：数据库驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
    <scope>runtime</scope>
</dependency>

<!-- 按需：独立运行（内嵌 Jetty），部署到 Tomcat 则不需要 -->
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>9.4.46.v20220331</version>
</dependency>
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-webapp</artifactId>
    <version>9.4.46.v20220331</version>
</dependency>
```

框架已传递引入（无需再声明）：fastjson、druid、snakeyaml、slf4j + logback、dom4j、
commons-fileupload/io/beanutils/codec/collections4、cos、log4j-over-slf4j。

以下为 **optional**，用到才引：javax.mail、c3p0（已淘汰）、mybatis、jedis + commons-pool2、
freemarker、velocity、ehcache、PostgreSQL/达梦/金仓/openGauss 驱动、jetty。

---

## 4. 新工程骨架

```text
my-tarn-app/
├── pom.xml
├── sql/schema.sql                       # 建表脚本（示例）
├── web/                                 # 独立运行时使用；war 部署时对应 src/main/webapp
│   ├── index.html
│   └── WEB-INF/web.xml
└── src/main/
    ├── java/
    │   └── samples/tarn/maven/
    │       ├── DemoApplication.java      # 启动类（main）
    │       ├── entity/Book.java          # 实体
    │       ├── dao/BookDao.java          # 数据访问
    │       ├── service/BookService.java  # 服务接口
    │       ├── service/impl/BookServiceImpl.java
    │       └── controller/BookController.java
    └── resources/
        ├── application.yml               # 数据源 / MVC / 系统配置
        └── context-mapping.xml           # 控制器、服务扫描包
```

要点：

- `context-mapping.xml` **必须**存在，控制器/服务所在包要写进去（否则不会被扫描、路由表为空）；
- `@EService("bookService")` 的名字要与控制器字段名（`bookService`）一致；
- 实体列名 = 字段名大写（`title` → `TITLE`），主键用 `@Ekey(key="id", isAutoIncrement=true)`。

---

## 5. 完整示例（与 samples/tarn-maven-demo 一致）

### 5.1 `application.yml`

```yaml
tarn:
  datasource:
    type: MYSQL
    pool-type: druid
    url: jdbc:mysql://127.0.0.1:3306/tarn_demo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password:
    driver-class-name: com.mysql.cj.jdbc.Driver
    decrypt: false         # 2.1：true 时建池前用 Druid ConfigTools 解密 password
    # public-key: "..."    # 自定义公钥，留空用内置默认公钥
  mvc:
    controller-type: auto
    intercept-suffix: jspx|jsp|do|u|shtml|rx
    default-view-type: OTHER
  system:
    debug: true
    encoding: utf-8
  server:               # 仅独立运行（StandaloneLauncher）使用
    port: 9090
    context-path: /
    webapp-dir: web
    config-class: samples.tarn.maven.AppConfig   # 可选：全局拦截 / 插件装配入口
```

### 5.2 `context-mapping.xml`

```xml
<framework>
    <controllers>
        <mapping package="samples.tarn.maven.controller" name="Maven 集成示例" author="dfr"/>
    </controllers>
    <services>
        <mapping package="samples.tarn.maven.service.impl" name="Maven 集成示例服务"/>
    </services>
    <repositorys/>
</framework>
```

### 5.3 实体

```java
@Etable("demo_book")
public class Book {
    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;
    private String title;     // 列 TITLE
    private String author;    // 列 AUTHOR
    private Integer stock;    // 列 STOCK
    // getter / setter 省略
}
```

### 5.4 DAO（JSON 之外的写操作走新 API）

```java
public class BookDao {

    private final Q q = new Q();

    public Long save(Book book) {
        Object key = q.insertReturnKey(book);          // 主键自动回填到 book
        return key instanceof Number ? ((Number) key).longValue() : book.getId();
    }

    public int update(Book book) {
        return q.updateRows(book);                     // 返回受影响行数
    }

    public int deleteById(Long id) throws DaoException {
        return q.exec(SqlBuilder.create("DELETE FROM demo_book WHERE ID = #{id}")
                .bind("id", id).build());
    }
}
```

### 5.5 Service

```java
@EService("bookService")        // 名字 = 控制器字段名
public class BookServiceImpl implements BookService {

    private final BookDao bookDao = new BookDao();

    public Long create(Book book) throws DaoException { return bookDao.save(book); }
    public int update(Book book) throws DaoException { return bookDao.update(book); }
}
```

### 5.6 Controller

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
    public JsonResult detail(@EPathVariable("id") Long id) { /* ... */ }

    @Json
    @ERequestMapping(value = "/save", method = RequestMethod.POST)
    public JsonResult save(@ERequestBody Book book) { /* ... */ }
}
```

### 5.7 启动类（独立运行）

```java
public class DemoApplication {
    public static void main(String[] args) {
        StandaloneLauncher.run(9090, "/", "web");   // port, contextPath, webappDir

        // 2.1 四参重载：显式指定 configClass（全局拦截 / 插件装配），优先级高于 YAML
        // StandaloneLauncher.run(9090, "/", "web", "samples.tarn.maven.AppConfig");
    }
}
```

`web/WEB-INF/web.xml` 只需一个空的 `web-app 3.0`（过滤器由 `StandaloneLauncher` 代码注册）：

```xml
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
         http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         version="3.0">
    <display-name>my-tarn-app</display-name>
</web-app>
```

---

## 6. 构建、运行、验证

```bash
# 1) 构建（外部工程，无需感知父 POM）
mvn clean package -DskipTests

# 2) 建表（MySQL）
mysql -uroot -p < sql/schema.sql

# 3) 运行（三选一）
mvn -q compile exec:java                       # exec 插件
java -cp "target/classes:target/lib/*" samples.tarn.maven.DemoApplication   # 打包产物
# 或在 IDE 中直接运行 DemoApplication#main（工作目录设为模块根目录）
```

验证：

```bash
curl http://127.0.0.1:9090/book/ping
# {"code":200,"data":"pong","message":"success","success":true,...}

curl http://127.0.0.1:9090/book/list
curl "http://127.0.0.1:9090/book/detail/1"
curl -X POST http://127.0.0.1:9090/book/save \
     -H 'Content-Type: application/json' \
     -d '{"title":"Tarn 实战","author":"dfr","stock":10}'
```

---

## 7. 部署到 Tomcat（不用内嵌 Jetty）

`pom.xml` 改 `<packaging>war</packaging>`，并添加：

```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
</dependency>
```

`src/main/webapp/WEB-INF/web.xml` 声明过滤器：

```xml
<filter>
    <filter-name>TarnDispatcherFilter</filter-name>
    <filter-class>com.dfrdev.tarn.core.TarnDispatcherFilter</filter-class>
    <init-param>
        <param-name>configClass</param-name>
        <param-value>com.myapp.AppConfig</param-value>   <!-- 可选，需实现 TarnConfig -->
    </init-param>
</filter>
<filter-mapping>
    <filter-name>TarnDispatcherFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

---

## 8. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `Could not find artifact com.oreilly.servlet:cos:jar:05Nov2002` | 中央仓库没有该历史 jar | 在框架工程根目录 `mvn install`（会从 `libs/` 装进 `~/.m2`），或让管理员上传到私服 |
| 路由全部 404 | `context-mapping.xml` 缺失 / 包名写错 | 确认资源在 `src/main/resources`，`<mapping package>` 与控制器包一致 |
| 启动即报 `dev.properties not exist`（老版本） | 1.x 强依赖 `dev.properties` | 2.0 起可只用 `application.yml`；老项目继续保留 `dev.properties` 也兼容 |
| `DaoException` 编译报“未捕获的异常” | `DaoException extends Throwable`（非 Exception） | Service 方法声明 `throws DaoException`，或 Controller 用 `catch (DaoException e)` |
| 写库失败业务却“成功” | 1.x 吞 SQL 异常 | 2.0 起抛 `TarnDataException`（RuntimeException），接入全局异常处理即可 |
| `delete(entity)` 影响行数异常大 | 1.x 用“所有非空字段”拼 WHERE | 2.0 起严格按主键；请升级并使用 `deleteRows(entity)` |
| 表名/列名报错 | 列名规则是“字段名大写” | 用 `@Etable("表名")`，或改库表为大写列名 |
| log4j 1.x 安全告警 | 老代码使用 `org.apache.log4j.Logger` | 框架已用 `log4j-over-slf4j` 桥接，实际日志走 logback，无需引入 log4j 1.x |
| 启动报「数据源密码解密失败」 | `decrypt: true` 但密码是明文，或密文与 `public-key` 不配对 | 明文场景 `decrypt: false`；加密场景记住**私钥加密、公钥解密** |
| 全局拦截器不生效 | 未指定 `configClass`，或类未实现 `TarnConfig` | Tomcat 用 `web.xml` init-param；独立运行用 `tarn.server.config-class` 或 `run(...)` 四参 |
| 依赖已 install 但 IDE 仍报找不到 | `-pl` 只构建核心，或本地仓库未刷新 | `mvn -pl tarn-framework -am clean install`，IDE 重新导入 |

---

## 9. 与仓库内示例的对应关系

| 文件 | 说明 |
| --- | --- |
| [samples/tarn-maven-demo/pom.xml](../samples/tarn-maven-demo/pom.xml) | 外部工程如何声明依赖、exec 插件、依赖拷贝 |
| [samples/tarn-maven-demo/src/main/java/samples/tarn/maven/entity/Book.java](../samples/tarn-maven-demo/src/main/java/samples/tarn/maven/entity/Book.java) | 实体注解 |
| [.../dao/BookDao.java](../samples/tarn-maven-demo/src/main/java/samples/tarn/maven/dao/BookDao.java) | ORM 写操作 + 参数化查询 |
| [.../service/impl/BookServiceImpl.java](../samples/tarn-maven-demo/src/main/java/samples/tarn/maven/service/impl/BookServiceImpl.java) | `@EService` 注入 |
| [.../controller/BookController.java](../samples/tarn-maven-demo/src/main/java/samples/tarn/maven/controller/BookController.java) | 静态/动态路由、JSON 入参出参 |
| [samples/tarn-maven-demo/sql/schema.sql](../samples/tarn-maven-demo/sql/schema.sql) | 建表脚本 |

> 该示例工程**不在**框架父 POM 的 `<modules>` 中，可用
> `mvn -f samples/tarn-maven-demo/pom.xml clean package` 独立构建，模拟“同事电脑上的业务工程”。
