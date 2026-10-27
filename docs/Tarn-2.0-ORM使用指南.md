# Tarn 2.0 ORM 使用指南（含详细示例）

> 配套文档：[Tarn-2.0-白皮书与升级迁移指南.md](./Tarn-2.0-白皮书与升级迁移指南.md) ｜ **合并版总手册**：[Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md)（ORM 章节见 §9）
> 可运行示例：`tarn-demo/src/main/java/examples/demo/**`（Web 全链路）、`tarn-demo/src/main/java/examples/orm/**`（ORM 专项）
> **Tarn 2.1 新架构**：新增组合式引擎 `Orm.template()`（工厂模式），老继承链 `Q extends DB extends DAOAbstract` 全部保留且内部委托新引擎，二者行为一致（同一实现），老项目零改动。

---

## 1. 设计原则

Tarn 2.0 的 ORM 改造遵循一条硬约束：**数据库语义不变，代码质量升级**。

- 注解语义、列名规则、SQL 生成结果与 1.x 保持一致 → 存量业务零回归风险；
- 内部用 `EntityMapper` 缓存元数据，去掉每次 CRUD 的全量反射；
- 在保留老方法的前提下，新增**参数化**（防注入）、**多表关联**、**方言分页**能力。

### 1.1 元数据解析规则（与老框架完全一致）

| 规则 | 说明 |
| --- | --- |
| 表名 | `@Etable("demo_user")` 的值；不写则用**类名大写**（`DemoUser` → `DEMOUSER`） |
| 列名 | **字段名大写**，不做驼峰转下划线（`deptid` → `DEPTID`，`createTime` → `CREATETIME`） |
| 主键 | `@Ekey(key="id")`，列名为 `key` 大写；未标注时默认列名 `ID` |
| 自增主键 | `@Ekey(isAutoIncrement=true)`：INSERT 不写主键列，主键由数据库生成 |
| 字段跳过 | `@ORMFilter`（完全解除映射）、`@EFilter(true)`（保存/更新时跳过） |
| null 更新 | 默认不更新 null 字段；`@EUpdateNull(true)` 的字段 null 也会写入（用于显式清空） |
| 方法要求 | 字段必须有 `getXxx()`；boolean 可用 `isXxx()`；无 getter 的字段被忽略 |
| 静态/瞬态 | `static`、`transient` 字段自动忽略（2.0 质量修复） |
| 复杂类型 | `List` / `Map` / `Set` / 数组 / 自定义类 → 自动 `JsonKit.toJson()` 后入库 |

> **关键提醒**：老库列名是 `DEPT_ID` 这类下划线形式时，ORM 实体方法无法直接映射。解决方式见 §9.2（别名 / MyBatis / `queryMaps`）。

---

## 2. 注解速查

| 注解 | 目标 | 属性 | 作用 |
| --- | --- | --- | --- |
| `@Etable` | 类 | `value`（表名） | 显式指定表名 |
| `@Ekey` | 字段 | `key`（列名，默认 `id`）、`isAutoIncrement` | 标记主键、是否自增 |
| `@EFilter` | 字段 | `value`（默认 `true`） | `true`：保存/更新跳过该字段（库默认值、审计字段、关联展示字段） |
| `@EUpdateNull` | 字段 | `value`（默认 `true`） | `true`：**更新时 null 也写入**（即清空列） |
| `@ORMFilter` | 字段 | — | 完全解除映射，不参与任何 SQL |
| `@EOrderBy` | 字段 | `name`、`order`（默认 `DESC`） | 老查询链路（`querys` / `pager`）的默认排序 |

> 注意 `@EFilter(true)` 与 `@ORMFilter` 的区别：前者“读得到、写不进”（可查出来展示，但 INSERT/UPDATE 不含该列）；后者“读不到也写不进”。

---

## 3. 基础 CRUD（老 API，全部保留）

`Q extends DB extends DAOAbstract`，**无状态**，可每次 `new Q()`（推荐），也可作为字段持有。

### 3.1 实体定义

```java
@Etable("demo_user")
public class DemoUser {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String name;          // 列 NAME
    private Long deptid;          // 列 DEPTID
    private Integer status;       // 列 STATUS

    @EUpdateNull(false)           // 更新时 null 不覆盖原值
    private String remark;

    @ORMFilter                    // 仅 JOIN 展示，不入库
    private String deptName;

    @EFilter                      // 由数据库默认值填充，保存/更新不写
    private java.util.Date createtime;

    // getter / setter 省略
}
```

### 3.2 新增

```java
Q q = new Q();

DemoUser user = new DemoUser();
user.setName("张三");
user.setDeptid(1L);
user.setStatus(1);
user.setRemark("演示数据");

// 方式一：不关心主键
q.save(user);

// 方式二：需要拿回自增主键
List<Object> keys = q.saveRK(user);
Long newId = ((Number) keys.get(0)).longValue();
```

生成 SQL（自增主键场景）：

```sql
INSERT INTO demo_user (NAME, DEPTID, STATUS, REMARK, ...) VALUES (?, ?, ?, ?, ...)
```

- `CREATETIME` 因 `@EFilter` 不出现；
- 若主键非自增，INSERT 会包含主键列，`saveRK` 返回的集合可能为空。

### 3.3 更新

```java
DemoUser user = new DemoUser();
user.setId(1L);
user.setName("张三-改");
user.setStatus(0);
// remark 为 null → 不更新（默认行为，也是 @EUpdateNull(false) 的效果）

q.update(user);
```

生成 SQL：

```sql
UPDATE demo_user SET NAME = ?, STATUS = ? WHERE ID = ?
```

**显式清空某列**：

```java
user.setRemark("");        // 传空串 → 写入空串
q.update(user);
```

```java
public class DemoUser {
    @EUpdateNull(true)     // null 也写 → 生成 REMARK = NULL
    private String remark;
}
```

**按非主键字段更新**（第二参数指定条件字段名）：

```java
DemoUser cond = new DemoUser();
cond.setName("张三");
cond.setStatus(2);

// 以 NAME 作为定位条件（不是主键）
q.update(cond, "name");
```

> 第二参数传的是**实体字段名**，框架会转成大写列名作为 WHERE 条件。

### 3.4 删除

```java
DemoUser user = new DemoUser();
user.setId(1L);
q.delete(user);          // DELETE FROM demo_user WHERE ID = ?
```

### 3.5 查询

| 方法 | 说明 | 返回 |
| --- | --- | --- |
| `q.findById(User.class, cond)` | 按实体 `@Ekey` 值查询单条 | `Object`（需强转） |
| `q.findById(User.class, cond, "ID,NAME")` | 同上，限定列 | `Object` |
| `q.find(cond)` | 按非空字段作 AND 条件，取首条 | `DAO` / `Object` |
| `q.query(cond)` | 同 `find`，语义化命名 | `Object` |
| `q.querys(cond)` | 按非空字段作 AND 条件，返回列表 | `List` |
| `q.querys(User.class, sql)` | 手写 SQL 查询列表（**字符串拼接有注入风险**） | `List` |
| `q.dao(clazz, cond)` | 查询单条并映射为指定类 | `Object` |
| `q.daos(clazz, cond)` | 查询列表并映射为指定类 | `List` |

```java
// 主键查询
DemoUser cond = new DemoUser();
cond.setId(1L);
DemoUser u = (DemoUser) q.findById(DemoUser.class, cond);

// 条件查询（name / status 均非空 → AND 拼接，值走参数化）
DemoUser where = new DemoUser();
where.setStatus(1);
where.setDeptid(2L);
List<DemoUser> list = (List<DemoUser>) (List<?>) q.querys(where);
```

### 3.6 老分页（`Pager`）保持不变

```java
Model model = new Model("demo_user");
model.filter = "STATUS = 1 ORDER BY ID DESC";       // 老 API：条件直接拼接到 SQL

Pager pager = q.pager(DemoUser.class, model);      // 分页 SQL 自动生成
List<DemoUser> rows = pager.getDatas();
int totalRecord = pager.getTotalRecord();
int totalPage = pager.getTotalPage();
```

> 说明：`dbType` 为新增库型（PostgreSQL / 达梦 / 金仓 / GaussDB / Vastbase / Oracle12c）时老分页自动走方言生成；
> MySQL / MSSQL / Oracle 仍走 1.x 原逻辑，行为不变。

新增代码更推荐 §6 的 `QueryBuilder.page()`。

### 3.7 Tarn 2.1 写操作 API（推荐新代码使用）

老的 `save / update / delete / saveRK` 签名与语义全部保留（零改动兼容），2.1 新增了**有返回值、异常可感知**的写 API：

| 新 API（`Q` / `DB`） | 说明 | 返回 |
| --- | --- | --- |
| `insert(entity)` | 新增，纯主键/字段驱动，自增主键不写入 SQL | 受影响行数 |
| `insertReturnKey(entity)` | 新增并把数据库生成的主键**回填到实体** | 主键对象 |
| `updateRows(entity)` | 按主键更新（WHERE 只认主键），null 跳过 | 受影响行数 |
| `deleteRows(entity)` | 按主键删除（WHERE 只认主键） | 受影响行数 |
| `batchInsert(List<?>)` | 批量新增，内部事务，失败整体回滚 | 成功条数 |
| `beginTransaction() / commit() / rollback()` | 编程式事务 | — |

```java
Q q = new Q();

// 新增 + 主键回填（推荐）
Long id = ((Number) q.insertReturnKey(user)).longValue();
// user.getId() 此时已经是数据库生成的主键

// 更新：只更新非 null 字段，返回真正受影响的行数
int rows = q.updateRows(user);
if (rows == 0) {
    // 主键不存在，或提交的字段与原值完全一致
}

// 删除：严格按主键
q.deleteRows(user);

// 批量
q.batchInsert(list);
```

#### 2.1 与老版本的行为差异（升级须知）

| 场景 | 老行为（1.x） | 2.1 行为 |
| --- | --- | --- |
| SQL 执行失败 | `LogUtil.error` 打印后**静默忽略**，业务以为成功 | 抛 `TarnDataException`（RuntimeException，可被全局异常处理捕获） |
| `delete(entity)` / `ddel(entity)` | 用“所有非空字段”拼 WHERE，主键为空时会**误删全表** | 严格按主键，主键为空直接抛 `TarnDataException` |
| 自增主键 | 不回填，需自己查 `LAST_INSERT_ID()` | `insertReturnKey` 自动回填实体主键 |
| 更新/删除 | 无返回值 | `updateRows` / `deleteRows` 返回受影响行数 |
| 更新 SQL 生成 | 反射拼串 | 统一走 `EntitySql`（纯函数，可单测），参数全部 `?` 绑定 |

> `save / update` 等老方法仍为 `void`，只是失败时不再吞异常——老项目换 jar 无需改代码，但生产环境建议接入全局异常处理，避免异常冒泡成 500 白页。

#### 架构（Tarn 2.1：组合 + 工厂，老继承链薄门面化）

2.1 把「实体写语义 + JDBC 原语 + 事务」的**唯一实现**收敛到组合式引擎 `OrmTemplate`，
老继承链 `Tarn extends Q extends DB extends DAOAbstract` 保留全部签名，但内部逐方法委托同一实现——
**不存在两套行为**，无论从哪个入口调用，语义与异常都一致：

```text
                     ┌─────────────────────────────────────────────┐
   新代码（推荐）      │  Orm.template() 工厂入口 → OrmTemplate 引擎   │
                     │  （组合 ConnectionFactory，不继承任何类）      │
                     └──────────────────┬──────────────────────────┘
                                        │ 单一实现（EntitySql 生成 SQL）
   老代码（零改动兼容）                    ▼
   Tarn → Q → DB → DAOAbstract   ──委托──►  OrmTemplate.shared()
   （全部老签名保留）                          │
                                              ▼
                                     ConnectionFactory（连接池 / 线程事务）

   Tarn 还额外提供静态配置与分页上下文（isDebug / sysDevConf / PageContext / version()）
```

- **为什么这样改**：老链 4 层 `extends` 导致 `Tarn` 暴露了全部 100+ 个 ORM 方法（API 面污染）、
  语义混杂在多层重复实现；组合式引擎把「能做什么」（门面）与「怎么做」（引擎）分离，
  新代码不再需要继承任何类即可获得完整 ORM 能力。
- 老入口 `new Q()` / `new Tarn()` 可继续使用；`DAO` 是查询结果行容器（`CaseInsensitiveMap`），不是访问入口。

### 3.8 组合式引擎（Tarn 2.1 推荐）：`Orm.template()`

新代码**无需继承任何类**，一行工厂调用获得完整 ORM 能力：

```java
import cc.ruixing.framework.tarn.orm.Orm;
import cc.ruixing.framework.tarn.orm.OrmTemplate;

OrmTemplate orm = Orm.template();   // 全局共享单例，线程安全

// ---- 实体写 ----
orm.insert(book);                              // 返回受影响行数
Object key = orm.insertReturnKey(book);        // 主键回填到实体
int rows = orm.updateRows(book);               // 按主键更新，null 跳过
orm.deleteRows(book);                          // 按主键删除
orm.batchInsert(bookList);                     // 单事务批量，失败整体回滚

// ---- SQL / 查询 ----
orm.exec(SqlBuilder.create("UPDATE demo_book SET STOCK = #{s} WHERE ID = #{i}")
        .bind("s", 8).bind("i", 1L).build());
List<Book> list = orm.query(ps, Book.class);   // 实体列表
Book one = orm.queryOne(ps, Book.class);       // 单条，无结果返回 null
List<Map<String, Object>> maps = orm.queryMaps(ps);   // 任意结果集
int total = orm.count(ps);                     // SELECT COUNT(0) ...

// ---- 声明式事务（推荐） ----
orm.inTransaction(new TransactionCallback<DemoOrder>() {
    public DemoOrder doInTransaction(OrmTemplate orm) {
        orm.exec(update);          // 扣库存
        orm.insertReturnKey(order); // 生成订单
        return order;              // 正常返回 → 提交
    }                              // 抛任何异常 → 自动回滚并包成 TarnDataException
});
```

`OrmTemplate` 与 `Q` 的 API 几乎一一对应（少掉老 Statement 路径），差异只在：
**所有异常统一为非受检 `TarnDataException`**，无需在签名上声明 `throws DaoException`。

> 与 `Q` 的关系：`Q` 里的 `insert / insertReturnKey / updateRows / deleteRows / batchInsert /
> beginTransaction / commit / rollback` 全部一行委托 `OrmTemplate.shared()`。
> 团队约定建议：**新模块一律 `Orm.template()`；存量模块保持 `Q` 不动，按方法级替换逐步迁移。**

新增代码只需与 `Q` 或 `Orm.template()` 打交道；需要自定义 SQL 时用 `SqlBuilder`（参数绑定），不建议再手拼字符串。

#### 3.8.1 实体写 API 逐一示例

```java
OrmTemplate orm = Orm.template();

// ===== insert：新单插入，不关心主键 =====
Book b1 = new Book();
b1.setTitle("Tarn 实战"); b1.setAuthor("dfr"); b1.setStock(10);
int rows = orm.insert(b1);             // rows == 1

// ===== insertReturnKey：需要拿回主键（自动回填到实体） =====
Book b2 = new Book();
b2.setTitle("Spring Boot 实战"); b2.setAuthor("dfr"); b2.setStock(5);
Object key = orm.insertReturnKey(b2);
Long newId = ((Number) key).longValue();  // 或直接 b2.getId() 已经被回填

// ===== updateRows：按主键更新，null 字段跳过 =====
Book b = new Book();
b.setId(newId); b.setStock(99);
int updated = orm.updateRows(b);        // generated SQL: UPDATE demo_book SET STOCK = ? WHERE ID = ?
// 注意：TITLE / AUTHOR 没设值（null）→ 不参与 SET 子句，不会覆盖原值

// ===== deleteRows：严格按主键删除 =====
Book del = new Book(); del.setId(newId);
int deleted = orm.deleteRows(del);       // WHERE 只有 ID = ?，不会误删全表
// 主键为空 → 抛 IllegalArgumentException（防误删）
```

#### 3.8.2 批量写 + 事务：`batchInsert` / `inTransaction`

```java
List<Book> books = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    Book b = new Book();
    b.setTitle("批量导入-" + i); b.setAuthor("system"); b.setStock(i);
    books.add(b);
}

// 方式一：batchInsert 自带单事务，失败整体回滚
int success = orm.batchInsert(books);
// 如果某一条 insert 失败 → 自动回滚前面所有，抛 TarnDataException

// 方式二：声明式事务（推荐：正常返回 → 提交；抛异常 → 自动回滚）
Order order = new Order(); order.setUserId(1L); order.setAmount(99);
List<OrderItem> items = ...;
orm.inTransaction(new TransactionCallback<Order>() {
    public Order doInTransaction(OrmTemplate orm) {
        // 条件更新防超卖：AND STOCK >= #{qty}，受影响行数为 0 即库存不足
        int rows = orm.exec(SqlBuilder
                .create("UPDATE demo_book SET STOCK = STOCK - #{qty} WHERE ID = #{id} AND STOCK >= #{qty}")
                .bind("qty", 2).bind("id", 1L).build());
        if (rows == 0) throw new IllegalStateException("库存不足");

        orm.insertReturnKey(order);
        orm.batchInsert(items);
        return order;   // 正常返回 → 事务提交
    }
});
// 抛出的任何 RuntimeException 都会触发回滚，并包成 TarnDataException 抛出

// 方式三：编程式事务（需要更细粒度控制时）
orm.beginTransaction();
try {
    orm.insert(order);
    orm.batchInsert(items);
    orm.commit();
} catch (Exception e) {
    orm.rollback();
    throw new TarnDataException("下单失败", e);
}
```

#### 3.8.3 参数化写：`exec` / `execReturnKeys`

`insertReturnKey` / `updateRows` / `deleteRows` 只能用于**表结构完全被实体注解描述**的场景；
需要自定义 INSERT/UPDATE/DELETE（多表 UPDATE、条件更新、批量 INSERT）时用参数化 SQL：

```java
// 条件更新：扣库存（带防超卖条件）
int updated = orm.exec(SqlBuilder.create(
        "UPDATE demo_book SET STOCK = STOCK - #{qty} WHERE ID = #{id} AND STOCK >= #{qty}")
        .bind("qty", 2).bind("id", 1L).build());

// 批量 INSERT + 拿回自增主键（MySQL 单条 INSERT 支持，批量用 batchInsert 更高效）
ParameterizedSql ps = SqlBuilder.create(
        "INSERT INTO demo_book (TITLE, AUTHOR, STOCK) VALUES (#{t}, #{a}, #{s})")
        .bind("t", "新书").bind("a", "dfr").bind("s", 5).build();
List<Object> keys = orm.execReturnKeys(ps);
// keys.get(0) 是自增主键，new Book().getClass() 时强转 Number
```

#### 3.8.4 查询四件套对比

| API | 返回 | 适用场景 |
| --- | --- | --- |
| `orm.query(ps, Book.class)` | `List<Book>` | 表结构与实体注解对齐的单表查询（最常用） |
| `orm.queryOne(ps, Book.class)` | `Book` / null | 只取第一条，无结果返回 null |
| `orm.queryMaps(ps)` | `List<Map<String,Object>>` | 多表 JOIN、聚合函数、任意列组合 |
| `orm.query(ps, RowMapper)` | `List<Object>`（按 RowMapper 自定义） | 需要复杂行映射（JSON 列反序列化、关联对象组装等） |

```java
// 场景 1：单表列表（实体注解完整）
ParameterizedSql listPs = QueryBuilder
        .select("ID, TITLE, AUTHOR, STOCK")
        .from("demo_book")
        .where("STOCK >= #{s}").bind("s", 3)
        .orderBy("ID", true).build();
List<Book> books = orm.query(listPs, Book.class);

// 场景 2：单条查询（用 count 前置 + limit 1）
ParameterizedSql onePs = SqlBuilder
        .create("SELECT ID, TITLE, AUTHOR FROM demo_book WHERE ID = #{id}")
        .bind("id", 1L).build();
Book book = orm.queryOne(onePs, Book.class);
// book == null → 没找到；book.getTitle() → 安全调用

// 场景 3：多表 JOIN + 聚合 → queryMaps
ParameterizedSql joinPs = QueryBuilder
        .select("o.ID AS ORDER_ID, o.USER_ID, SUM(oi.PRICE * oi.QTY) AS TOTAL")
        .from("demo_order o")
        .leftJoin("demo_order_item oi").on("o.ID = oi.ORDER_ID")
        .where("o.STATUS = ?", 1)
        .groupBy("o.ID, o.USER_ID")
        .page(1, 10)
        .build();
List<Map<String, Object>> rows = orm.queryMaps(joinPs);
// rows.get(0).get("ORDER_ID") / .get("TOTAL") —— 注意别名与列名大小写（Oracle/达梦可能大写）

// 场景 4：自定义 RowMapper（JSON 字段反序列化）
ParameterizedSql jsonPs = SqlBuilder.create("SELECT ID, TAGS FROM demo_note WHERE ID = #{id}")
        .bind("id", noteId).build();
List<DemoNote> notes = orm.query(jsonPs, new RowMapper() {
    public Object mapRow(ResultSet rs) throws SQLException {
        DemoNote n = new DemoNote();
        n.setId(rs.getLong("ID"));
        String tagsJson = rs.getString("TAGS");  // VARCHAR 存的 JSON
        n.setTags(JsonKit.parseArray(tagsJson, String.class));  // 反序列化
        return n;
    }
});
```

#### 3.8.5 统计 + 分页

```java
// 统计：SELECT COUNT(0) 必须写在 SQL 里
int total = orm.count(SqlBuilder.create("SELECT COUNT(0) FROM demo_book WHERE STOCK >= ?", 3)
        .build());

// 分页：用 QueryBuilder.page(absPage, size) 自动走当前方言
// MySQL → LIMIT ?, ?   Oracle12c → OFFSET ? ROWS FETCH NEXT ? ROWS ONLY   达梦/金仓/GaussDB 同理
List<Book> page1 = orm.query(QueryBuilder
        .select("*").from("demo_book")
        .where("STOCK >= #{s}").bind("s", 3)
        .orderBy("ID", true)
        .page(1, 20)
        .build(), Book.class);

// 老 Pager 兼容：QueryBuilder.page 的返回值带 SQL + 参数，Pager 可以继续用
Model model = new Model("demo_book");
model.filter = "STOCK = 1 ORDER BY ID DESC";
Pager pager = q.pager(DemoUser.class, model);   // 老 API，行为不变
```

#### 3.8.6 事务三路线对比

```java
// ① 声明式（推荐：正常返回=提交，抛异常=回滚）
orm.inTransaction(new TransactionCallback<Order>() {
    public Order doInTransaction(OrmTemplate orm) {
        orm.exec(updateStock);
        orm.insertReturnKey(order);
        orm.batchInsert(items);
        return order;   // 返回值被 inTransaction 透出
    }
});  // 抛出任何 RuntimeException → 自动回滚 + 包成 TarnDataException

// ② 编程式（需要手动控制时）
try {
    orm.beginTransaction();
    orm.exec(updateStock);
    orm.insert(order);
    orm.commit();
} catch (Exception e) {
    orm.rollback();
    throw new TarnDataException("下单失败", e);
}
// 注意：beginTransaction/commit/rollback 是线程内生效的静态语义
// 同一线程不能交叉嵌套 inTransaction 与编程式，也不能与 MyBatis 事务混用

// ③ 老 API（保留，行为不变）
q.transOpen();
try {
    q.exec(ps);
    q.insert(user);
    q.transCommit();
} catch (Exception e) {
    q.transRollback();
    throw new TarnDataException("失败", e);
}
```

---

## 4. 实体进阶

### 4.1 `@EFilter`：只读或只写字段

```java
@EFilter                       // 等价 @EFilter(true)
private java.util.Date createtime;    // 数据库默认值填充，保存/更新不写
```

如果需要“保存时写、更新时不写”，请拆成两个实体或改用参数化 SQL（`@EFilter` 不区分 INSERT / UPDATE）。

### 4.2 `@ORMFilter`：VO / 关联展示字段

```java
@ORMFilter
private String deptName;       // 来自 JOIN 查询，不入库
```

`@ORMFilter` 字段可以被 `queryEntityList` / `query(ps, Class)` 通过列别名填充（见 §5.2）。

### 4.3 复杂类型字段 → JSON 列（2.1 支持完整往返）

```java
public class DemoNote {
    private List<String> tags;             // → JSON 数组: ["orm","tarn"]
    private Map<String, Object> meta;      // → JSON 对象: {"creator":"dfr"}
    private Attrs attrs;                   // 自定义类 → JSON 对象
}
```

- 保存时：`EntityMapper` 检测到复杂类型 → `JsonKit.toJson(value)` 入库；
- 读取时（**2.1 修复后原生支持**）：`queryEntityList` / `query(ps, Class)` 会按字段声明类型自动反序列化——
  `List` / 数组 → `JsonKit.parseArray`（元素取泛型实参），`Map` → `JsonKit.parse`，`Set` → `LinkedHashSet` 包装，
  自定义类 → `JsonKit.fromJson`；
- 数据库列类型建议：`VARCHAR(2000)` / `TEXT` / `CLOB`（`queryEntityList` 已处理 CLOB）。

> 2.1 修复的老 bug：`List<String>` 字段回读时误把 `java.lang.Class` 当元素类型传给 JSON 引擎，
> 导致反序列化必然失败且异常被静默吞掉（字段永远为 null）。已在 `ActionInvocation.invokeSetORM` 修正，
> 并通过 `examples.orm.OrmUsageExamples#e1ComplexFieldJson` 用 MySQL 实测 List / Map / 自定义类三类字段的完整往返。

### 4.4 `@EOrderBy`：老查询默认排序

```java
@EOrderBy(name = "CREATETIME", order = "DESC")
private java.util.Date createtime;
```

仅对老链路（`querys` / `pager` 基于实体解析时）生效；新 API 请显式 `orderBy(...)`。

### 4.5 实体命名建议

- 字段用**单段小写**（`deptid`、`username`），避免驼峰导致列名与库不一致；
- 必须用驼峰属性时，加 `@ORMFilter` 的展示字段并用别名查询，或用 MyBatis Mapper。

---

## 5. 多表关联查询（2.0 新增）

### 5.1 方式一：`QueryBuilder` + `queryMaps`（最直观）

```java
ParameterizedSql ps = QueryBuilder
        .select("u.ID, u.NAME, u.DEPTID, u.STATUS, d.NAME AS DEPTNAME")
        .from("demo_user u")
        .leftJoin("demo_dept d").on("u.DEPTID = d.ID")
        .where("u.STATUS = #{status}").bind("status", 1)
        .orderBy("u.ID", false)
        .page(1, 10)
        .build();

List<Map<String, Object>> rows = q.queryMaps(ps);
```

- `page(absPage, size)`：按**当前数据库方言**生成分页（MySQL `LIMIT`、Oracle `ROWNUM`、达梦 / 金仓 / GaussDB 自动适配）；
- 返回值 key 为列别名（Oracle 等库可能返回大写，取值时建议忽略大小写或统一 `AS` 别名）；
- ON 条件与表引用走白名单校验，值全部参数绑定。

### 5.2 方式二：`QueryBuilder` + `query(ps, Class)`（映射回实体）

```java
ParameterizedSql ps = QueryBuilder
        .select("u.ID, u.NAME, u.DEPTID, u.STATUS, d.NAME AS DEPTNAME")
        .from("demo_user u")
        .leftJoin("demo_dept d").on("u.DEPTID = d.ID")
        .where("u.STATUS = ?", 1)
        .orderBy("u.ID", false)
        .build();

@SuppressWarnings("unchecked")
List<DemoUser> list = (List<DemoUser>) (List<?>) q.query(ps, DemoUser.class);
```

映射规则：

1. 列别名 → 实体字段（忽略大小写，`DEPTNAME` → `deptName`）；
2. 命中 `@ORMFilter` 字段时也会被填充（`deptName` 就是这种用法）；
3. 支持 Oracle/达梦/金仓返回的大写列标签（2.0 修复，老逻辑会静默丢值）。

### 5.3 方式三：`SqlBuilder` 手写 SQL + 参数绑定

```java
// 单表
ParameterizedSql ps = SqlBuilder
        .select("ID, NAME, DEPTID, STATUS, REMARK")
        .from("demo_user")
        .where("STATUS = #{status}").bind("status", status)
        .orderBy("ID", false)
        .limit(offset, size)            // 注意：limit 是字面量，不参与方言
        .build();

@SuppressWarnings("unchecked")
List<DemoUser> list = (List<DemoUser>) (List<?>) q.query(ps, DemoUser.class);

// 更新
int rows = q.exec(SqlBuilder
        .create("UPDATE demo_user SET STATUS = #{status} WHERE ID = #{id}")
        .bind("status", 0)
        .bind("id", 1L)
        .build());

// 统计
int total = q.count(SqlBuilder.create("SELECT COUNT(0) FROM demo_user")
        .where("STATUS = ?", 1)
        .build());
```

// JOIN 变体对比（QueryBuilder 提供四种 JOIN 变体 + 通用 join）
// === innerJoin：只保留两边都匹配的行 ===
qb = QueryBuilder.select("u.*, d.NAME AS DEPTNAME")
        .from("demo_user u").innerJoin("demo_dept d").on("u.DEPTID = d.ID")
        .build();

// === leftJoin：保留左表全部（即使右表无匹配）—— 最常用 ===
qb = QueryBuilder.select("u.*, d.NAME AS DEPTNAME")
        .from("demo_user u").leftJoin("demo_dept d").on("u.DEPTID = d.ID")
        .build();

// === rightJoin：保留右表全部 ===
qb = QueryBuilder.select("u.*, d.NAME AS DEPTNAME")
        .from("demo_user u").rightJoin("demo_dept d").on("u.DEPTID = d.ID")
        .build();

// === 通用 join：配合第三个参数写自己的 JOIN 关键字（如 "INNER JOIN" 或 "LEFT OUTER JOIN"） ===
qb = QueryBuilder.select("u.*, d.NAME AS DEPTNAME")
        .from("demo_user u").join("demo_dept d", "LEFT OUTER JOIN").on("u.DEPTID = d.ID")
        .build();

// ON 条件支持位置绑定（on(condition, ...positionalBinds)）
ParameterizedSql join2 = QueryBuilder
        .select("o.ID AS ORDER_ID, o.USER_ID, o.STATUS")
        .from("demo_order o")
        .innerJoin("demo_user u")
        .on("o.USER_ID = u.ID AND u.STATUS = ?", 1)     // ON 条件带参数绑定
        .where("o.STATUS = ?", 2)
        .build();

// groupBy + having：聚合查询 + 分组筛选
ParameterizedSql agg = QueryBuilder
        .select("DEPTID, COUNT(0) AS CNT, AVG(SALARY) AS AVG_SALARY")
        .from("demo_user")
        .where("STATUS = ?", 1)
        .groupBy("DEPTID")
        .having("COUNT(0) >= ?", 5)                    // HAVING 也能参数绑定
        .orderBy("AVG_SALARY", false)
        .build();
List<Map<String, Object>> aggRows = q.queryMaps(agg);
// aggRows.get(0).get("DEPTID") / .get("CNT") / .get("AVG_SALARY")

```

`SqlBuilder` 两种占位符：

| 占位 | 配套方法 | 特点 |
| --- | --- | --- |
| `?` | `where/and/or/append/having` 的可变参数 | 按位置绑定 |
| `#{name}` | `bind(name, value)` | 按名绑定，未绑定会抛 `IllegalArgumentException` |

### 5.4 方式四：MyBatis Mapper（复杂 SQL / 已熟悉 MyBatis 的团队）

```java
public interface DemoUserMapper {

    @Select("SELECT ID AS id, NAME AS name, DEPTID AS deptid, STATUS AS status, "
          + "REMARK AS remark FROM demo_user WHERE ID = #{id}")
    DemoUser selectById(@Param("id") Long id);

    @Insert("INSERT INTO demo_user (NAME, DEPTID, STATUS, REMARK) "
          + "VALUES (#{name}, #{deptid}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DemoUser user);
}
```

```java
// 需 tarn.mybatis.enabled: true
DemoUserMapper mapper = MybatisKit.getMapper(DemoUserMapper.class);
DemoUser u = mapper.selectById(1L);
```

- 首次 `getMapper` 自动注册接口，无需 XML / `mybatis-config.xml`；
- 会话绑定当前线程，请求结束由 `TarnDispatcherFilter` 自动 `close()`；
- 非 Web 线程用完后手动 `MybatisKit.close()`。

### 5.5 方式五：自定义 VO + 显式 SQL

```java
public class UserDeptVO {
    private Long id;
    private String name;
    private String deptName;   // 列别名 DEPTNAME
    // getter/setter
}

List<UserDeptVO> list = (List<UserDeptVO>) (List<?>) q.queryEntityList(
        "SELECT u.ID, u.NAME, d.NAME AS DEPTNAME "
      + "FROM demo_user u LEFT JOIN demo_dept d ON u.DEPTID = d.ID "
      + "WHERE u.STATUS = ?",
        java.util.Arrays.<Object>asList(1),
        UserDeptVO.class);
```

---

## 6. 参数化 DAO API（推荐新代码使用）

| 方法 | 签名 | 说明 |
| --- | --- | --- |
| `exec` | `int exec(ParameterizedSql ps)` | INSERT/UPDATE/DELETE，值绑定，异常抛 `DaoException` |
| `query` | `List<?> query(ParameterizedSql ps, RowMapper mapper)` | 自定义行映射 |
| `query` | `List<?> query(ParameterizedSql ps, Class clazz)` | 映射为实体列表 |
| `queryEntityList` | `List<Object> queryEntityList(String sql, List<Object> params, Class clazz)` | 原生 `?` SQL + 实体映射 |
| `queryMaps` | `List<Map<String,Object>> queryMaps(ParameterizedSql ps)` | 任意结果集 → Map |
| `count` | `int count(ParameterizedSql ps)` | 要求 `SELECT COUNT(0) ...` |

`RowMapper` 用法：

```java
List<?> list = q.query(ps, new RowMapper() {
    public Object mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", rs.getLong("ID"));
        m.put("name", rs.getString("NAME"));
        return m;
    }
});
```

**老 API 与新 API 的行为差异（重要）**

| | 老 API（`exec(String)` / `find` / `pager`） | 新 API（`exec(ps)` / `query(ps)`） |
| --- | --- | --- |
| 异常 | 多数 `LogUtil.error` 后返回 0 / null，**静默失败** | 抛 `DaoException`，调用方必须处理 |
| 参数 | 字符串拼接或 `Map` 替换 | `PreparedStatement` 绑定 |
| 分页 | `LIMIT` 硬编码或游标 | 方言自动生成 |

### 6.2 命名参数 SQL（`#{name}` 语法）

老 `Q` 里的 `dsexec` / `dsquerys` 方法接受 `#{name}` 占位 + `Map<String,Object>` 参数。
2.1 把这条链路**收敛到 OrmTemplate 静态工厂**，并扩展出完整的绑定语义表：

```java
// 方式一：Orm.named() 把 #{name} SQL + Map 参数 → ParameterizedSql（走 PreparedStatement 绑定）
//          默认 strict=true（缺参抛 IllegalArgumentException）、likeAware=false
Map<String, Object> params = new HashMap<>();
params.put("id", 1L);
params.put("status", 1);

ParameterizedSql ps = OrmTemplate.named(
        "SELECT * FROM demo_book WHERE ID = #{id} AND STATUS = #{status}", params);
List<Book> list = q.query(ps, Book.class);

// 方式二：显式 strict / likeAware（三参数版本）
// strict=true → Map 里缺 #{id} 会立即抛 IllegalArgumentException（推荐）
// strict=false → 缺 #{id} 自动绑定 null（不推荐，容易漏查）
// likeAware=true → 占位符前有 "like " 时自动把值包成 "%#{id}%"（like 语法糖）
ParameterizedSql psLike = OrmTemplate.named(
        "SELECT * FROM demo_book WHERE TITLE LIKE #{q}",
        Collections.singletonMap("q", "Tarn"),
        false, true);
// likeAware=true 生成的 SQL: LIKE %Tarn%  ← 值两边自动加 %，原始 SQL 里不要写 %
```

命名参数专用 DAO 入口（`Q` 和 `OrmTemplate` 都有）：

| 方法 | 说明 |
| --- | --- |
| `execNamed(namedSql, params)` | INSERT/UPDATE/DELETE，默认 strict |
| `execNamed(namedSql, params, strict)` | 指定 strict 行为 |
| `execNamedReturnKeys(namedSql, params)` | 执行后拿回自增主键列表 |
| `queryNamedLegacy(namedSql, params, Class)` | 映射为实体列表（老 API 保留，内部走 `queryEntityList`） |
| `queryOneNamed(namedSql, params, RowMapper)` | 单条查询 + 自定义 RowMapper |
| `queryOneBean(namedSql, params, Class<T>)` | 单条查询 → 实体 |

```java
// execNamed：条件更新（strict=true，缺 #{q} 立即报错）
int updated = q.execNamed(
        "UPDATE demo_book SET STOCK = STOCK - #{q} WHERE ID = #{id} AND STOCK >= #{q}",
        new HashMap<String, Object>() {{ put("q", 2); put("id", 1L); }});
// 或用 OrmTemplate.execNamed（同一实现）
int updated2 = OrmTemplate.execNamed(sql, params);

// execNamedReturnKeys：批量 INSERT + 拿回自增主键
List<Object> keys = q.execNamedReturnKeys(
        "INSERT INTO demo_book (TITLE, STOCK) VALUES (#{t}, #{s})",
        new HashMap<String, Object>() {{ put("t", "新书"); put("s", 5); }});
Long newId = ((Number) keys.get(0)).longValue();

// queryOneNamed：单条 + 自定义 RowMapper
Book book = (Book) q.queryOneNamed(
        "SELECT * FROM demo_book WHERE ID = #{id}",
        Collections.singletonMap("id", newId),
        new RowMapper() {
            public Object mapRow(ResultSet rs) throws SQLException {
                Book b = new Book();
                b.setId(rs.getLong("ID"));
                b.setTitle(rs.getString("TITLE"));
                return b;
            }
        });
```

**三个行为对照（同一份 SQL，strict/likeAware 组合差异）**：

```java
String sql = "SELECT * FROM demo_book WHERE STATUS = #{s} AND TITLE LIKE #{q}";
Map<String, Object> params = Collections.singletonMap("s", 1);  // 缺 #{q}

// strict=true likeAware=false → 抛 IllegalArgumentException：Missing named parameter [q]
// strict=false likeAware=false → #{q} 绑定为 null，最终 WHERE TITLE LIKE NULL（不匹配任何行）
// strict=false likeAware=true  → #{q} 绑定为 null 但被包成 "%null%"（更糟，强烈不推荐）
```

> 团队建议：**业务代码默认 strict=true**，只有确实需要「缺参忽略条件」的场景才显式 `strict=false`，
> 并在 SQL 里用 `(#{q} IS NULL OR TITLE LIKE #{q})` 这种显式逻辑。

### 6.3 EntityMapper / EntitySql：作为工具类直接调用

`OrmTemplate` 内部用 `EntityMapper` / `EntitySql` 生成 SQL 并映射行——
这两个类是 **public** 的，业务代码可直接调用，适合写通用 DAO 或自己拼 SQL：

```java
import cc.ruixing.framework.tarn.orm.EntityMapper;
import cc.ruixing.framework.tarn.orm.EntitySql;
import cc.ruixing.framework.tarn.orm.EntityMapper.Meta;

// === EntityMapper：元数据查询 ===
Meta meta = EntityMapper.meta(Book.class);
// meta.tableName       → "demo_book"
// meta.pkName           → "id"（小写）
// meta.pkColumn         → "ID"（大写列名）
// meta.columnMap        → {TITLE → Column, AUTHOR → Column, STOCK → Column, ...}（字段名 → 列信息）
// Column.col            → 大写列名
// Column.field          → 原字段名
// Column.isAutoIncrement / isFilter / isORMFilter / isUpdateNull

// 通用 DAO 里拿主键
String pkCol = EntityMapper.primaryKeyName(Book.class);  // "id"
String table = EntityMapper.tableName(Book.class);      // "demo_book"

// 自己写 SQL 时拼 WHERE 主键条件
Book b = new Book(); b.setId(1L);
Map<String, Object> colValueMap = EntityMapper.toColumnValueMap(b, false);  // 不含主键
// → {TITLE=..., AUTHOR=..., STOCK=...}

// 反射读/写主键（例如把数据库生成的主键回填到实体）
Object pkValue = EntityMapper.readPrimaryKeyValue(b);   // Long(1)
EntityMapper.writePrimaryKey(b, 1L);                    // b.setId(1L)

// 查询结果列名 → 实体字段名（大小写不敏感，用于 queryEntityList 映射）
String fieldName = EntityMapper.resolveFieldName(Book.class, "TITLE");  // "title"

// === EntitySql：把实体 → 参数化 SQL ===
Book b2 = new Book(); b2.setTitle("新书"); b2.setAuthor("dfr"); b2.setStock(10);

ParameterizedSql insert = EntitySql.insert(b2);
// 生成: INSERT INTO demo_book (TITLE, AUTHOR, STOCK) VALUES (?, ?, ?)  + 参数列表
//       自增主键不在 INSERT 里；@EFilter 字段不在 INSERT 里

b2.setId(1L); b2.setTitle("新书-改");   // 其他字段不动
ParameterizedSql update = EntitySql.update(b2);
// 生成: UPDATE demo_book SET TITLE = ? WHERE ID = ?
//       null 字段自动跳过（默认行为）；@EUpdateNull(true) 的字段例外

Book del = new Book(); del.setId(1L);
ParameterizedSql delete = EntitySql.deleteByPrimaryKey(del);
// 生成: DELETE FROM demo_book WHERE ID = ?
```

常见用法：写通用 DAO 基类（让 `OrmTemplate` + `EntityMapper` 自动适配任意实体）、
批量处理时自己拼复杂 SQL（利用 `EntitySql` 保证与实体注解一致，手写不会错）、
写 MyBatis / 其他持久层之间的桥接（拿表名和主键列名）。

---

## 7. 方言与分页

### 7.1 支持的数据库

| 配置值（`tarn.datasource.type` / 老 `DBType`） | 数据库 | 分页实现 |
| --- | --- | --- |
| `MYSQL` | MySQL / MariaDB | `LIMIT offset, size` |
| `POSTGRESQL` | PostgreSQL | `LIMIT size OFFSET offset` |
| `ORACLE` | Oracle 11g 及以下 | `ROWNUM` 嵌套 |
| `ORACLE12C` | Oracle 12c+ | `OFFSET ... FETCH` |
| `MSSQL` | SQL Server | `ROW_NUMBER()` / `TOP` |
| `DM` | 达梦 DM8 | `LIMIT`/`ROWNUM` 适配 |
| `KINGBASE` | 人大金仓 KingbaseES | `LIMIT ... OFFSET` |
| `GAUSSDB` | openGauss / GaussDB | `LIMIT ... OFFSET` |
| `VASTBASE` | 海量 Vastbase | `LIMIT ... OFFSET` |

### 7.2 配置与自动识别

```yaml
tarn:
  datasource:
    type: DM          # 显式指定
    url: jdbc:dm://127.0.0.1:5236/DB
    decrypt: false    # 2.1：true 时建池前用 Druid ConfigTools 解密 password
    # public-key: "..."   # 自定义公钥；留空用内置默认公钥（私钥加密、公钥解密）
```

> 数据源密码加密的密钥生成步骤与三种场景验证见《全书》§4.3。
> 解密失败会直接抛 `TarnException` 中止启动，不会带着错误密码进入运行期。

- 未配置 `type` 时按 JDBC URL 前缀自动识别（`jdbc:dm` / `jdbc:kingbase` / `jdbc:opengauss` / `jdbc:vastbase` ...）；
- 都识别不到时兜底 MySQL 方言；
- 自定义方言：实现 `Dialect` 后 `DialectFactory.registerDialect(new XxxDialect())`。

### 7.3 分页写法

```java
// 推荐：方言分页（absPage 从 1 开始）
QueryBuilder.select("u.ID, u.NAME")
        .from("demo_user u")
        .where("u.STATUS = ?", 1)
        .orderBy("u.ID", false)
        .page(2, 20)                 // 第 2 页，每页 20
        .build();

// 需要总条数时单独 count
int total = q.count(SqlBuilder.create("SELECT COUNT(0) FROM demo_user")
        .where("STATUS = ?", 1).build());
```

> `SqlBuilder.limit(offset, size)` 生成的是固定 `LIMIT` 字面量，**跨库不安全**；需要跨库时用 `QueryBuilder.page()`。

---

## 8. 事务

### 8.1 声明式事务（Tarn 2.1 推荐：`inTransaction`）

正常返回即提交，抛出任何异常自动回滚并包装为非受检 `TarnDataException`——
业务代码只写成功路径，不再需要 try/commit/rollback 样板：

```java
Orm.template().inTransaction(new TransactionCallback<DemoOrder>() {
    public DemoOrder doInTransaction(OrmTemplate orm) {
        // 条件更新防超卖：AND STOCK >= #{qty}，受影响行数为 0 即库存不足
        int rows = orm.exec(SqlBuilder
                .create("UPDATE demo_book SET STOCK = STOCK - #{qty}"
                        + " WHERE ID = #{id} AND STOCK >= #{qty}")
                .bind("qty", 2).bind("id", 1L).build());
        if (rows == 0) {
            throw new IllegalStateException("库存不足，无法下单");   // → 自动回滚
        }
        DemoOrder order = new DemoOrder();
        order.setOrderNo("SO-2026-0001");
        orm.insertReturnKey(order);                                // 与扣库存同事务
        return order;                                              // 正常返回 → 提交
    }
});
```

已用 MySQL 实测：提交（两笔写全部生效）与回滚（抛异常后零残留）两个方向均正确，
完整可运行代码见 `examples.orm.OrmUsageExamples` 的 E4 / E5 场景。

### 8.2 编程式事务

`Q` / `OrmTemplate` 都提供 `beginTransaction() / commit() / rollback()`（同一线程连接实现）：

```java
OrmTemplate orm = Orm.template();
orm.beginTransaction();
try {
    orm.exec(updateA);
    orm.insert(order);
    orm.commit();
} catch (Exception e) {
    orm.rollback();
    throw e;
}
```

### 8.3 老 API（保留）

`DAOAbstract`（`Q` 继承）自带线程级事务：

```java
Q q = new Q();
try {
    q.transOpen();                       // 开启（默认 READ_COMMITTED）
    q.save(a);
    q.update(b);
    q.transCommit();
} catch (Exception e) {
    q.transRollback();
    throw e;
}
```

| 方法 | 说明 |
| --- | --- |
| `transOpen()` / `transOpen(int level)` | 开启事务，连接绑定到当前线程 |
| `transCommit()` | 提交并归还连接 |
| `transRollback()` / `transRollback(Savepoint)` | 回滚（可回滚到保存点） |

注意事项（三条事务路线共用同一线程连接实现）：

- 事务连接在**当前线程**，不要跨线程传递；
- `transExists()` 的返回值语义是“事务**不存在**”（历史命名遗留），调用方无需关心；
- `Q.beginTransaction/commit/rollback` 与 `Orm.template()` 的同名方法、`transOpen/Commit/Rollback`
  指向同一个线程连接，**不要交叉嵌套同一业务单元**；
- MyBatis 插件的事务走 `MybatisKit.openSession(false)` 自行 `commit/rollback`，不要与上述事务混用。

---

## 9. 异常与跨库兼容

### 9.1 `DaoException extends Throwable`

历史遗留：它不是 `Exception`，因此：

```java
public DemoUser findById(Long id) throws DaoException { ... }   // 必须显式声明
```

```java
try {
    q.exec(ps);
} catch (DaoException e) {          // 不能只 catch Exception
    LogUtil.error("执行失败", e);
}
```

建议：新模块在 Service 层统一把 `DaoException` 转成业务运行时异常，Controller 只处理业务异常。

### 9.2 列名大小写 / 下划线

| 场景 | 处理方式 |
| --- | --- |
| 列名为大写（Oracle/达梦/金仓） | `queryEntityList` 已忽略大小写匹配实体字段，无需处理 |
| 列名为 `DEPT_ID` 等下划线 | 查询时 `AS` 别名（`DEPT_ID AS deptid`），或改用 MyBatis（`mapUnderscoreToCamelCase=true` 默认开启） |
| 结果集任意结构 | `queryMaps` 直接取列名，不做实体映射 |
| 关键字列（`ORDER` / `LEVEL`） | 手写 SQL 加引号，或改名 |

### 9.3 其他常见坑

1. **主键非自增却写了 `isAutoIncrement=true`** → INSERT 不含主键，数据库报非空约束；
2. **`@EFilter` 用在主键上无效**（主键走单独分支）；
3. **实体没有 getter** → 该字段被忽略（不报错，容易“查不到值”）；
4. **布尔字段** → 建议 `Boolean` + `isXxx()`，数据库用 `INT`/`BIT`；
5. **日期字段** → `java.util.Date` / `java.sql.Timestamp` 均可，`CLOB` 由框架自动转字符串；
6. **`count(Model)` 拼接 `filter`** → 老 API 直接拼 SQL，新代码请用 `count(ParameterizedSql)`。

---

## 10. 性能建议

| 建议 | 原因 |
| --- | --- |
| 复用 `new Q()` 即可，无需单例 | `Q` 无状态，连接走线程上下文 |
| 列表查询用 `queryEntityList` / `query(ps, Class)` | 参数化 + 预编译，避免拼接与全表扫描 |
| 大数据量导出用 `queryEntityList` 一次性取回 | 老 `pager` 的游标方式会长时间占用连接 |
| 批量写用事务包裹 | 减少提交次数 |
| 生产关闭 `debug` 与 `save-sql-log` | 后者会写 `SQLEXECLOG` 表，严重拖慢 |
| 实体字段保持精简 | 元数据一次解析后缓存，字段越多 SQL 越长 |

---

## 11. 完整示例索引

| 文件 | 内容 |
| --- | --- |
| [DemoUser.java](../tarn-demo/src/main/java/examples/demo/entity/DemoUser.java) | 实体注解：`@Etable` / `@Ekey` / `@EUpdateNull` / `@ORMFilter` / `@EFilter` |
| [DemoDept.java](../tarn-demo/src/main/java/examples/demo/entity/DemoDept.java) | 关联表实体 |
| [DemoUserService.java](../tarn-demo/src/main/java/examples/demo/service/DemoUserService.java) | ORM / SqlBuilder / QueryBuilder / MyBatis 四类写法 |
| [DemoUserMapper.java](../tarn-demo/src/main/java/examples/demo/mapper/DemoUserMapper.java) | MyBatis 注解式 Mapper |
| [DemoUserController.java](../tarn-demo/src/main/java/examples/demo/controller/DemoUserController.java) | 新路由 + `@Json` 全链路接口 |
| [BookDao.java](../tarn-demo/src/main/java/examples/standalone/dao/BookDao.java) | DAO 分层模板：insertReturnKey / updateRows / SqlBuilder 动态条件 |
| [OrmUsageExamples.java](../tarn-demo/src/main/java/examples/orm/OrmUsageExamples.java) | **业务场景示例集（E1~E8）**：JSON 字段、动态条件、分页、下单扣库存事务、JOIN、聚合、手动事务 |
| [OrmApiVerification.java](../tarn-demo/src/main/java/examples/orm/OrmApiVerification.java) | **全量 API 验证程序（46 项断言）**：实体绑定、全部写 API、事务、老 API 兼容 |
| [DemoNote.java](../tarn-demo/src/main/java/examples/orm/entity/DemoNote.java) | 复杂字段实体（List / Map / 自定义类 JSON 序列化） |
| [DemoOrder.java](../tarn-demo/src/main/java/examples/orm/entity/DemoOrder.java) | 订单实体（BigDecimal / 事务场景） |

运行方式：

```bash
# 全量 API 验证（46 项断言，全 PASS 退出码 0）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmApiVerification

# 业务场景示例（E1~E8 顺序执行，打印真实结果）
mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmUsageExamples
```

### 11.1 建表 DDL

```sql
CREATE TABLE demo_dept (
    ID   BIGINT PRIMARY KEY AUTO_INCREMENT,
    NAME VARCHAR(64) NOT NULL
);

CREATE TABLE demo_user (
    ID      BIGINT PRIMARY KEY AUTO_INCREMENT,
    NAME    VARCHAR(64) NOT NULL,
    DEPTID  BIGINT,
    STATUS  INT DEFAULT 1,
    REMARK  VARCHAR(255)
);
```

### 11.2 接口冒烟

```bash
# 新增
curl -X POST http://127.0.0.1:8080/api/demo/user/save \
     -H 'Content-Type: application/json' \
     -d '{"name":"张三","deptid":1,"status":1,"remark":"演示"}'

# 明细
curl 'http://127.0.0.1:8080/api/demo/user/detail?id=1'

# 分页
curl 'http://127.0.0.1:8080/api/demo/users/page?status=1&page=1&size=10'

# 联表
curl 'http://127.0.0.1:8080/api/demo/users/join?status=1&page=1&size=10'

# MyBatis（需 tarn.mybatis.enabled=true）
curl 'http://127.0.0.1:8080/api/demo/users/mybatis?status=1'

# JSON 输出演示
curl 'http://127.0.0.1:8080/api/demo/ping'
```

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1761312000000
}
```

### 11.3 真实 MySQL 验证报告（Tarn 2.1）

验证环境：MySQL 5.7 / `jdbc:mysql://127.0.0.1:3306/tarn_demo`，表 `demo_book(ID, TITLE, AUTHOR, STOCK)`，
实体 `Book`（`@Etable("demo_book")` + `@Ekey(key="id", isAutoIncrement=true)`），Druid 连接池，
验证程序 [OrmApiVerification.java](../tarn-demo/src/main/java/examples/orm/OrmApiVerification.java)，
**46 项断言全部 PASS，退出码 0**。

| 验证组 | 覆盖点 | 结论 |
| --- | --- | --- |
| T01 实体绑定 | `@Etable` 表名、`@Ekey` 主键/自增、字段→大写列名（TITLE/AUTHOR/STOCK） | 通过 |
| T02~T06 写 API | insert 不含自增主键列；insertReturnKey 回填+回查一致；updateRows null 跳过且主键只进 WHERE；更新不存在主键返回 0；deleteRows 重复删除返回 0、无主键抛 `IllegalArgumentException` | 通过 |
| T07 批量 | batchInsert 成功计数；NOT NULL 违例抛 `TarnDataException` 且整体回滚 | 通过 |
| T08/T09 事务 | `inTransaction` 提交（两笔写生效）与回滚（异常抛出且 `cause` 保留、零残留） | 通过 |
| T10~T14 查询 | RowMapper 命名参数绑定；`query(Class)` 大小写列映射；`queryOne` 无结果返回 null；`queryMaps`；参数化 COUNT；limit 分页；表名注入被白名单拦截 | 通过 |
| T15~T17 老入口 | `new Q()` save/saveRK/update/delete、find(主键)/querys(空实体全表)/findById；`new Tarn()` 兼容 + `version()` | 通过 |

落库复核（mysql 客户端直查）与断言一致，示例结束后示例表自动清理、`demo_book` 重置为种子数据。

---

## 12. 从老 ORM 迁移到新 ORM 的对照表

| 老代码 | 建议替换 | 备注 |
| --- | --- | --- |
| `exec("update t set a='" + v + "'")` | `exec(SqlBuilder.create("UPDATE t SET A=#{a}").bind("a", v).build())` | 防注入 |
| `find(User.class, "id=" + id)` | `query(SqlBuilder...build(), User.class)` | 防注入 |
| `querys(User.class, sql)` | `queryEntityList(sql, params, User.class)` | 支持参数绑定 |
| `pager(sql, size, index)` | `QueryBuilder...page()` | 跨库分页 |
| 手写多表 SQL + `queryMaps` | `QueryBuilder.join().on()` | 白名单校验 |
| `dao.get("SELECT ...")` | MyBatis Mapper | 复杂 SQL 场景 |
| `q.save(user)` | 保持（内部已用元数据缓存） | 零改动 |
| `q.update(user)` | 保持，按需加 `@EUpdateNull` | 零改动 |
| `new Q()` / `new Tarn()` | `Orm.template()`（可选） | 同一实现，替换无风险；老入口长期保留 |

> 迁移原则：**存量按原样跑，新增按新写法写**。不要在一次性重构中混用两套 API，避免回归面扩大。
> `Q` 与 `Orm.template()` 底层是同一个 `OrmTemplate` 实例，替换入口不改变任何行为（见 §3.8）。
