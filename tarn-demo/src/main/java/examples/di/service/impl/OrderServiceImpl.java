package examples.di.service.impl;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EService;
import examples.di.service.InventoryService;
import examples.di.service.OrderService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单服务实现。
 * <p>
 * 循环依赖：本类注入 {@link InventoryService}，而 {@code InventoryServiceImpl} 又注入
 * {@link OrderService}。容器在实例化后、属性注入前就把早期引用放入三级缓存，
 * 因此可以正常完成创建。
 * <p>
 * 注意：{@code @EService("orderService")} 显式指定 Bean 名称，字段名与之保持一致，
 * 也可以不写 value（默认名称首字母小写：orderServiceImpl），此时按类型回退注入。
 *
 * @author dfr
 */
@EService("orderService")
public class OrderServiceImpl implements OrderService {

    private static final AtomicInteger ORDER_SEQ = new AtomicInteger();

    /**
     * 循环依赖：订单服务依赖库存服务
     */
    @EAutowired
    private InventoryService inventoryService;

    @Override
    public String placeOrder(String sku, int qty) {
        if (sku == null || sku.trim().isEmpty()) {
            return "商品编码不能为空";
        }
        if (qty <= 0) {
            return "数量必须大于 0";
        }
        if (!inventoryService.deduct(sku, qty)) {
            return "库存不足: " + sku + " 剩余 " + inventoryService.stock(sku);
        }
        return "ORDER-" + ORDER_SEQ.incrementAndGet() + " (sku=" + sku + ", qty=" + qty
                + ", 剩余库存=" + inventoryService.stock(sku) + ")";
    }

    @Override
    public InventoryService getInventoryService() {
        return inventoryService;
    }

    /**
     * 便于演示查看：当前订单号计数
     */
    public static int orderCount() {
        return ORDER_SEQ.get();
    }
}
