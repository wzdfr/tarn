package examples.di;

import com.dfrdev.tarn.container.TarnApplicationContext;
import examples.di.component.AuditComponent;
import examples.di.component.OrderDraft;
import examples.di.component.TimingBeanPostProcessor;
import examples.di.service.InventoryService;
import examples.di.service.OrderService;
import examples.di.service.impl.InventoryServiceImpl;
import examples.di.service.impl.OrderServiceImpl;

import java.util.Arrays;

/**
 * 不依赖 Web 容器与数据库的编程式依赖注入示例。
 * <p>
 * 运行方式：
 * <ol>
 *     <li>IDE：直接运行本类 {@code main}（工作目录设为 {@code tarn-demo}）；</li>
 *     <li>命令行（Linux / macOS）：
 *     <pre>
 * mvn -q -pl tarn-demo -am compile
 * mvn -q -pl tarn-demo dependency:build-classpath -Dmdep.outputFile=/tmp/tarn-demo-cp.txt
 * java -cp "tarn-demo/target/classes:tarn-framework/target/classes:$(cat /tmp/tarn-demo-cp.txt)" \
 *      examples.di.DependencyInjectionDemo
 *     </pre>
 *     Windows 下 classpath 分隔符改为 {@code ;}。</li>
 * </ol>
 *
 * @author dfr
 */
public final class DependencyInjectionDemo {

    private DependencyInjectionDemo() {
    }

    public static void main(String[] args) {
        // 1. 创建一个隔离的容器（不影响全局 TarnApplicationContext.getInstance()）
        TarnApplicationContext context = new TarnApplicationContext();

        // 2. 编程式注册：名称 → 实现类
        context.registerSingleton("orderService", OrderServiceImpl.class);
        context.registerSingleton("inventoryService", InventoryServiceImpl.class);
        context.registerSingleton("auditComponent", AuditComponent.class);
        context.registerSingleton("timingBeanPostProcessor", TimingBeanPostProcessor.class);
        // 原型：每次获取都是新实例
        context.registerPrototype("orderDraft", OrderDraft.class);

        // 3. 初始化 BeanPostProcessor（扫描模式下由框架启动时自动调用）
        context.refresh();

        // 4. 按类型获取，容器完成注入与生命周期
        OrderService orderService = context.getBean(OrderService.class);
        InventoryService inventoryService = context.getBean(InventoryService.class);

        System.out.println("=== 1. 循环依赖（三级缓存）===");
        System.out.println("orderService.getInventoryService() == inventoryService : "
                + (orderService.getInventoryService() == inventoryService));
        System.out.println("inventoryService.getOrderService() == orderService : "
                + (inventoryService.getOrderService() == orderService));

        System.out.println();
        System.out.println("=== 2. 下单（Service 之间互相调用）===");
        System.out.println("初始库存 book-1 : " + inventoryService.stock("book-1"));
        System.out.println("下单结果        : " + orderService.placeOrder("book-1", 3));
        System.out.println("剩余库存 book-1 : " + inventoryService.stock("book-1"));
        System.out.println("库存不足场景    : " + orderService.placeOrder("book-3", 1));

        System.out.println();
        System.out.println("=== 3. 生命周期与 Aware ===");
        AuditComponent audit = context.getBean(AuditComponent.class);
        System.out.println("初始化状态 : " + audit.snapshot());

        System.out.println();
        System.out.println("=== 4. BeanPostProcessor ===");
        TimingBeanPostProcessor processor = context.getBean(TimingBeanPostProcessor.class);
        System.out.println("已处理 Bean : " + processor.stats());

        System.out.println();
        System.out.println("=== 5. 原型 Bean（每次新实例 + 自动注入）===");
        OrderDraft first = context.getBean("orderDraft", OrderDraft.class);
        OrderDraft second = context.getBean("orderDraft", OrderDraft.class);
        System.out.println("first  : id=" + first.getDraftId()
                + ", injected=" + first.isInjected()
                + ", book-1 stock=" + first.stockOf("book-1"));
        System.out.println("second : id=" + second.getDraftId()
                + ", injected=" + second.isInjected()
                + ", book-1 stock=" + second.stockOf("book-1"));
        System.out.println("是不同实例 : " + (first != second));

        System.out.println();
        System.out.println("=== 6. 容器信息 ===");
        System.out.println("Bean 定义 : " + Arrays.toString(context.getBeanDefinitionNames()));
        System.out.println("定义数量  : " + context.getBeanDefinitionCount());
        System.out.println("orderService 是单例 : " + context.isSingleton("orderService"));
        System.out.println("orderService 类型 Bean : "
                + context.getBeansOfType(OrderService.class).keySet());

        System.out.println();
        System.out.println("=== 7. 关闭容器（逆序执行 @EPreDestroy）===");
        context.close();
        System.out.println("audit.initialized = " + audit.isInitialized());
        System.out.println("audit.destroyed   = " + audit.isDestroyed());
    }
}
