package examples.di.component;

import com.dfrdev.tarn.aop.annotation.EComponent;
import com.dfrdev.tarn.container.BeanPostProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bean 后置处理器示例。
 * <p>
 * 标注 {@code @EComponent} 并实现 {@link BeanPostProcessor} 后，
 * 容器在 {@code refresh()} 阶段会实例化它并加入处理器列表，
 * 之后创建的每个 Bean 都会经过 before / after 回调。
 *
 * @author dfr
 */
@EComponent("timingBeanPostProcessor")
public class TimingBeanPostProcessor implements BeanPostProcessor {

    private final AtomicInteger beforeCount = new AtomicInteger();
    private final AtomicInteger afterCount = new AtomicInteger();
    private final Set<String> beforeBeans = ConcurrentHashMap.newKeySet();
    private final Set<String> afterBeans = ConcurrentHashMap.newKeySet();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        beforeCount.incrementAndGet();
        beforeBeans.add(beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        afterCount.incrementAndGet();
        afterBeans.add(beanName);
        return bean;
    }

    /**
     * 供控制器/示例程序查询处理统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("beforeCount", beforeCount.get());
        data.put("afterCount", afterCount.get());
        data.put("beforeBeans", new ArrayList<String>(beforeBeans));
        data.put("afterBeans", new ArrayList<String>(afterBeans));
        return data;
    }

    public List<String> processedBeans() {
        return new ArrayList<String>(afterBeans);
    }
}
