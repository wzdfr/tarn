package examples.di.service;

/**
 * 库存服务：与 {@link OrderService} 互相注入，用于演示三级缓存解决循环依赖。
 *
 * @author dfr
 */
public interface InventoryService {

    /**
     * 查询库存
     */
    int stock(String sku);

    /**
     * 扣减库存
     *
     * @return 是否扣减成功
     */
    boolean deduct(String sku, int qty);

    /**
     * 暴露注入进来的订单服务，用于验证循环依赖已解析
     */
    OrderService getOrderService();
}
