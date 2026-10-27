package examples.orm.entity;

import com.dfrdev.tarn.aop.annotation.Ekey;
import com.dfrdev.tarn.aop.annotation.Etable;

import java.math.BigDecimal;

/**
 * ORM 使用示例实体：订单（演示事务场景「下单扣库存」）。
 *
 * <pre>
 * CREATE TABLE demo_order (
 *     ID       BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     ORDERNO  VARCHAR(64) NOT NULL,
 *     BOOKID   BIGINT NOT NULL,
 *     QTY      INT NOT NULL DEFAULT 1,
 *     AMOUNT   DECIMAL(10,2) NOT NULL DEFAULT 0
 * );
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
@Etable("demo_order")
public class DemoOrder {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String orderNo;

    private Long bookId;

    private Integer qty;

    /** BigDecimal 是简单类型（java.math.*），直接参数绑定，不走 JSON 序列化 */
    private BigDecimal amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "DemoOrder{id=" + id + ", orderNo='" + orderNo + "', bookId=" + bookId
                + ", qty=" + qty + ", amount=" + amount + "}";
    }
}
