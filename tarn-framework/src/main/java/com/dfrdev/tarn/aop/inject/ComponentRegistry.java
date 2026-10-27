package com.dfrdev.tarn.aop.inject;

import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 组件注册表（Tarn 2.1）：Bean 名称 → 实现类 → 单例实例。
 * <p>
 * {@code ServiceInject}（{@code @EService}）与 {@code RepositoryInject}（{@code @ERepository}）
 * 各持有一个实例，替换原先两份重复的 {@code HashMap + newInstance()} 逻辑。
 * <p>
 * 线程安全：注册与单例创建均使用 {@link ConcurrentHashMap}，创建过程加锁避免重复实例化。
 *
 * @author dfr
 * @since 2.1
 */
public final class ComponentRegistry {

    /**
     * 实例工厂：由调用方决定如何创建（可附加创建后处理，如给 Service 注入 Repository）
     */
    public interface Creator {
        Object create(String implClassName);
    }

    private final String scope;
    private final ConcurrentMap<String, String> implNames = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, Object> instances = new ConcurrentHashMap<String, Object>();

    /**
     * @param scope 作用域名称（service / repository），仅用于日志
     */
    public ComponentRegistry(String scope) {
        this.scope = scope;
    }

    /**
     * 注册 Bean 名称与实现类全名
     */
    public void register(String beanName, String implClassName) {
        if (StringUtil.isEmpty(beanName) || StringUtil.isEmpty(implClassName)) {
            return;
        }
        implNames.put(beanName.trim(), implClassName.trim());
    }

    public String implOf(String beanName) {
        return beanName == null ? null : implNames.get(beanName);
    }

    public Object instanceOf(String beanName) {
        return beanName == null ? null : instances.get(beanName);
    }

    /**
     * 取单例，不存在则用给定工厂创建并缓存。
     *
     * @return 未注册该 Bean 时返回 null（保持老框架“注入 null”的行为）
     */
    public Object instance(String beanName, Creator creator) {
        Object exists = instanceOf(beanName);
        if (exists != null) {
            return exists;
        }
        String impl = implOf(beanName);
        if (StringUtil.isEmpty(impl)) {
            return null;
        }
        synchronized (this) {
            exists = instanceOf(beanName);
            if (exists != null) {
                return exists;
            }
            Object created = creator == null ? instantiate(impl) : creator.create(impl);
            if (created != null) {
                instances.put(beanName, created);
            }
            return created;
        }
    }

    /**
     * 按名称创建新实例（不写入单例缓存，等价老 {@code newInstance(name)}）
     */
    public Object newInstance(String beanName) {
        String impl = implOf(beanName);
        return StringUtil.isEmpty(impl) ? null : instantiate(impl);
    }

    /**
     * 反射实例化，失败仅记录错误并返回 null（与老框架容错行为一致）
     */
    public static Object instantiate(String implClassName) {
        try {
            return Class.forName(implClassName).newInstance();
        } catch (Exception e) {
            LogUtil.error("[Tarn] 组件实例化失败 " + implClassName + ": " + e.getMessage());
            return null;
        }
    }

    public boolean contains(String beanName) {
        return beanName != null && implNames.containsKey(beanName);
    }

    public int size() {
        return implNames.size();
    }

    public String getScope() {
        return scope;
    }

    /**
     * 已注册的实现清单快照（兼容老 API 返回 HashMap 的约定）
     */
    public HashMap<String, String> impls() {
        return new HashMap<String, String>(implNames);
    }

    /**
     * 已创建的单例快照（诊断用）
     */
    public Map<String, Object> instances() {
        return new HashMap<String, Object>(instances);
    }
}
