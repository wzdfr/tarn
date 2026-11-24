package examples.di.component;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EComponent;
import com.dfrdev.tarn.aop.annotation.EPostConstruct;
import com.dfrdev.tarn.aop.annotation.EPreDestroy;
import com.dfrdev.tarn.container.ApplicationContext;
import com.dfrdev.tarn.container.ApplicationContextAware;
import com.dfrdev.tarn.container.BeanNameAware;
import examples.di.service.OrderService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计组件：演示 {@code @EComponent} + 生命周期 + Aware 回调。
 * <p>
 * 执行顺序：
 * <pre>
 * 字段注入（orderService）
 *   → BeanNameAware.setBeanName
 *   → ApplicationContextAware.setApplicationContext
 *   → @EPostConstruct
 *   → 容器关闭时 @EPreDestroy
 * </pre>
 *
 * @author dfr
 */
@EComponent("auditComponent")
public class AuditComponent implements BeanNameAware, ApplicationContextAware {

    /**
     * 通用组件同样可以注入 Service
     */
    @EAutowired
    private OrderService orderService;

    private String beanName;
    private boolean contextReady;
    private boolean initialized;
    private boolean destroyed;
    private String initTime;

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.contextReady = applicationContext != null;
    }

    @EPostConstruct
    public void init() {
        this.initialized = true;
        this.initTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    @EPreDestroy
    public void destroy() {
        this.destroyed = true;
    }

    /**
     * 供控制器/示例程序查询生命周期状态
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("beanName", beanName);
        data.put("contextReady", contextReady);
        data.put("initialized", initialized);
        data.put("destroyed", destroyed);
        data.put("initTime", initTime);
        data.put("orderServiceInjected", orderService != null);
        data.put("orderServiceClass", orderService == null ? null : orderService.getClass().getSimpleName());
        return data;
    }

    /**
     * 便于示例程序判断是否已注入
     */
    public boolean isOrderServiceInjected() {
        return orderService != null;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
