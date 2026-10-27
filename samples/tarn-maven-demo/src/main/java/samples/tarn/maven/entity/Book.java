package samples.tarn.maven.entity;

import cc.ruixing.framework.tarn.aop.annotation.Ekey;
import cc.ruixing.framework.tarn.aop.annotation.Etable;

/**
 * 实体示例：图书。
 * <p>
 * Tarn ORM 列名规则：字段名大写（{@code title} → {@code TITLE}）。
 *
 * <pre>
 * CREATE TABLE demo_book (
 *     ID     BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     TITLE  VARCHAR(128) NOT NULL,
 *     AUTHOR VARCHAR(64),
 *     STOCK  INT DEFAULT 0
 * );
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
@Etable("demo_book")
public class Book {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String title;

    private String author;

    private Integer stock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
