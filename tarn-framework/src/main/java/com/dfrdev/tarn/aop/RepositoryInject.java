package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.annotation.EResource;
import com.dfrdev.tarn.aop.inject.ComponentInjector;
import com.dfrdev.tarn.aop.inject.ComponentRegistry;
import com.dfrdev.tarn.aop.inject.ComponentScanner;
import com.dfrdev.tarn.aop.service.RepositoryAOP;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.xml.XMLDocument;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;

/**
 * DAO 层注入（{@code @ERepository} / {@code @EResource}）。
 * <p>
 * Tarn 2.1 重构：与 {@link ServiceInject} 共用扫描/注册/注入组件，只保留对外静态 API。
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

    private static final ComponentInjector INJECTOR = new ComponentInjector(
            EResource.class, REGISTRY,
            new ComponentInjector.BeanNameParser() {
                @Override
                public String parse(Annotation annotation, String fieldName) {
                    String value = ((EResource) annotation).value();
                    return StringUtil.isNotEmpty(value) ? value : fieldName;
                }
            },
            null);

    static {
        Config.Repositorys repositorys = Config.getConfigContextMappingRepositorys();
        LIMIT_PACKAGE = parseLimitPackages(repositorys.getBase());
        XMLDocument report = ComponentScanner.scan("Repository实现类", repositorys.getRepositorys(),
                new RepositoryAOP(), "packages");
        ComponentScanner.write(report, "framework-mapping-repositorys.xml");
        LogUtil.info("[Tarn] Repository 注册完成，共 " + REGISTRY.size() + " 个");
    }

    /**
     * 为目标的 {@code @EResource} 字段注入 Repository 单例
     */
    public static void inject(Object obj) {
        INJECTOR.inject(obj);
    }

    /**
     * 注册 Repository 实现（供 {@code RepositoryAOP} 扫描时调用）
     */
    public static HashMap<String, String> setRepositoryImpl(String key, String value) {
        REGISTRY.register(key, value);
        return REGISTRY.impls();
    }

    /**
     * 实现类是否落在 limitPackage 白名单内（未配置时全部允许）
     */
    public static boolean checkPackage(String impl) {
        if (LIMIT_PACKAGE == null || LIMIT_PACKAGE.length == 0) {
            return true;
        }
        if (StringUtil.isEmpty(impl)) {
            return false;
        }
        for (String limit : LIMIT_PACKAGE) {
            if (StringUtil.isNotEmpty(limit) && impl.indexOf(limit) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按名称创建新的 Repository 实例（不参与单例缓存）
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
