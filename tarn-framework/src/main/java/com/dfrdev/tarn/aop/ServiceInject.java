package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.inject.ComponentRegistry;
import com.dfrdev.tarn.aop.inject.ComponentScanner;
import com.dfrdev.tarn.aop.service.ServiceAOP;
import com.dfrdev.tarn.container.TarnApplicationContext;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.xml.XMLDocument;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * 服务入口（{@code @EService} / {@code @EAutowired}）。
 * <p>
 * Tarn 2.2 起，Service 的创建、注入与生命周期统一由
 * {@link TarnApplicationContext} 负责；本类只保留注册与兼容 API。
 * <p>
 * 静态初始化链路保持不变：{@code Inject} → {@code new ServiceInject()} →
 * 扫描 Service 包 → 若配置了 Repository 包则初始化 {@code RepositoryInject}。
 *
 * @author dfr
 */
public class ServiceInject implements Serializable {
    private static final long serialVersionUID = 6392376146163710046L;

    private static final ComponentRegistry REGISTRY = new ComponentRegistry("service");

    static {
        List<XMLElement> packages = Config.getConfigContextMappingServices();
        XMLDocument report = ComponentScanner.scan("Service实现类", packages, new ServiceAOP(), "packages");
        List<XMLElement> repositories = Config.getConfigContextMappingRepository();
        if (repositories != null && !repositories.isEmpty()) {
            new RepositoryInject();
        }
        ComponentScanner.write(report, "framework-mapping-services.xml");
        LogUtil.info("[Tarn] Service 注册完成，共 " + REGISTRY.size() + " 个");
    }

    /**
     * 为目标的 {@code @EAutowired} / {@code @EResource} 字段注入依赖并执行生命周期回调。
     * <p>
     * 兼容旧调用入口；容器创建的 Bean 无需调用本方法。
     */
    public static void inject(Object obj) {
        TarnApplicationContext.getInstance().injectBean(obj);
    }

    /**
     * 注册 Service 实现（供 {@code ServiceAOP} 扫描时调用）
     */
    public static HashMap<String, String> setServiceImpl(String key, String value) {
        if (StringUtil.isEmpty(value)) {
            return REGISTRY.impls();
        }
        String beanName = StringUtil.isNotEmpty(key) ? key.trim()
                : TarnApplicationContext.defaultBeanName(null, value);
        if (StringUtil.isEmpty(beanName)) {
            return REGISTRY.impls();
        }
        REGISTRY.register(beanName, value);
        TarnApplicationContext.getInstance().registerSingleton(beanName, value);
        return REGISTRY.impls();
    }

    /**
     * 按名称创建新的 Service 实例（不参与单例缓存，也不执行注入与生命周期）
     */
    public static Object newInstance(String insName) {
        return REGISTRY.newInstance(insName);
    }

    public static ComponentRegistry getRegistry() {
        return REGISTRY;
    }
}
