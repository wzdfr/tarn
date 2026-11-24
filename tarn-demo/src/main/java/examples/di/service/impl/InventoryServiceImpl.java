package examples.di.service.impl;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EService;
import examples.di.service.InventoryService;
import examples.di.service.OrderService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库存服务实现（内存数据，便于独立运行）。
 * <p>
 * 与 {@link OrderServiceImpl} 构成循环依赖：
 * {@code OrderServiceImpl → InventoryService}，
 * {@code InventoryServiceImpl → OrderService}。
 *
 * @author dfr
 */
@EService("inventoryService")
public class InventoryServiceImpl implements InventoryService {

    private final Map<String, Integer> stocks = new ConcurrentHashMap<String, Integer>();

    /**
     * 循环依赖：库存服务反向依赖订单服务
     */
    @EAutowired
    private OrderService orderService;

    public InventoryServiceImpl() {
        stocks.put("book-1", 10);
        stocks.put("book-2", 5);
        stocks.put("book-3", 0);
    }

    @Override
    public int stock(String sku) {
        Integer value = stocks.get(sku);
        return value == null ? 0 : value;
    }

    @Override
    public synchronized boolean deduct(String sku, int qty) {
        Integer current = stocks.get(sku);
        if (current == null || current < qty) {
            return false;
        }
        stocks.put(sku, current - qty);
        return true;
    }

    @Override
    public OrderService getOrderService() {
        return orderService;
    }
}
