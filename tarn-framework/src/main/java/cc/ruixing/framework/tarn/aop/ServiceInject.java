package cc.ruixing.framework.tarn.aop;


import cc.ruixing.framework.tarn.aop.annotation.EAutowired;
import cc.ruixing.framework.tarn.aop.inject.ComponentInjector;
import cc.ruixing.framework.tarn.aop.inject.ComponentRegistry;
import cc.ruixing.framework.tarn.aop.inject.ComponentScanner;
import cc.ruixing.framework.tarn.aop.service.ServiceAOP;
import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.StringUtil;
import cc.ruixing.framework.tarn.util.xml.XMLDocument;
import cc.ruixing.framework.tarn.util.xml.XMLElement;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;

/**
 * 服务注入（{@code @EService} / {@code @EAutowired}）。
 * <p>
 * Tarn 2.1 重构：扫描与注入逻辑统一收敛到
 * {@link ComponentScanner} / {@link ComponentRegistry} / {@link ComponentInjector}，
 * 本类只保留对外静态 API（老项目无感知）。
 * <p>
 * 静态初始化链路保持不变：{@code Inject} → {@code new ServiceInject()} →
 * 扫描 Service 包 → 若配置了 Repository 包则初始化 {@code RepositoryInject}。
 *
 * @author dfr
 */
public class ServiceInject implements Serializable {
    private static final long serialVersionUID = 6392376146163710046L;

    private static final ComponentRegistry REGISTRY = new ComponentRegistry("service");

    /**
     * 是否需要注入数据 Bean（Repository）服务
     */
    private static boolean checkRepository = false;

    private static final ComponentInjector INJECTOR = new ComponentInjector(
            EAutowired.class, REGISTRY,
            new ComponentInjector.BeanNameParser() {
                @Override
                public String parse(Annotation annotation, String fieldName) {
                    String value = ((EAutowired) annotation).value();
                    return StringUtil.isNotEmpty(value) ? value : fieldName;
                }
            },
            new ComponentInjector.CreatedCallback() {
                @Override
                public void onCreated(Object instance, String implClassName) {
                    // Service 实例创建后继续注入其 @EResource 依赖（保持老框架行为）
                    if (checkRepository && RepositoryInject.checkPackage(implClassName)) {
                        RepositoryInject.inject(instance);
                    }
                }
            });

    static {
        List<XMLElement> packages = Config.getConfigContextMappingServices();
        XMLDocument report = ComponentScanner.scan("Service实现类", packages, new ServiceAOP(), "packages");
        List<XMLElement> repositories = Config.getConfigContextMappingRepository();
        checkRepository = repositories != null && !repositories.isEmpty();
        if (checkRepository) {
            new RepositoryInject();
        }
        ComponentScanner.write(report, "framework-mapping-services.xml");
        LogUtil.info("[Tarn] Service 注册完成，共 " + REGISTRY.size() + " 个");
    }

    /**
     * 为目标的 {@code @EAutowired} 字段注入 Service 单例
     */
    public static void inject(Object obj) {
        INJECTOR.inject(obj);
    }

    /**
     * 注册 Service 实现（供 {@code ServiceAOP} 扫描时调用）
     */
    public static HashMap<String, String> setServiceImpl(String key, String value) {
        REGISTRY.register(key, value);
        return REGISTRY.impls();
    }

    /**
     * 按名称创建新的 Service 实例（不参与单例缓存）
     */
    public static Object newInstance(String insName) {
        return REGISTRY.newInstance(insName);
    }

    public static ComponentRegistry getRegistry() {
        return REGISTRY;
    }
}
