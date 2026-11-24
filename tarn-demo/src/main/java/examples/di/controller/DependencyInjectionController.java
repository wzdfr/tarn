package examples.di.controller;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.container.ApplicationContext;
import com.dfrdev.tarn.container.ApplicationContextAware;
import com.dfrdev.tarn.container.TarnApplicationContext;
import com.dfrdev.tarn.json.JsonResult;
import examples.di.component.AuditComponent;
import examples.di.component.OrderDraft;
import examples.di.component.TimingBeanPostProcessor;
import examples.di.service.InventoryService;
import examples.di.service.OrderService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 依赖注入演示控制器（路由前缀 {@code /di}）。
 * <p>
 * 演示内容：
 * <ul>
 *     <li>Controller 由容器以原型作用域创建，字段自动注入；</li>
 *     <li>Service 之间循环依赖；</li>
 *     <li>{@code @EComponent} 的生命周期与 Aware 回调；</li>
 *     <li>BeanPostProcessor 统计；</li>
 *     <li>通过容器 API 查询 Bean 与创建原型对象。</li>
 * </ul>
 * 启动独立服务：运行 {@code examples.standalone.StandaloneApp#main}，
 * 然后访问 {@code http://localhost:8080/di/context} 等接口。
 *
 * @author dfr
 */
@EController("/di")
public class DependencyInjectionController implements ApplicationContextAware {

    @EAutowired
    private OrderService orderService;

    @EAutowired
    private InventoryService inventoryService;

    @EAutowired
    private AuditComponent auditComponent;

    @EAutowired
    private TimingBeanPostProcessor timingBeanPostProcessor;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * GET /di/order?sku=book-1&qty=2
     * <p>
     * 下单并展示循环依赖注入后的库存服务调用结果。
     */
    @Json
    @ERequestMapping(value = "/order", method = RequestMethod.GET)
    public JsonResult order(@ERequestParam("sku") String sku,
                            @ERequestParam("qty") Integer qty) {
        String finalSku = (sku == null || sku.trim().isEmpty()) ? "book-1" : sku;
        int finalQty = (qty == null || qty <= 0) ? 1 : qty;

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("sku", finalSku);
        data.put("qty", finalQty);
        data.put("beforeStock", inventoryService.stock(finalSku));
        data.put("result", orderService.placeOrder(finalSku, finalQty));
        data.put("afterStock", inventoryService.stock(finalSku));
        return JsonResult.ok(data);
    }

    /**
     * GET /di/circular
     * <p>
     * 验证 OrderService ↔ InventoryService 的循环依赖已被三级缓存解析。
     */
    @Json
    @ERequestMapping(value = "/circular", method = RequestMethod.GET)
    public JsonResult circular() {
        boolean orderToInventory = orderService.getInventoryService() == inventoryService;
        boolean inventoryToOrder = inventoryService.getOrderService() == orderService;

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("orderServiceClass", orderService.getClass().getSimpleName());
        data.put("inventoryServiceClass", inventoryService.getClass().getSimpleName());
        data.put("orderService.inventoryService == controller.inventoryService", orderToInventory);
        data.put("inventoryService.orderService == controller.orderService", inventoryToOrder);
        data.put("resolved", orderToInventory && inventoryToOrder);
        return JsonResult.ok(data);
    }

    /**
     * GET /di/lifecycle
     * <p>
     * 查看 {@link AuditComponent} 的 BeanNameAware / ApplicationContextAware /
     * {@code @EPostConstruct} 执行结果。
     */
    @Json
    @ERequestMapping(value = "/lifecycle", method = RequestMethod.GET)
    public JsonResult lifecycle() {
        return JsonResult.ok(auditComponent.snapshot());
    }

    /**
     * GET /di/post-processor
     * <p>
     * 查看 {@link TimingBeanPostProcessor} 统计的初始化前后回调次数。
     */
    @Json
    @ERequestMapping(value = "/post-processor", method = RequestMethod.GET)
    public JsonResult postProcessor() {
        return JsonResult.ok(timingBeanPostProcessor.stats());
    }

    /**
     * GET /di/context
     * <p>
     * 通过 {@link ApplicationContext} API 查询容器信息。
     */
    @Json
    @ERequestMapping(value = "/context", method = RequestMethod.GET)
    public JsonResult context() {
        ApplicationContext context = currentContext();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("beanCount", context.getBeanDefinitionCount());
        data.put("beanNames", sorted(context.getBeanDefinitionNames()));
        data.put("containsOrderService", context.containsBean("orderService"));
        data.put("orderServiceIsSingleton", context.isSingleton("orderService"));
        data.put("orderServiceBeans", context.getBeansOfType(OrderService.class).keySet());
        return JsonResult.ok(data);
    }

    /**
     * GET /di/draft
     * <p>
     * 使用 {@code createBean} 创建两个原型对象，验证每次都是新实例且都完成注入。
     */
    @Json
    @ERequestMapping(value = "/draft", method = RequestMethod.GET)
    public JsonResult draft() {
        TarnApplicationContext context = (TarnApplicationContext) currentContext();
        OrderDraft first = (OrderDraft) context.createBean(OrderDraft.class);
        OrderDraft second = (OrderDraft) context.createBean(OrderDraft.class);

        Map<String, Object> firstData = new LinkedHashMap<String, Object>();
        firstData.put("draftId", first.getDraftId());
        firstData.put("injected", first.isInjected());
        firstData.put("book1Stock", first.stockOf("book-1"));

        Map<String, Object> secondData = new LinkedHashMap<String, Object>();
        secondData.put("draftId", second.getDraftId());
        secondData.put("injected", second.isInjected());
        secondData.put("book1Stock", second.stockOf("book-1"));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("first", firstData);
        data.put("second", secondData);
        data.put("differentInstance", first != second);
        return JsonResult.ok(data);
    }

    private ApplicationContext currentContext() {
        return applicationContext == null ? TarnApplicationContext.getInstance() : applicationContext;
    }

    private String[] sorted(String[] names) {
        String[] copy = names.clone();
        Arrays.sort(copy);
        return copy;
    }
}
