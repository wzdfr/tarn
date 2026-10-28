package examples.orm;

import com.dfrdev.tarn.core.db.DAO;
import com.dfrdev.tarn.core.TarnDataException;
import com.dfrdev.tarn.core.web.Model;
import com.dfrdev.tarn.core.web.Pager;
import com.dfrdev.tarn.core.db.Q;
import com.dfrdev.tarn.core.db.RowMapper;
import com.dfrdev.tarn.core.connection.ConnectionFactory;
import com.dfrdev.tarn.core.sql.SqlBuilder;
import com.dfrdev.tarn.orm.Orm;
import com.dfrdev.tarn.plugin.orm.QueryBuffer;
import examples.standalone.entity.Book;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老 API 委托路径专项验证（Tarn 2.1，真实 MySQL：tarn_demo.demo_book）。
 * <p>
 * 背景：Q / DB / DAOAbstract 的老方法签名不变，内部已全部迁移到 OrmTemplate
 * （#name 命名占位 → ? 参数绑定、DAO/实体行映射复刻老语义）。
 * 本程序逐项验证这些"老入口、新内核"的路径行为正确：
 * <ol>
 *     <li>DAO 容器写：save / saveRK / update / delete（ds/du/ddel → execNamed）</li>
 *     <li>原始 SQL 查询：daos(String) / daos(Class, String)（queryLegacy 行映射）</li>
 *     <li>Model 分页：pager(Model) / dao(Model) / daos(Model)（pagerList/pager 链）</li>
 *     <li>SqlBuffer：daos(SqlBuffer) / exec(SqlBuffer) / count(SqlBuffer)</li>
 *     <li>RowMapper 与 like 语义：getObj（like #name 自动 %包装）</li>
 *     <li>异常语义：update(sql, map) 参数缺失抛 TarnDataException（老 TarnException 等价）</li>
 *     <li>pagerAbsolute 游标分页（内存切片等价语义）</li>
 *     <li>get(Class, sql, map) setter 名匹配映射</li>
 * </ol>
 * 运行：
 * <pre>
 *     mvn -pl tarn-demo compile exec:java -Dexec.mainClass=examples.orm.OrmLegacyApiVerification
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
public class OrmLegacyApiVerification {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        banner("Tarn 老 API 委托路径验证  target=" + ConnectionFactory.DATA_SOURCE_POOL);
        ConnectionFactory.init();

        // 确定性数据基础
        Orm.template().exec(SqlBuilder.create("TRUNCATE TABLE demo_book").build());
        seed("Java 编程思想", "Bruce Eckel", 10);
        seed("Effective Java", "Joshua Bloch", 8);
        seed("Tarn 实战", "dfr", 3);
        seed("Tarn 进阶", "dfr", 0);

        l01DaoSave();
        l02DaoSaveRK();
        l03DaoUpdate();
        l04DaoDelete();
        l05DaosRawSql();
        l06DaosEntitySql();
        l07PagerModel();
        l08SqlBuffer();
        l09ExecSqlBuffer();
        l10RowMapperLike();
        l11UpdateMissingParamThrows();
        l12PagerAbsolute();
        l13GetBySetters();
        l14DaoModelSingle();
        l15DaosModel();

        banner("结果: PASS=" + passed + "  FAIL=" + failed);
        System.exit(failed > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // 1. DAO 容器写路径（ds/dsrk/du/ddel → execNamed / execNamedReturnKeys）
    // ------------------------------------------------------------------

    private static void l01DaoSave() {
        Q q = new Q();
        DAO d = new DAO("demo_book");
        d.set("TITLE", "DAO老链路").set("AUTHOR", "dfr").set("STOCK", 7);
        q.save(d);
        Book found = findByTitle("DAO老链路");
        check("L01 Q.save(DAO) 落库", found != null && found.getStock() == 7);
    }

    private static void l02DaoSaveRK() {
        Q q = new Q();
        DAO d = new DAO("demo_book");
        d.set("TITLE", "DAO带主键").set("AUTHOR", "dfr").set("STOCK", 1);
        List<Object> keys = q.saveRK(d);
        check("L02 Q.saveRK(DAO) 返回自增主键", keys != null && keys.size() == 1 && keys.get(0) != null);
    }

    private static void l03DaoUpdate() throws SQLException {
        Q q = new Q();
        List<DAO> daos = q.daos("SELECT * FROM demo_book WHERE TITLE = 'DAO老链路'");
        check("L03 前置: daos 读到待更新记录", daos.size() == 1);
        DAO d = daos.get(0);
        d.set("STOCK", 9);
        q.update(d);
        Book found = findByTitle("DAO老链路");
        check("L03 Q.update(DAO) 按主键更新", found != null && found.getStock() == 9);
    }

    private static void l04DaoDelete() throws SQLException {
        Q q = new Q();
        // 老语义：ddel(DAO) 按 useFields 全字段拼 WHERE；手工构造 DAO 只带 TITLE 定位，
        // 避免加载态 DAO 中 NULL 列被映射为 "null" 字符串导致匹配不上（1.x 即如此，非本次改动引入）
        DAO d = new DAO("demo_book");
        d.set("TITLE", "DAO带主键");
        q.delete(d);
        check("L04 Q.delete(DAO) 删除", findByTitle("DAO带主键") == null);
    }

    // ------------------------------------------------------------------
    // 2. 原始 SQL 查询（pager(Class, sql) → queryLegacy）
    // ------------------------------------------------------------------

    private static void l05DaosRawSql() throws Exception {
        Q q = new Q();
        List<DAO> list = q.daos("SELECT * FROM demo_book WHERE AUTHOR = 'dfr' ORDER BY ID");
        boolean tableOk = list.size() > 0 && "demo_book".equalsIgnoreCase(list.get(0).getTableName());
        check("L05 daos(String) DAO 列表+表名回填", list.size() == 3 && tableOk);
        check("L05 DAO 字段读取", "dfr".equals(list.get(0).get("AUTHOR")));
    }

    private static void l06DaosEntitySql() throws Exception {
        Q q = new Q();
        // STOCK >= 8：Java 编程思想(10)、Effective Java(8)、DAO老链路(L03 改为 9)，共 3 行
        List list = q.daos(Book.class, "SELECT * FROM demo_book WHERE STOCK >= 8 ORDER BY ID");
        check("L06 daos(Class, sql) 实体映射", list.size() == 3
                && "Java 编程思想".equals(((Book) list.get(0)).getTitle()));
    }

    // ------------------------------------------------------------------
    // 3. Model 分页（dpa/pagerList 链 → queryLegacy + count）
    // ------------------------------------------------------------------

    private static void l07PagerModel() throws Exception {
        Q q = new Q();
        Model m = new Model("demo_book", "STOCK >= 0 ORDER BY ID");
        m.pageSize = 2;
        m.absPage = 1;
        Pager p = q.pager(Book.class, m);
        check("L07 pager(Class, Model) 第一页 2 行", p != null && p.getDatas().size() == 2);
        m.absPage = 3;
        Pager p3 = q.pager(Book.class, m);
        // 共 5 行（含 L01 新增），第 3 页 1 行
        check("L07 pager 第三页 1 行（末页）", p3 != null && p3.getDatas().size() == 1);
    }

    // ------------------------------------------------------------------
    // 4. SqlBuffer（命名占位 #name → ? 绑定）
    // ------------------------------------------------------------------

    private static void l08SqlBuffer() throws Exception {
        Q q = new Q();
        QueryBuffer qb = new QueryBuffer("SELECT * FROM demo_book WHERE STOCK >= #stock AND AUTHOR = #author");
        qb.setParam("stock", 3);
        qb.setParam("author", "dfr");
        List list = q.querys(Book.class, qb);
        check("L08 querys(Class, SqlBuffer) 命名参数绑定", list.size() == 2);

        // 老语义：getCountSql() 需先 countField(...) 指定统计字段（否则返回 null → count 为 0）
        QueryBuffer countQb = new QueryBuffer("SELECT * FROM demo_book WHERE STOCK >= #stock");
        countQb.countField("count(0)");
        countQb.setParam("stock", 8);
        int total = q.count(countQb);
        check("L08 count(SqlBuffer) 命名参数统计", total == 3);
    }

    private static void l09ExecSqlBuffer() {
        Q q = new Q();
        QueryBuffer qb = new QueryBuffer("UPDATE demo_book SET STOCK = STOCK + 1 WHERE TITLE = #title");
        qb.setParam("title", "Tarn 实战");
        int rows = q.exec(qb);
        check("L09 exec(SqlBuffer) 更新 1 行", rows == 1);
    }

    // ------------------------------------------------------------------
    // 5. RowMapper + like 语义（get → queryOneNamed，like #name 自动 %包装）
    // ------------------------------------------------------------------

    private static void l10RowMapperLike() throws Exception {
        Q q = new Q();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("title", "Java");
        Object result = q.getObj("SELECT * FROM demo_book WHERE TITLE like #title", params, new RowMapper() {
            public Object mapRow(ResultSet rs) throws SQLException {
                return rs.getString("TITLE");
            }
        });
        check("L10 getObj(like #title) 自动 %包装", "Java 编程思想".equals(result));
    }

    // ------------------------------------------------------------------
    // 6. 异常语义（update 参数缺失 → TarnDataException，等价老 TarnException）
    // ------------------------------------------------------------------

    private static void l11UpdateMissingParamThrows() {
        Q q = new Q();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("stock", 1);
        try {
            q.delete("UPDATE demo_book SET STOCK = #stock WHERE TITLE = #title", params);
            check("L11 update 参数缺失抛错", false);
        } catch (TarnDataException e) {
            check("L11 update 参数缺失抛 TarnDataException", e.getMessage() != null && e.getMessage().contains("title"));
        } catch (Exception e) {
            check("L11 update 参数缺失抛出意外异常 " + e.getClass().getSimpleName(), false);
        } catch (Throwable e) {
            // DaoException extends Throwable：走到这里说明抛了老受检异常而非新语义
            check("L11 update 参数缺失抛出意外 Throwable " + e.getClass().getSimpleName(), false);
        }
    }

    // ------------------------------------------------------------------
    // 7. pagerAbsolute（游标分页 → 内存切片等价语义）
    // ------------------------------------------------------------------

    private static void l12PagerAbsolute() {
        Q q = new Q();
        Pager p2 = q.pagerAbsolute(Book.class, "SELECT * FROM demo_book ORDER BY ID", 2, 2);
        boolean page2 = p2 != null && p2.getDatas().size() == 2
                && "Tarn 实战".equals(((Book) p2.getDatas().get(0)).getTitle());
        check("L12 pagerAbsolute 第 2 页 2 行", page2);
        Pager p9 = q.pagerAbsolute(Book.class, "SELECT * FROM demo_book ORDER BY ID", 2, 9);
        check("L12 pagerAbsolute 超页回退末页", p9 != null && p9.getDatas().size() == 1);
    }

    // ------------------------------------------------------------------
    // 8. get(Class, sql, map)（queryOneBean setter 名匹配）
    // ------------------------------------------------------------------

    private static void l13GetBySetters() {
        Q q = new Q();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("author", "dfr");
        Book book = q.get(Book.class, "SELECT ID as id, TITLE as title, AUTHOR as author, STOCK as stock FROM demo_book WHERE AUTHOR = #author", params);
        check("L13 get(Class, sql, map) setter 匹配", book != null && "dfr".equals(book.getAuthor()));
    }

    // ------------------------------------------------------------------
    // 9. Model 单条/多条（dd/dds → pagerList → queryLegacy）
    // ------------------------------------------------------------------

    private static void l14DaoModelSingle() throws Exception {
        Q q = new Q();
        Model m = new Model("demo_book", "TITLE = 'Effective Java'");
        DAO d = q.dao(m);
        check("L14 dao(Model) 单条 DAO", d != null && "Joshua Bloch".equals(d.get("AUTHOR")));
    }

    private static void l15DaosModel() throws Exception {
        Q q = new Q();
        Model m = new Model("demo_book", "AUTHOR = 'dfr' AND STOCK > 0");
        DAO[] arr = q.daos(m);
        check("L15 daos(Model) DAO 数组", arr != null && arr.length == 2);
        List list = q.daos(Book.class, new Model("demo_book", "AUTHOR = 'dfr' AND STOCK > 0"));
        check("L15 daos(Class, Model) 实体列表", list != null && list.size() == 2);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static void seed(String title, String author, int stock) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setStock(stock);
        Orm.template().insert(b);
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

    private static void banner(String s) {
        System.out.println("============================================================");
        System.out.println("  " + s);
        System.out.println("============================================================");
    }
}
