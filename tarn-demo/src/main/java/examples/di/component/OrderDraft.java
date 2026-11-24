package examples.di.component;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import examples.di.service.InventoryService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 原型 Bean 示例：不加任何容器注解，由
 * {@code TarnApplicationContext#registerPrototype} 或 {@code createBean} 创建。
 * <p>
 * 每次获取都会得到新实例，同时完成依赖注入。
 *
 * @author dfr
 */
public class OrderDraft {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final int draftId = SEQ.incrementAndGet();

    @EAutowired
    private InventoryService inventoryService;

    public int getDraftId() {
        return draftId;
    }

    public int stockOf(String sku) {
        return inventoryService == null ? -1 : inventoryService.stock(sku);
    }

    public boolean isInjected() {
        return inventoryService != null;
    }
}
