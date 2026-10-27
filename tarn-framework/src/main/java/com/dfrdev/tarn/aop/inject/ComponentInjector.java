package com.dfrdev.tarn.aop.inject;

import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.LogUtil;

import java.lang.annotation.Annotation;

/**
 * 注解驱动的字段注入器（Tarn 2.1）。
 * <p>
 * Service（{@code @EAutowired}）与 Repository（{@code @EResource}）的注入流程完全一致，
 * 差别只在注解类型、注册表与“实例创建后是否继续注入 Repository”这一后置动作，
 * 因此统一由本类承载，避免两份重复实现。
 *
 * @author dfr
 * @since 2.1
 */
public final class ComponentInjector {

    /**
     * 从注解中解析 Bean 名称（注解 value 为空时回退字段名）
     */
    public interface BeanNameParser {
        String parse(Annotation annotation, String fieldName);
    }

    /**
     * 实例创建后的回调（用于 Service 创建后继续注入其 Repository 依赖）
     */
    public interface CreatedCallback {
        void onCreated(Object instance, String implClassName);
    }

    private final Class<? extends Annotation> annotationType;
    private final ComponentRegistry registry;
    private final BeanNameParser parser;
    private final CreatedCallback callback;

    private final ComponentRegistry.Creator creator = new ComponentRegistry.Creator() {
        @Override
        public Object create(String implClassName) {
            Object instance = ComponentRegistry.instantiate(implClassName);
            if (instance != null && callback != null) {
                callback.onCreated(instance, implClassName);
            }
            return instance;
        }
    };

    public ComponentInjector(Class<? extends Annotation> annotationType, ComponentRegistry registry,
                             BeanNameParser parser, CreatedCallback callback) {
        this.annotationType = annotationType;
        this.registry = registry;
        this.parser = parser;
        this.callback = callback;
    }

    /**
     * 为目标对象的注解字段注入单例组件。
     * <p>
     * 属性无 setter 时跳过并输出警告（老实现直接抛 NoSuchMethodException 后打印堆栈）。
     */
    public void inject(Object target) {
        if (target == null) {
            throw new TarnException("Injection target is null.");
        }
        for (BeanProperty property : BeanProperties.of(target.getClass())) {
            Annotation annotation = property.getAnnotation(annotationType);
            if (annotation == null) {
                continue;
            }
            String beanName = parser.parse(annotation, property.getName());
            Object value = registry.instance(beanName, creator);
            if (!property.set(target, value)) {
                LogUtil.warn("[Tarn] " + target.getClass().getName() + "." + property.getName()
                        + " 无可用 setter，跳过注入 [" + registry.getScope() + ":" + beanName + "]");
            }
        }
    }

    public ComponentRegistry getRegistry() {
        return registry;
    }
}
