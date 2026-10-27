package examples.orm;

import cc.ruixing.framework.tarn.core.connection.ConnectionFactory;
import cc.ruixing.framework.tarn.core.sql.ParameterizedSql;
import cc.ruixing.framework.tarn.core.sql.SqlBuilder;
import cc.ruixing.framework.tarn.orm.Orm;
import cc.ruixing.framework.tarn.orm.OrmTemplate;
import cc.ruixing.framework.tarn.orm.TransactionCallback;
import examples.orm.entity.DemoNote;
import examples.orm.entity.DemoOrder;
import examples.standalone.entity.Book;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ORM 业务场景使用示例（Tarn 2.1 组合式引擎，可直接运行）。
 * <p>
 * 与 {@link OrmApiVerification}（断言式全量 API 验证）互补，本类以「业务开发者视角」
 * 给出可直接复制的代码模板，每个场景独立成方法并打印真实执行结果：
 * <ol>
 *     <li>E1 复杂字段 JSON 序列化（List / Map / 自定义类往返）</li>
 *     <li>E2 动态条件查询（可选条件逐个追加，全部参数绑定）</li>
 *     <li>E3 分页查询（count + limit 的标准列表页写法）</li>
 *     <li>E4 声明式事务 —— 下单扣库存（正常提交）</li>
 *     <li>E5 声明式事务 —— 库存不足自动回滚</li>
 *     <li>E6 JOIN 查询 + queryMaps（多表/自由列）</li>
 *     <li>E7 GROUP BY 聚合统计</li>
 *     <li>E8 手动事务（beginTransaction / commit / rollback 的 try-catch 规范）</li>
 * </ol>
 * 运行：
 * <pre>
 *     mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmUsageExamples
 * </pre>
 *
 * @author dfr
 * @see OrmApiVerification 全量 API 验证（断言式）
 * @since 2.1
 */
public class OrmUsageExamples {

    public static void main(String[] args) throws Exception {
        banner("Tarn ORM 业务场景示例  target=" + url());
        ConnectionFactory.init();
        setup();

        e1ComplexFieldJson();
        e2DynamicConditions();
        e3Pagination();
        e4TransactionPlaceOrder();
        e5TransactionRollback();
        e6JoinQuery();
        e7Aggregation();
        e8ManualTransaction();

//        cleanup();
        banner("全部业务场景示例执行完成");
        System.exit(0);
    }

    // ==================================================================
    // E1. 复杂字段 JSON 序列化：List / Map / 自定义类
    // ==================================================================

    /**
     * 实体中 List/Map/自定义类字段无需任何注解，入库时自动 JSON 序列化，
     * 查询时自动反序列化回原类型；数据库列用 VARCHAR/TEXT 承载即可。
     */
    private static void e1ComplexFieldJson() {
        banner("E1. 复杂字段 JSON 序列化（List / Map / 自定义类）");

        DemoNote note = new DemoNote();
        note.setTitle("Tarn ORM 笔记");
        note.setTags(Arrays.asList("orm", "tarn", "mysql"));
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("creator", "dfr");
        meta.put("stars", 5);
        note.setMeta(meta);
        DemoNote.Attrs attrs = new DemoNote.Attrs();
        attrs.setPinned(true);
        attrs.setLevel(3);
        note.setAttrs(attrs);

        Orm.template().insertReturnKey(note);

        DemoNote loaded = Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_note")
                        .where("TITLE = #{t}").bind("t", "Tarn ORM 笔记").build(),
                DemoNote.class);
        System.out.println("写入: " + note);
        System.out.println("回读: " + loaded);
    }

    // ==================================================================
    // E2. 动态条件查询
    // ==================================================================

    /**
     * 列表页常见的「条件可选」查询：用 and() 逐个追加，
     * 值全部走 ? 或 #{name} 参数绑定，杜绝字符串拼接注入。
     */
    private static void e2DynamicConditions() {
        banner("E2. 动态条件查询（可选条件逐个追加）");

        String keyword = "Tarn";       // 模拟前端传参，置为 null 可观察无条件分支
        Integer minStock = 3;

        SqlBuilder b = SqlBuilder
                .select("ID, TITLE, AUTHOR, STOCK")
                .from("demo_book")
                .where("STOCK >= ?", minStock);
        if (keyword != null && !keyword.isEmpty()) {
            b.and("TITLE LIKE ?", "%" + keyword + "%");
        }
        b.orderBy("STOCK", false);

        List<Book> books = Orm.template().query(b.build(), Book.class);
        System.out.println("条件: stock>=" + minStock + (keyword == null ? "" : " 且 title like '%" + keyword + "%'"));
        for (Book book : books) {
            System.out.println("  - " + book.getId() + " | " + book.getTitle()
                    + " | " + book.getAuthor() + " | stock=" + book.getStock());
        }
    }

    // ==================================================================
    // E3. 分页查询
    // ==================================================================

    /**
     * 标准列表页分页：先 count 取总数，再 limit 取当前页。
     * 两个 SQL 共用同一组动态条件，保证统计与明细一致。
     */
    private static void e3Pagination() {
        banner("E3. 分页查询（count + limit）");

        int pageNo = 2;
        int pageSize = 2;

        SqlBuilder countBuilder = SqlBuilder.create("SELECT COUNT(0) FROM demo_book").where("STOCK >= ?", 0);
        int total = Orm.template().count(countBuilder.build());

        int offset = (pageNo - 1) * pageSize;
        List<Book> page = Orm.template().query(
                SqlBuilder.select("ID, TITLE, AUTHOR, STOCK")
                        .from("demo_book")
                        .where("STOCK >= ?", 0)
                        .orderBy("ID", true)
                        .limit(offset, pageSize)
                        .build(),
                Book.class);

        System.out.println("第 " + pageNo + " 页 / 共 " + ((total + pageSize - 1) / pageSize)
                + " 页（总数 " + total + "，每页 " + pageSize + "）");
        for (Book book : page) {
            System.out.println("  - " + book.getId() + " | " + book.getTitle());
        }
    }

    // ==================================================================
    // E4. 声明式事务：下单扣库存（正常提交）
    // ==================================================================

    /**
     * 经典「下单扣库存」：扣减与生成订单必须在同一事务。
     * 条件更新（AND STOCK >= ?）保证并发下不超卖；inTransaction
     * 正常返回即提交，抛出任何异常自动回滚。
     */
    private static void e4TransactionPlaceOrder() {
        banner("E4. 声明式事务：下单扣库存（提交）");

        final Long bookId = 1L;
        final int qty = 2;

        Orm.template().inTransaction(new TransactionCallback<DemoOrder>() {
            public DemoOrder doInTransaction(OrmTemplate orm) {
                // 1. 条件更新扣库存（不满足 STOCK >= qty 时受影响行数为 0）
                int rows = orm.exec(SqlBuilder
                        .create("UPDATE demo_book SET STOCK = STOCK - #{qty} WHERE ID = #{id} AND STOCK >= #{qty}")
                        .bind("qty", qty).bind("id", bookId).build());
                if (rows == 0) {
                    throw new IllegalStateException("库存不足，无法下单");
                }
                // 2. 生成订单（与扣库存在同一事务）
                DemoOrder order = new DemoOrder();
                order.setOrderNo("SO-2026-0001");
                order.setBookId(bookId);
                order.setQty(qty);
                order.setAmount(new BigDecimal("179.20"));
                orm.insertReturnKey(order);
                return order;
            }
        });

        System.out.println("下单成功：");
        System.out.println("  订单 = " + firstOrder("SO-2026-0001"));
        System.out.println("  图书库存 = " + stockOf(bookId) + "（原 10，扣 2）");
    }

    // ==================================================================
    // E5. 声明式事务：库存不足自动回滚
    // ==================================================================

    /**
     * 库存不足时业务代码直接抛异常即可，inTransaction 捕获后自动回滚，
     * 不会出现「订单已生成但库存未扣」或反之的中间状态。
     */
    private static void e5TransactionRollback() {
        banner("E5. 声明式事务：库存不足自动回滚");

        int beforeStock = stockOf(2L);
        int beforeOrders = orderCount();
        try {
            Orm.template().inTransaction(new TransactionCallback<Void>() {
                public Void doInTransaction(OrmTemplate orm) {
                    DemoOrder order = new DemoOrder();
                    order.setOrderNo("SO-2026-BAD");
                    order.setBookId(2L);
                    order.setQty(99999);
                    order.setAmount(BigDecimal.ZERO);
                    orm.insertReturnKey(order);        // 订单先落库（未提交）

                    int rows = orm.exec(SqlBuilder
                            .create("UPDATE demo_book SET STOCK = STOCK - #{qty}"
                                    + " WHERE ID = #{id} AND STOCK >= #{qty}")
                            .bind("qty", 99999).bind("id", 2L).build());
                    if (rows == 0) {
                        throw new IllegalStateException("库存不足，无法下单");
                    }
                    return null;
                }
            });
        } catch (RuntimeException e) {
            System.out.println("下单失败（预期）: " + e.getMessage());
        }
        System.out.println("回滚后：订单数 " + beforeOrders + " -> " + orderCount()
                + "，图书[2]库存 " + beforeStock + " -> " + stockOf(2L));
    }

    // ==================================================================
    // E6. JOIN 查询 + queryMaps
    // ==================================================================

    /**
     * 多表关联、自由列场景用 queryMaps 返回 Map 行；
     * 含别名的 SQL 用 SqlBuilder.create() 起始（from() 仅接受单表标识符）。
     */
    private static void e6JoinQuery() {
        banner("E6. JOIN 查询 + queryMaps（订单关联图书）");

        ParameterizedSql ps = SqlBuilder
                .create("SELECT o.ORDERNO, o.QTY, o.AMOUNT, b.TITLE, b.AUTHOR FROM demo_order o"
                        + " JOIN demo_book b ON o.BOOKID = b.ID")
                .where("o.QTY > ?", 0)
                .orderBy("o.ID")
                .build();
        List<Map<String, Object>> rows = Orm.template().queryMaps(ps);
        for (Map<String, Object> row : rows) {
            System.out.println("  - " + row.get("ORDERNO") + " | " + row.get("TITLE")
                    + " | qty=" + row.get("QTY") + " | amount=" + row.get("AMOUNT"));
        }
    }

    // ==================================================================
    // E7. GROUP BY 聚合统计
    // ==================================================================

    private static void e7Aggregation() {
        banner("E7. GROUP BY 聚合统计（按作者汇总图书数与库存）");

        List<Map<String, Object>> stats = Orm.template().queryMaps(
                SqlBuilder.create("SELECT AUTHOR, COUNT(0) AS BOOK_CNT, IFNULL(SUM(STOCK), 0) AS STOCK_SUM"
                        + " FROM demo_book")
                        .groupBy("AUTHOR")
                        .orderBy("BOOK_CNT", false)
                        .build());
        for (Map<String, Object> row : stats) {
            System.out.println("  - " + row.get("AUTHOR") + "：图书 " + row.get("BOOK_CNT")
                    + " 种，库存合计 " + row.get("STOCK_SUM"));
        }
    }

    // ==================================================================
    // E8. 手动事务（长事务 / 需要精确控制提交时机）
    // ==================================================================

    /**
     * 需要手动控制提交/回滚时的规范写法：
     * begin -> try{ 业务; commit } catch { rollback; rethrow }。
     * 优先使用 E4 的 inTransaction 声明式写法，仅在特殊场景使用手动模式。
     */
    private static void e8ManualTransaction() {
        banner("E8. 手动事务（beginTransaction / commit / rollback）");

        OrmTemplate orm = Orm.template();
        orm.beginTransaction();
        try {
            orm.exec(SqlBuilder.create("UPDATE demo_book SET STOCK = STOCK + 1 WHERE ID = 3").build());
            DemoOrder order = new DemoOrder();
            order.setOrderNo("SO-2026-0002");
            order.setBookId(3L);
            order.setQty(1);
            order.setAmount(new BigDecimal("99.00"));
            orm.insertReturnKey(order);
            orm.commit();
            System.out.println("手动事务已提交：订单 " + order.getOrderNo()
                    + "，图书[3]库存=" + stockOf(3L));
        } catch (Exception e) {
            orm.rollback();
            System.out.println("手动事务已回滚: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ==================================================================
    // 环境准备 / 清理 / 工具
    // ==================================================================

    private static void setup() {
        // 示例专用表（幂等重建）
        execDdl("DROP TABLE IF EXISTS demo_order");
        execDdl("CREATE TABLE demo_order ("
                + " ID BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + " ORDERNO VARCHAR(64) NOT NULL,"
                + " BOOKID BIGINT NOT NULL,"
                + " QTY INT NOT NULL DEFAULT 1,"
                + " AMOUNT DECIMAL(10,2) NOT NULL DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execDdl("DROP TABLE IF EXISTS demo_note");
        execDdl("CREATE TABLE demo_note ("
                + " ID BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + " TITLE VARCHAR(128),"
                + " TAGS VARCHAR(512),"
                + " META VARCHAR(512),"
                + " ATTRS VARCHAR(512)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        // 图书种子数据
        execDdl("TRUNCATE TABLE demo_book");
        Orm.template().batchInsert(Arrays.asList(
                book("Java 编程思想", "Bruce Eckel", 10),
                book("Effective Java", "Joshua Bloch", 8),
                book("Tarn 实战", "dfr", 3),
                book("Tarn 进阶", "dfr", 0)));
    }

    private static void cleanup() {
        execDdl("DROP TABLE IF EXISTS demo_order");
        execDdl("DROP TABLE IF EXISTS demo_note");
        execDdl("TRUNCATE TABLE demo_book");
        System.out.println("（示例表 demo_order/demo_note 已清理，demo_book 已重置）");
    }

    private static void execDdl(String sql) {
        Orm.template().exec(SqlBuilder.create(sql).build());
    }

    private static Book book(String title, String author, int stock) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setStock(stock);
        return b;
    }

    private static int stockOf(Long bookId) {
        Book b = Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_book").where("ID = #{id}").bind("id", bookId).build(),
                Book.class);
        return b == null || b.getStock() == null ? -1 : b.getStock();
    }

    private static int orderCount() {
        return Orm.template().count(SqlBuilder.create("SELECT COUNT(0) FROM demo_order").build());
    }

    private static DemoOrder firstOrder(String orderNo) {
        return Orm.template().queryOne(
                SqlBuilder.select("*").from("demo_order").where("ORDERNO = #{no}").bind("no", orderNo).build(),
                DemoOrder.class);
    }

    private static String url() {
        try {
            return ConnectionFactory.DATA_SOURCE_POOL == null
                    ? "(未初始化)" : String.valueOf(ConnectionFactory.DATA_SOURCE_POOL.getConnection().getMetaData().getURL());
        } catch (Exception e) {
            return "(获取连接失败)";
        }
    }

    private static void banner(String msg) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("  " + msg);
        System.out.println("============================================================");
    }
}
