package examples.di.service;

/**
 * 订单服务：与 {@link InventoryService} 构成循环依赖示例。
 *
 * @author dfr
 */
public interface OrderService {

    /**
     * 下单：扣减库存并返回订单号
     *
     * @param sku 商品编码
     * @param qty 数量
     * @return 订单号；库存不足时返回失败原因
     */
    String placeOrder(String sku, int qty);

    /**
     * 暴露注入进来的库存服务，用于验证循环依赖已解析
     */
    InventoryService getInventoryService();
}
