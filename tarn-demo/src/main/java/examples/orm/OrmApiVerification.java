package examples.orm;

import com.dfrdev.tarn.core.DaoException;
import com.dfrdev.tarn.core.Tarn;
import com.dfrdev.tarn.core.TarnDataException;
import com.dfrdev.tarn.core.Q;
import com.dfrdev.tarn.core.RowMapper;
import com.dfrdev.tarn.core.connection.ConnectionFactory;
import com.dfrdev.tarn.core.sql.ParameterizedSql;
import com.dfrdev.tarn.core.sql.SqlBuilder;
import com.dfrdev.tarn.json.JsonKit;
import com.dfrdev.tarn.orm.EntityMapper;
import com.dfrdev.tarn.orm.Orm;
import com.dfrdev.tarn.orm.OrmTemplate;
import com.dfrdev.tarn.orm.TransactionCallback;
import com.dfrdev.tarn.util.StringUtil;
import examples.standalone.entity.Book;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ORM 全量 API 验证程序（Tarn 2.1，真实 MySQL：tarn_demo.demo_book）。
 * <p>
 * 覆盖内容（每项独立断言，输出 PASS/FAIL，任一失败进程退出码非 0）：
 * <ol>
 *     <li>实体↔表绑定（EntityMapper：表名/列名/主键/自增）</li>
 *     <li>insert / insertReturnKey（主键回填）</li>
 *     <li>updateRows（null 跳过、受影响行数、主键不存在返回 0）</li>
 *     <li>deleteRows（按主键删除）</li>
 *     <li>batchInsert（批量 + 失败整体回滚）</li>
 *     <li>声明式事务 inTransaction（提交 / 回滚）</li>
 *     <li>参数化查询：query(RowMapper) / query(Class) / queryOne / queryMaps / count</li>
 *     <li>SqlBuilder：条件、命名参数、排序、分页 limit</li>
 *     <li>老 API 兼容：new Q() / new Tarn() 的 save / saveRK / update / delete / find / querys / findById</li>
 * </ol>
 * 运行：
 * <pre>
 *     mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmApiVerification
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
public class OrmApiVerification {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        banner("Tarn ORM 全量 API 验证  target=" + target());
        ConnectionFactory.init();   // 触发连接池初始化（application.yml 直连配置）
        ParameterizedSql parameterizedSql = SqlBuilder.select("*").from("demo_book").where("stock >= #{stock}").bind("stock", 2).build();

        List<Book> list = Orm.template().query(parameterizedSql, Book.class);
        list.stream().forEach(item->{
            System.out.println("tiemx:"+ JsonKit.toJson(item));
        });
        // 每次验证前清空数据，保证断言确定性
//        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());

        // ===== 1. 实体 ↔ 表绑定 =====
        t01EntityMetaBinding();
        // ===== 2. 写操作 =====
        t02Insert();
        t03InsertReturnKey();
        t04UpdateRows();
        t05UpdateMissingRow();
        t06DeleteRows();
        t07BatchInsertAndRollback();
        // ===== 3. 事务 =====
        t08TransactionCommit();
        t09TransactionRollback();
        // ===== 4. 参数化查询 =====
        t10QueryRowMapper();
        t11QueryEntityClass();
        t12QueryMaps();
        t13Count();
        t14SqlBuilderPaging();
        // ===== 5. 老 API 兼容（Q / Tarn 入口） =====
        t15LegacyQSaveUpdateDelete();
        t16LegacyFindAndQuerys();
        t17TarnEntryCompat();

        banner("结果: PASS=" + passed + "  FAIL=" + failed);
        System.exit(failed > 0 ? 1 : 0);
    }

    private static String target() {
        try {
            return ConnectionFactory.DATA_SOURCE_POOL == null
                    ? "(未初始化)" : String.valueOf(ConnectionFactory.DATA_SOURCE_POOL.getConnection().getMetaData().getURL());
        } catch (Exception e) {
            return "(获取连接失败: " + e.getMessage() + ")";
        }
    }

    // ------------------------------------------------------------------
    // 1. 实体绑定
    // ------------------------------------------------------------------

    /**
     * 验证点：@Etable 表名、@Ekey 主键与自增、字段名→大写列名映射。
     */
    private static void t01EntityMetaBinding() {
        EntityMapper.Meta meta = EntityMapper.meta(Book.class);
        check("T01 表名绑定 @Etable(demo_book)", "demo_book".equals(meta.tableName));
        check("T01 主键列存在且为 ID", meta.primaryKeyColumn != null && "ID".equals(meta.primaryKeyColumn.columnName));
        check("T01 主键声明自增", meta.primaryKeyColumn.pkAutoIncrement);
        check("T01 字段→大写列名(TITLE/AUTHOR/STOCK)",
                findCol(meta, "TITLE") != null && findCol(meta, "AUTHOR") != null && findCol(meta, "STOCK") != null);
    }

    private static EntityMapper.Column findCol(EntityMapper.Meta meta, String name) {
        for (EntityMapper.Column c : meta.columns) {
            if (c.columnName.equals(name)) {
                return c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 2. 写操作（OrmTemplate 组合入口）
    // ------------------------------------------------------------------

    /**
     * 验证点：insert 返回受影响行数；SQL 中不含自增主键列。
     */
    private static void t02Insert() {
        Book b = book(null, "Tarn 实战", "dfr", 10);
        int rows = Orm.template().insert(b);
        check("T02 insert 返回受影响行数=1", rows == 1);
        check("T02 insert 不回填主键(未用 insertReturnKey)", b.getId() == null);
        ParameterizedSql sql = com.dfrdev.tarn.orm.EntitySql.insert(book(null, "x", null, null));
        check("T02 自增主键不进入 INSERT 列", !sql.getSql().contains("ID"));
    }

    /**
     * 验证点：insertReturnKey 返回数据库生成主键并回填实体；回查数据一致。
     */
    private static void t03InsertReturnKey() {
        Book b = book(null, "深入 Tarn 内核", "dfr", 5);
        Object key = Orm.template().insertReturnKey(b);
        check("T03 返回生成主键", key != null);
        check("T03 主键已回填到实体", b.getId() != null && key.toString().equals(b.getId().toString()));
        Book loaded = Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_book").where("ID = #{id}").bind("id", b.getId()).build(),
                Book.class);
        check("T03 回查数据一致(title/author/stock)", loaded != null
                && "深入 Tarn 内核".equals(loaded.getTitle())
                && "dfr".equals(loaded.getAuthor())
                && Integer.valueOf(5).equals(loaded.getStock()));
    }

    /**
     * 验证点：updateRows 按主键更新、null 字段跳过、返回真实受影响行数。
     */
    private static void t04UpdateRows() {
        Book seed = book(null, "更新前的书", "原作者", 1);
        Orm.template().insert(seed);
        Book loaded = findByTitle("更新前的书");

        Book change = new Book();
        change.setId(loaded.getId());
        change.setTitle("更新后的书");
        change.setStock(99);
        // author 保持 null：默认不更新 null 字段
        int rows = Orm.template().updateRows(change);

        Book after = findByTitle("更新后的书");
        check("T04 返回受影响行数=1", rows == 1);
        check("T04 更新生效(stock=99)", after != null && Integer.valueOf(99).equals(after.getStock()));
        check("T04 null 字段未更新(author 保留)", "原作者".equals(after.getAuthor()));
        ParameterizedSql sql = com.dfrdev.tarn.orm.EntitySql.update(change);
        check("T04 主键不进 SET、只进 WHERE", sql.getSql().startsWith("UPDATE demo_book SET ")
                && sql.getSql().endsWith("WHERE ID = ?") && !sql.getSql().contains("ID = ?,"));
    }

    /**
     * 验证点：更新不存在的主键返回 0（而不是抛异常或报错）。
     */
    private static void t05UpdateMissingRow() {
        Book ghost = new Book();
        ghost.setId(999999L);
        ghost.setTitle("幽灵书");
        int rows = Orm.template().updateRows(ghost);
        check("T05 更新不存在主键返回 0", rows == 0);
    }

    /**
     * 验证点：deleteRows 严格按主键删除；重复删除返回 0。
     */
    private static void t06DeleteRows() {
        Book seed = book(null, "待删除的书", "dfr", 1);
        Orm.template().insert(seed);
        Book loaded = findByTitle("待删除的书");
        int first = Orm.template().deleteRows(loaded);
        int second = Orm.template().deleteRows(loaded);
        check("T06 首次删除返回 1", first == 1);
        check("T06 重复删除返回 0", second == 0);
        try {
            Book noPk = new Book();
            Orm.template().deleteRows(noPk);
            check("T06 无主键删除抛 IllegalArgumentException", false);
        } catch (IllegalArgumentException expected) {
            check("T06 无主键删除抛 IllegalArgumentException", true);
        }
    }

    /**
     * 验证点：batchInsert 批量成功；TITLE 为 NOT NULL，非法数据触发整体回滚。
     */
    private static void t07BatchInsertAndRollback() {
        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());

        List<Book> batch = new ArrayList<Book>();
        for (int i = 1; i <= 3; i++) {
            batch.add(book(null, "批量书-" + i, "dfr", i));
        }
        int total = Orm.template().batchInsert(batch);
        int count = Orm.template().count(SqlBuilder.create("SELECT COUNT(0) FROM demo_book").build());
        check("T07 批量插入返回 3", total == 3);
        check("T07 库中行数=3", count == 3);

        List<Book> bad = new ArrayList<Book>();
        bad.add(book(null, "合法数据", "dfr", 1));
        bad.add(book(null, null, "dfr", 2));   // TITLE NOT NULL → SQL 失败
        try {
            Orm.template().batchInsert(bad);
            check("T07 非法批量抛 TarnDataException", false);
        } catch (TarnDataException e) {
            int after = Orm.template().count(SqlBuilder.create("SELECT COUNT(0) FROM demo_book").build());
            check("T07 非法批量抛 TarnDataException", true);
            check("T07 失败整体回滚(行数仍=3)", after == 3);
        }
    }

    // ------------------------------------------------------------------
    // 3. 事务
    // ------------------------------------------------------------------

    /**
     * 验证点：inTransaction 正常返回 → 提交，回调内多次写全部生效。
     */
    private static void t08TransactionCommit() {
        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());
        Orm.template().inTransaction(new TransactionCallback<Void>() {
            public Void doInTransaction(OrmTemplate orm) {
                orm.insert(book(null, "事务书-1", "dfr", 1));
                orm.insert(book(null, "事务书-2", "dfr", 2));
                return null;
            }
        });
        int count = Orm.template().count(SqlBuilder.create("SELECT COUNT(0) FROM demo_book").build());
        check("T08 事务提交(两行都生效)", count == 2);
    }

    /**
     * 验证点：inTransaction 抛异常 → 自动回滚，库中无残留。
     */
    private static void t09TransactionRollback() {
        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());
        try {
            Orm.template().inTransaction(new TransactionCallback<Void>() {
                public Void doInTransaction(OrmTemplate orm) throws Exception {
                    orm.insert(book(null, "将被回滚的书", "dfr", 1));
                    throw new IllegalStateException("模拟业务失败");
                }
            });
            check("T09 异常向上抛出", false);
        } catch (TarnDataException e) {
            check("T09 异常向上抛出", e.getCause() instanceof IllegalStateException);
        }
        int count = Orm.template().count(SqlBuilder.create("SELECT COUNT(0) FROM demo_book").build());
        check("T09 事务回滚(0 行残留)", count == 0);
    }

    // ------------------------------------------------------------------
    // 4. 参数化查询
    // ------------------------------------------------------------------

    private static void seedForQuery() {
        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());
        List<Book> batch = new ArrayList<Book>();
        batch.add(book(null, "Java 编程思想", "Bruce Eckel", 10));
        batch.add(book(null, "Effective Java", "Joshua Bloch", 8));
        batch.add(book(null, "Tarn 实战", "dfr", 3));
        batch.add(book(null, "Tarn 进阶", "dfr", 0));
        Orm.template().batchInsert(batch);
    }

    /**
     * 验证点：RowMapper 逐行映射 + 命名参数绑定。
     */
    private static void t10QueryRowMapper() {
        seedForQuery();
        ParameterizedSql ps = SqlBuilder
                .select("ID, TITLE, AUTHOR, STOCK")
                .from("demo_book")
                .where("AUTHOR = #{author}").bind("author", "dfr")
                .orderBy("ID", true)
                .build();
        List<String> titles = new ArrayList<String>();
        List<?> rows = Orm.template().query(ps, new RowMapper() {
            public Object mapRow(ResultSet rs) throws SQLException {
                titles.add(rs.getString("TITLE"));
                return rs.getString("TITLE");
            }
        });
        check("T10 RowMapper 命中 2 行", rows.size() == 2 && titles.size() == 2);
        check("T10 命中内容正确", titles.contains("Tarn 实战") && titles.contains("Tarn 进阶"));
    }

    /**
     * 验证点：query(ps, Class) 实体列表映射（列标签大小写兼容：库列大写→字段小写）。
     */
    private static void t11QueryEntityClass() {
        List<Book> books = Orm.template().query(
                SqlBuilder.select("*").from("demo_book").where("STOCK > ?", 0).orderBy("STOCK", false).build(),
                Book.class);
        check("T11 实体查询命中 3 行", books.size() == 3);
        check("T11 字段映射正确(id/title/stock)", books.get(0).getId() != null
                && StringUtil.isNotEmpty(books.get(0).getTitle())
                && books.get(0).getStock() != null && books.get(0).getStock() > 0);
        Book one = Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_book").where("TITLE = #{t}").bind("t", "Effective Java").build(),
                Book.class);
        check("T11 queryOne 命中单条", one != null && "Joshua Bloch".equals(one.getAuthor()));
        Book none = Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_book").where("TITLE = #{t}").bind("t", "不存在的书").build(),
                Book.class);
        check("T11 queryOne 无结果返回 null", none == null);
    }

    /**
     * 验证点：queryMaps 适用于多表/自由列场景。
     */
    private static void t12QueryMaps() {
        List<Map<String, Object>> maps = Orm.template().queryMaps(
                SqlBuilder.select("ID, TITLE, STOCK").from("demo_book").where("STOCK = ?", 8).build());
        check("T12 queryMaps 命中 1 行", maps.size() == 1);
        check("T12 Map 列名与值正确", maps.get(0).containsKey("TITLE")
                && "Effective Java".equals(String.valueOf(maps.get(0).get("TITLE"))));
    }

    /**
     * 验证点：参数化 COUNT。
     */
    private static void t13Count() {
        int count = Orm.template().count(
                SqlBuilder.create("SELECT COUNT(0) FROM demo_book").where("AUTHOR = #{a}").bind("a", "dfr").build());
        check("T13 参数化 COUNT=2", count == 2);
    }

    /**
     * 验证点：SqlBuilder 分页 limit + 排序 + 注入防护（标识符白名单）。
     */
    private static void t14SqlBuilderPaging() {
        List<Book> page1 = Orm.template().query(
                SqlBuilder.select("*").from("demo_book").orderBy("ID", true).limit(0, 2).build(),
                Book.class);
        List<Book> page2 = Orm.template().query(
                SqlBuilder.select("*").from("demo_book").orderBy("ID", true).limit(2, 2).build(),
                Book.class);
        check("T14 第一页 2 行", page1.size() == 2);
        check("T14 第二页 2 行且不重叠", page2.size() == 2
                && !page1.get(0).getId().equals(page2.get(0).getId()));
        try {
            SqlBuilder.select("*").from("demo_book; DROP TABLE demo_book").build();
            check("T14 表名注入被拦截", false);
        } catch (IllegalArgumentException e) {
            check("T14 表名注入被拦截", true);
        }
    }

    // ------------------------------------------------------------------
    // 5. 老 API 兼容（继承链入口 Q / Tarn）
    // ------------------------------------------------------------------

    /**
     * 验证点：老入口 new Q() 的 save/update/delete/saveRK 与新引擎行为一致。
     */
    private static void t15LegacyQSaveUpdateDelete() {
        Q q = new Q();
        Book b = book(null, "老API的书", "dfr", 7);
        q.save(b);                                              // 老 save → 内部委托 OrmTemplate.insert
        Book saved = findByTitle("老API的书");
        check("T15 老 save 落库", saved != null);

        List<Object> keys = q.saveRK(book(null, "老API主键书", "dfr", 7));
        check("T15 老 saveRK 返回主键", keys != null && keys.size() == 1 && keys.get(0) != null);

        saved.setTitle("老API改后");
        saved.setStock(77);
        q.update(saved);
        Book updated = findByTitle("老API改后");
        check("T15 老 update 按主键更新", updated != null && Integer.valueOf(77).equals(updated.getStock()));

        q.delete(saved);
        check("T15 老 delete 按主键删除", findByTitle("老API改后") == null);
    }

    /**
     * 验证点：老查询 API find(按主键) / querys / findById 正常映射。
     */
    private static void t16LegacyFindAndQuerys() throws Exception {
        seedForQuery();
        Q q = new Q();
        Book probe = new Book();
        probe.setId(1L);
        Book found = (Book) q.find(probe);                      // 按主键查
        check("T16 find(主键) 命中", found != null && found.getId() != null && found.getTitle() != null);

        List<?> all = q.querys(new Book());                     // 无条件全表
        check("T16 querys 全表 4 行", all != null && all.size() == 4);

        Object byId = q.findById(Book.class, 2L);
        check("T16 findById(Class, key) 命中", byId instanceof Book && "Effective Java".equals(((Book) byId).getTitle()));
    }

    /**
     * 验证点：Tarn 总入口（继承链顶端）仍可用，版本号正确。
     */
    private static void t17TarnEntryCompat() {
        check("T17 Tarn.version()=2.0.0", "2.0.0".equals(Tarn.version()));
        Tarn tarn = new Tarn();
        tarn.save(book(null, "Tarn入口的书", "dfr", 1));
        check("T17 new Tarn().save 落库", findByTitle("Tarn入口的书") != null);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static Book book(Long id, String title, String author, Integer stock) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor(author);
        b.setStock(stock);
        return b;
    }

    private static Book findByTitle(String title) {
        return Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_book").where("TITLE = #{t}").bind("t", title).build(),
                Book.class);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static void banner(String msg) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("  " + msg);
        System.out.println("============================================================");
    }
}
