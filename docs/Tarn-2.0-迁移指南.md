# Tarn 框架 1.1 / 2.0 升级迁移指南

> 适用版本：tarn-framework 1.0.0 → 1.1.0 / 2.0.0
> 核心原则：**所有老 API 保留不删除，仅标记 @Deprecated；存量项目替换 jar 即可运行。**

---

## 1. 包路径变更对照表

### 1.1 core 包拆分

原 `com.dfrdev.tarn.core.*` 拆分为 4 个子包：

| 新包路径 | 移入的类 | 职责 |
|---|---|---|
| `core.boot` | Tarn, Config, ConfigLoader, TarnConfig, PluginInit, Plugins | 框架启动与配置 |
| `core.web` | Controller, PageContext, Pager, Model, TarnDispatcherFilter, ExpandFilterFactory, FilterInterface, Filters, DefaultFilterImpl | Web/MVC 层 |
| `core.db` | Q, DB, DAOAbstract, DAO, DaoException, Service, AbstractService, RowMapper, Assert | 数据库/ORM 继承链 |
| `core.sql` | ParameterizedSql, QueryBuilder, SqlBuilder, AppendReflectSQLUtils, AppendReflectSQLEntity, AppendDaoSQLUtils, AppendParmeterSQLUtils, AppendAbstractUtils | SQL 工具 |
| `core`（保留） | TarnException, TarnDataException | 异常基类 |

**示例 import 变更**：
```java
// 老 import
import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.core.Tarn;
import com.dfrdev.tarn.core.DB;

// 新 import
import com.dfrdev.tarn.core.web.Controller;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.db.DB;
```

### 1.2 util 包分组

原 `com.dfrdev.tarn.util.*` 拆分为 6 个子包：

| 新包路径 | 移入的类 | 职责 |
|---|---|---|
| `util.crypto` | SecKit, HexUtil | 加密解密 |
| `util.io` | FileKit, FileUtil, IOUtil, IORuntimeException, ZipUtil, Resource, StreamProgress | 文件与流 |
| `util.text` | StringUtil, TextKit, StrFormatter, PlaceHolder, PlaceHolderUtil, CharsetConvert, CharsetUtil, ChineseSpelling, FastStringBuilder, HtmlUtil, PatternPool, RegexPool | 字符串处理 |
| `util.date` | DateUtil, LongTimeTask | 日期时间 |
| `util.net` | ServletUtil, URLDecoder, URLEncoder, Url, VisitIpAddr | 网络相关 |
| `util.classloader` | ClassLoaderUtil, ClassScanner, ClassUtil, PackageUtil, TypeUtil, ReflectUtil, Primitives, NumberUtil, ObjectUtil | 类加载与反射 |
| `util`（保留） | LogUtil, PropKit, PathKit, EmailKit, Filter, ResourceUtil | 通用工具 |

**示例 import 变更**：
```java
// 老 import
import com.dfrdev.tarn.util.StringUtil;
import com.dfrdev.tarn.util.SecKit;
import com.dfrdev.tarn.util.DateUtil;

// 新 import
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.crypto.SecKit;
import com.dfrdev.tarn.util.date.DateUtil;
```

---

## 2. 老 API → 新 API 对照表

### 2.1 单例模式统一

| 类 | 老方法 | 新方法 | 说明 |
|---|---|---|---|
| PluginManager | `PluginManager.me()` | `PluginManager.getInstance()` | 统一命名规范 |
| OrmTemplate | `OrmTemplate.shared()` | `OrmTemplate.getInstance()` | `shared()` 仍保留（使用广泛） |
| RenderFactory | `RenderFactory.getInstance()` | 无需变更 | 已符合规范 |

### 2.2 DB 类清晰方法别名

| 新别名 | 委托到老方法 | 语义 |
|---|---|---|
| `persist(Object)` | `ds(Object)` | 插入实体 |
| `persistReturnKey(Object)` | `dsrc(Object)` | 插入并返回主键 |
| `modify(Object)` | `du(Object)` | 更新实体 |
| `remove(Object)` | `ddel(Object)` | 删除实体 |
| `queryOne(Model)` | `dd(DAO.class, Model, "")` | 查询单条 |
| `queryList(Model)` | `dds(DAO.class, Model, "")` | 查询列表 |
| `queryPage(Model)` | `dpa(Model, "")` | 分页查询 |
| `execute(String sql)` | `dexe(String)` | 执行 SQL |

> 老方法全部保留并标记 `@Deprecated`，新代码推荐使用清晰命名。

### 2.3 继承链 @Deprecated 标记

以下类的继承链已标记 `@Deprecated`，引导新代码使用组合模式：

```
@Deprecated Tarn extends Q extends DB extends DAOAbstract extends ConnectionFactory
```

| 类 | 推荐替代 |
|---|---|
| `Tarn` | `Orm.template()` 或 `OrmTemplate.getInstance()` |
| `Q` | `OrmTemplate` 参数化查询 |
| `DB` | `OrmTemplate` 写操作 |
| `DAOAbstract` | `OrmTemplate` 底层实现 |

> 仅文档级废弃，不改变运行时行为。老项目继承这些类不受影响。

---

## 3. SQL 安全最佳实践

### 3.1 已废弃的原始 SQL 方法

以下方法因存在 SQL 注入风险已标记 `@Deprecated`：

- `Q.exec(String sql)` / `Q.daos(String sql)` 等
- `AbstractService.query(String sql)` / `querys(String sql)` / `execSql(String sql)`
- `Service.query(String sql)` / `querys(String sql)`

### 3.2 推荐替代方案

```java
// ❌ 不推荐（已废弃）
List<DAO> list = q.daos("SELECT * FROM user WHERE name = '" + name + "'");

// ✅ 推荐：使用 SqlBuilder 参数化查询
ParameterizedSql ps = SqlBuilder.select("*")
    .from("user")
    .where("name = ?", name)
    .build();
List<DAO> list = OrmTemplate.shared().queryDAOSql(ps.getSql(), ps.getParams());
```

---

## 4. 批量替换脚本

如果项目中有大量 import 需要更新，可使用以下命令批量替换：

```bash
# core 包
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.core\.Controller;/import com.dfrdev.tarn.core.web.Controller;/g' {} +
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.core\.Tarn;/import com.dfrdev.tarn.core.boot.Tarn;/g' {} +
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.core\.DB;/import com.dfrdev.tarn.core.db.DB;/g' {} +

# util 包
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.util\.StringUtil;/import com.dfrdev.tarn.util.text.StringUtil;/g' {} +
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.util\.SecKit;/import com.dfrdev.tarn.util.crypto.SecKit;/g' {} +
find . -name "*.java" -exec sed -i \
  's/import com\.dfrdev\.tarn\.util\.DateUtil;/import com.dfrdev.tarn.util.date.DateUtil;/g' {} +
```

> 注意：`LogUtil`、`PropKit`、`PathKit` 等保留在 `util/` 根目录，import 无需变更。

---

## 5. 兼容性保证

| 保证项 | 说明 |
|---|---|
| 老方法不删除 | 所有标记 @Deprecated 的方法保留，运行时行为不变 |
| 二进制兼容 | 继承链不改变，老项目编译通过 |
| 渐进迁移 | IDE 中 @Deprecated 会显示删除线，逐步替换即可 |
| 测试覆盖 | 33 个核心单元测试全部通过 |
