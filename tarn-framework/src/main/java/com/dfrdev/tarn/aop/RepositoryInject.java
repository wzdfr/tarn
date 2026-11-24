package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.inject.ComponentRegistry;
import com.dfrdev.tarn.aop.inject.ComponentScanner;
import com.dfrdev.tarn.aop.service.RepositoryAOP;
import com.dfrdev.tarn.container.TarnApplicationContext;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.xml.XMLDocument;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.Serializable;
import java.util.HashMap;

/**
 * DAO 层入口（{@code @ERepository} / {@code @EResource}）。
 * <p>
 * Tarn 2.2 起，Repository 的创建、注入与生命周期统一由
 * {@link TarnApplicationContext} 负责；本类只保留注册、白名单与兼容 API。
 *
 * @author dfr
 */
public class RepositoryInject implements Serializable {
    private static final long serialVersionUID = 6392376146163510046L;

    private static final ComponentRegistry REGISTRY = new ComponentRegistry("repository");

    /**
     * 允许注入 Repository 的包白名单（context-mapping.xml 的 {@code <repositorys limitPackage="...">}）
     */
    private static String[] LIMIT_PACKAGE = null;

    static {
        Config.Repositorys repositorys = Config.getConfigContextMappingRepositorys();
        LIMIT_PACKAGE = parseLimitPackages(repositorys.getBase());
        XMLDocument report = ComponentScanner.scan("Repository实现类", repositorys.getRepositorys(),
                new RepositoryAOP(), "packages");
        ComponentScanner.write(report, "framework-mapping-repositorys.xml");
        LogUtil.info("[Tarn] Repository 注册完成，共 " + REGISTRY.size() + " 个");
    }

    /**
     * 为目标的 {@code @EResource} 字段注入 Repository，并执行生命周期回调。
     * <p>
     * 兼容旧调用入口；容器创建的 Bean 无需调用本方法。
     */
    public static void inject(Object obj) {
        TarnApplicationContext.getInstance().injectBean(obj);
    }

    /**
     * 注册 Repository 实现（供 {@code RepositoryAOP} 扫描时调用）
     */
    public static HashMap<String, String> setRepositoryImpl(String key, String value) {
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
     * 实现类是否落在 limitPackage 白名单内（未配置时全部允许）
     */
    public static boolean checkPackage(String impl) {
        String[] limitPackage = LIMIT_PACKAGE;
        if (limitPackage == null || limitPackage.length == 0) {
            return true;
        }
        if (StringUtil.isEmpty(impl)) {
            return false;
        }
        for (String limit : limitPackage) {
            if (StringUtil.isNotEmpty(limit) && impl.indexOf(limit) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按名称创建新的 Repository 实例（不参与单例缓存，也不执行注入与生命周期）
     */
    public static Object newInstance(String insName) {
        return REGISTRY.newInstance(insName);
    }

    public static ComponentRegistry getRegistry() {
        return REGISTRY;
    }

    private static String[] parseLimitPackages(XMLElement base) {
        if (base == null) {
            return null;
        }
        String limitPackage = base.attributeValue("limitPackage");
        if (StringUtil.isEmpty(limitPackage)) {
            return null;
        }
        String[] packages = limitPackage.split(",");
        for (int i = 0; i < packages.length; i++) {
            packages[i] = packages[i].trim();
        }
        return packages;
    }
}
