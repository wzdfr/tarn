package com.dfrdev.tarn.container;

import com.dfrdev.tarn.aop.RepositoryInject;
import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EPreDestroy;
import com.dfrdev.tarn.aop.annotation.EPostConstruct;
import com.dfrdev.tarn.aop.annotation.EResource;
import com.dfrdev.tarn.aop.inject.BeanProperties;
import com.dfrdev.tarn.aop.inject.BeanProperty;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tarn 应用上下文（Tarn 2.2）。
 * <p>
 * 核心能力：
 * <ul>
 *     <li>统一管理 {@code @EService} / {@code @ERepository} / {@code @EComponent} 以及 Controller；</li>
 *     <li>字段注入：{@code @EAutowired}（服务/组件）与 {@code @EResource}（数据层）；</li>
 *     <li>生命周期：{@code @EPostConstruct} / {@code javax.annotation.PostConstruct}、
 *         {@link InitializingBean}，以及销毁阶段 {@code @EPreDestroy} / {@link DisposableBean}；</li>
 *     <li>{@link BeanPostProcessor} 前置/后置处理；</li>
 *     <li>三级缓存解决单例循环依赖。</li>
 * </ul>
 *
 * <h3>三级缓存</h3>
 * <pre>
 * singletonObjects       一级：完全初始化的单例
 * earlySingletonObjects  二级：提前暴露的原始实例（尚未完成属性注入/初始化）
 * singletonFactories     三级：ObjectFactory，按需产出早期引用
 * </pre>
 * 与 Spring 一致：实例化完成后先把工厂放入三级缓存，再执行属性注入，
 * 因此 A→B→A 的循环依赖可以在 B 注入 A 时拿到 A 的早期引用。
 *
 * @author dfr
 * @since 2.2
 */
public final class TarnApplicationContext implements ApplicationContext {

    private static final TarnApplicationContext INSTANCE = new TarnApplicationContext();

    private static final String[] POST_CONSTRUCT_ANNOTATIONS = {
            EPostConstruct.class.getName(),
            "javax.annotation.PostConstruct",
            "jakarta.annotation.PostConstruct"
    };

    private static final String[] PRE_DESTROY_ANNOTATIONS = {
            EPreDestroy.class.getName(),
            "javax.annotation.PreDestroy",
            "jakarta.annotation.PreDestroy"
    };

    /**
     * 早期引用工厂
     */
    private interface ObjectFactory {
        Object getObject();
    }

    private final ConcurrentMap<String, BeanDefinition> definitions = new ConcurrentHashMap<String, BeanDefinition>();

    /** 一级缓存：完整单例 */
    private final ConcurrentMap<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();
    /** 二级缓存：提前暴露的原始对象 */
    private final ConcurrentMap<String, Object> earlySingletonObjects = new ConcurrentHashMap<String, Object>();
    /** 三级缓存：早期引用工厂 */
    private final ConcurrentMap<String, ObjectFactory> singletonFactories = new ConcurrentHashMap<String, ObjectFactory>();

    /** 正在创建的单例（用于识别无法通过字段注入解析的构造期循环） */
    private final Set<String> singletonsInCreation =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    /** 当前线程正在创建的原型（用于识别原型循环依赖） */
    private final ThreadLocal<Set<String>> prototypesInCreation = new ThreadLocal<Set<String>>() {
        @Override
        protected Set<String> initialValue() {
            return new HashSet<String>();
        }
    };

    private final List<BeanPostProcessor> postProcessors = new CopyOnWriteArrayList<BeanPostProcessor>();
    private final List<String> creationOrder = new CopyOnWriteArrayList<String>();

    private final Object singletonLock = new Object();

    private volatile boolean closed = false;

    public TarnApplicationContext() {
    }

    /**
     * 框架全局容器
     */
    public static TarnApplicationContext getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------------

    /**
     * 注册单例（名称为空时按类名推导）
     */
    public void registerSingleton(String name, String className) {
        registerBeanDefinition(name, className, BeanDefinition.Scope.SINGLETON);
    }

    /**
     * 注册单例（名称为空时按类名推导）
     */
    public void registerSingleton(String name, Class<?> type) {
        if (type == null) {
            return;
        }
        registerBeanDefinition(name, type, BeanDefinition.Scope.SINGLETON);
    }

    /**
     * 注册原型
     */
    public void registerPrototype(String name, String className) {
        registerBeanDefinition(name, className, BeanDefinition.Scope.PROTOTYPE);
    }

    /**
     * 注册原型（名称为空时按类名推导）
     */
    public void registerPrototype(String name, Class<?> type) {
        if (type == null) {
            return;
        }
        registerBeanDefinition(name, type, BeanDefinition.Scope.PROTOTYPE);
    }

    /**
     * 注册 Bean 定义。名称冲突时保留先注册的定义并输出告警。
     */
    public void registerBeanDefinition(String name, String className, BeanDefinition.Scope scope) {
        if (StringUtil.isEmpty(className)) {
            return;
        }
        Class<?> type = null;
        try {
            type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (Throwable ignore) {
            // 延迟到首次获取时解析
        }
        String beanName = StringUtil.isNotEmpty(name) ? name.trim() : defaultBeanName(type, className);
        if (StringUtil.isEmpty(beanName)) {
            return;
        }
        registerBeanDefinition(beanName, type, className, scope);
    }

    /**
     * 注册 Bean 定义（直接持有 Class，避免字符串解析）
     */
    public void registerBeanDefinition(String name, Class<?> type, BeanDefinition.Scope scope) {
        if (type == null) {
            return;
        }
        String beanName = StringUtil.isNotEmpty(name) ? name.trim() : defaultBeanName(type, type.getName());
        registerBeanDefinition(beanName, type, type.getName(), scope);
    }

    private void registerBeanDefinition(String name, Class<?> type, String className,
                                        BeanDefinition.Scope scope) {
        if (StringUtil.isEmpty(name)) {
            return;
        }
        BeanDefinition definition = type == null
                ? new BeanDefinition(name, className, scope)
                : new BeanDefinition(name, type, scope);
        BeanDefinition previous = definitions.putIfAbsent(name, definition);
        if (previous != null && !previous.getClassName().equals(className)) {
            LogUtil.warn("[Tarn] Bean 名称冲突 '" + name + "'：" + previous.getClassName()
                    + " 与 " + className + "，保留前者");
        }
    }

    /**
     * 默认 Bean 名称：首字母小写的简单类名
     */
    public static String defaultBeanName(Class<?> type, String className) {
        String simple = null;
        if (type != null) {
            simple = type.getSimpleName();
        } else if (StringUtil.isNotEmpty(className)) {
            int index = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));
            simple = index >= 0 ? className.substring(index + 1) : className;
        }
        if (StringUtil.isEmpty(simple)) {
            return null;
        }
        if (simple.length() > 1 && Character.isUpperCase(simple.charAt(0))
                && Character.isUpperCase(simple.charAt(1))) {
            return simple;
        }
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    // ------------------------------------------------------------------
    // 获取
    // ------------------------------------------------------------------

    @Override
    public Object getBean(String name) {
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new TarnException("未注册名为 '" + name + "' 的 Bean");
        }
        return doGetBean(definition);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(Class<T> type) {
        if (type == null) {
            throw new TarnException("获取 Bean 的类型不能为空");
        }
        List<String> names = namesForType(type);
        if (names.isEmpty()) {
            throw new TarnException("未注册类型为 " + type.getName() + " 的 Bean");
        }
        if (names.size() > 1) {
            throw new TarnException("类型 " + type.getName() + " 存在多个候选: " + names
                    + "，请使用 getBean(String) 指定名称");
        }
        return (T) getBean(names.get(0));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(String name, Class<T> type) {
        Object bean = getBean(name);
        if (type != null && bean != null && !type.isInstance(bean)) {
            throw new TarnException("Bean '" + name + "' 的实际类型 " + bean.getClass().getName()
                    + " 不是 " + type.getName());
        }
        return (T) bean;
    }

    private Object doGetBean(BeanDefinition definition) {
        if (definition.isPrototype()) {
            return createBeanInternal(definition.getName(), definition);
        }
        return getSingleton(definition.getName(), true, definition);
    }

    /**
     * 创建任意类型的新实例（原型）：完成注入、后置处理与生命周期回调。
     * <p>
     * 用于 Controller 等“每次请求新建”的对象。
     */
    public Object createBean(Class<?> type) {
        if (type == null) {
            throw new TarnException("创建 Bean 的类型不能为空");
        }
        return createBeanInternal(type.getName(), new BeanDefinition(type.getName(), type,
                BeanDefinition.Scope.PROTOTYPE));
    }

    /**
     * 对已创建的对象执行注入与生命周期（兼容旧代码的手动注入入口）。
     * <p>
     * 与 {@link #createBean(Class)} 不同，本方法不会缓存对象。
     */
    public void injectBean(Object bean) {
        if (bean == null) {
            return;
        }
        if (closed) {
            throw new TarnException("ApplicationContext 已关闭");
        }
        String beanName = defaultBeanName(bean.getClass(), bean.getClass().getName());
        populateBean(bean, beanName);
        initializeBean(bean, beanName);
    }

    // ------------------------------------------------------------------
    // 三级缓存与创建
    // ------------------------------------------------------------------

    private Object getSingleton(String name, boolean allowEarlyReference, BeanDefinition definition) {
        Object bean = getSingleton(name, allowEarlyReference);
        if (bean != null) {
            return bean;
        }
        synchronized (singletonLock) {
            bean = getSingleton(name, allowEarlyReference);
            if (bean != null) {
                return bean;
            }
            bean = createBeanInternal(name, definition);
            if (bean != null) {
                singletonObjects.put(name, bean);
                creationOrder.add(name);
            }
            earlySingletonObjects.remove(name);
            singletonFactories.remove(name);
            return bean;
        }
    }

    private Object getSingleton(String name, boolean allowEarlyReference) {
        Object bean = singletonObjects.get(name);
        if (bean != null) {
            return bean;
        }
        bean = earlySingletonObjects.get(name);
        if (bean != null) {
            return bean;
        }
        if (allowEarlyReference) {
            ObjectFactory factory = singletonFactories.get(name);
            if (factory != null) {
                bean = factory.getObject();
                if (bean != null) {
                    earlySingletonObjects.put(name, bean);
                    singletonFactories.remove(name);
                }
            }
        }
        return bean;
    }

    private Object createBeanInternal(String name, BeanDefinition definition) {
        if (closed) {
            throw new TarnException("ApplicationContext 已关闭");
        }
        boolean prototype = definition.isPrototype();
        if (prototype) {
            Set<String> stack = prototypesInCreation.get();
            if (!stack.add(name)) {
                throw new TarnException("检测到原型 Bean 循环依赖: " + name);
            }
        } else if (!singletonsInCreation.add(name)) {
            throw new TarnException("检测到构造期循环依赖（构造方法/静态块中获取 Bean）: " + name);
        }

        try {
            Class<?> type = definition.resolveClass();
            Object bean = instantiate(type);

            if (!prototype) {
                final Object earlyReference = bean;
                // 三级缓存：属性注入前暴露早期引用，供循环依赖使用
                singletonFactories.put(name, new ObjectFactory() {
                    @Override
                    public Object getObject() {
                        return earlyReference;
                    }
                });
            }

            populateBean(bean, name);
            return initializeBean(bean, name);
        } finally {
            if (prototype) {
                prototypesInCreation.get().remove(name);
            } else {
                singletonsInCreation.remove(name);
                singletonFactories.remove(name);
            }
        }
    }

    private Object instantiate(Class<?> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new TarnException("Bean 实例化失败: " + type.getName(), e);
        }
    }

    // ------------------------------------------------------------------
    // 依赖注入
    // ------------------------------------------------------------------

    private void populateBean(Object bean, String beanName) {
        for (BeanProperty property : BeanProperties.ofIncludingInherited(bean.getClass())) {
            EAutowired autowired = property.getAnnotation(EAutowired.class);
            EResource resource = property.getAnnotation(EResource.class);
            if (autowired == null && resource == null) {
                continue;
            }

            String dependencyName;
            if (autowired != null) {
                dependencyName = StringUtil.isNotEmpty(autowired.value())
                        ? autowired.value().trim() : property.getName();
            } else {
                if (!RepositoryInject.checkPackage(bean.getClass().getName())) {
                    LogUtil.warn("[Tarn] " + bean.getClass().getName()
                            + " 不在 Repository 注入白名单内，跳过 " + property.getName());
                    continue;
                }
                dependencyName = StringUtil.isNotEmpty(resource.value())
                        ? resource.value().trim() : property.getName();
            }

            Object dependency = resolveDependency(dependencyName, property.getType());
            if (dependency == null) {
                LogUtil.warn("[Tarn] 无法注入 " + bean.getClass().getName() + "." + property.getName()
                        + "（bean=" + dependencyName + "），已跳过");
                continue;
            }
            if (!property.set(bean, dependency)) {
                LogUtil.warn("[Tarn] " + bean.getClass().getName() + "." + property.getName()
                        + " 不可写，注入失败");
            }
        }
    }

    /**
     * 按名称解析依赖；名称未命中时按类型回退（唯一候选才注入）。
     */
    private Object resolveDependency(String name, Class<?> type) {
        BeanDefinition definition = definitions.get(name);
        if (definition != null) {
            return doGetBean(definition);
        }
        List<String> candidates = namesForType(type);
        if (candidates.size() == 1) {
            return doGetBean(definitions.get(candidates.get(0)));
        }
        if (candidates.size() > 1) {
            LogUtil.warn("[Tarn] 依赖 '" + name + "' 按类型匹配到多个候选 " + candidates
                    + "，请使用 @EAutowired(\"beanName\") 明确指定");
        }
        return null;
    }

    private List<String> namesForType(Class<?> type) {
        List<String> names = new ArrayList<String>();
        if (type == null) {
            return names;
        }
        for (BeanDefinition definition : definitions.values()) {
            try {
                if (type.isAssignableFrom(definition.resolveClass())) {
                    names.add(definition.getName());
                }
            } catch (Throwable ignore) {
                // 无法加载的类不参与匹配
            }
        }
        return names;
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    private Object initializeBean(Object bean, String beanName) {
        invokeAwareMethods(bean, beanName);

        Object current = bean;
        for (BeanPostProcessor processor : postProcessors) {
            Object processed = processor.postProcessBeforeInitialization(current, beanName);
            if (processed != null) {
                current = processed;
            }
        }

        invokeInitMethods(current);

        for (BeanPostProcessor processor : postProcessors) {
            Object processed = processor.postProcessAfterInitialization(current, beanName);
            if (processed != null) {
                current = processed;
            }
        }
        return current;
    }

    private void invokeAwareMethods(Object bean, String beanName) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(this);
        }
    }

    private void invokeInitMethods(Object bean) {
        invokeAnnotatedMethods(bean, POST_CONSTRUCT_ANNOTATIONS);
        if (bean instanceof InitializingBean) {
            try {
                ((InitializingBean) bean).afterPropertiesSet();
            } catch (Exception e) {
                throw new TarnException("初始化失败: " + bean.getClass().getName(), e);
            }
        }
    }

    private void destroyBean(Object bean, String beanName) {
        invokeAnnotatedMethods(bean, PRE_DESTROY_ANNOTATIONS);
        if (bean instanceof DisposableBean) {
            try {
                ((DisposableBean) bean).destroy();
            } catch (Exception e) {
                LogUtil.error("[Tarn] 销毁失败 " + beanName + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * 执行标注了指定注解的无参方法（含父类），父类先于子类。
     */
    private void invokeAnnotatedMethods(Object bean, String[] annotationNames) {
        for (Method method : annotatedMethods(bean.getClass(), annotationNames)) {
            try {
                method.invoke(bean);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getTargetException() != null ? e.getTargetException() : e;
                throw new TarnException("生命周期方法执行失败: " + bean.getClass().getName()
                        + "#" + method.getName(), cause);
            } catch (Exception e) {
                throw new TarnException("生命周期方法执行失败: " + bean.getClass().getName()
                        + "#" + method.getName(), e);
            }
        }
    }

    private List<Method> annotatedMethods(Class<?> type, String[] annotationNames) {
        List<Class<?>> hierarchy = new ArrayList<Class<?>>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        List<Method> methods = new ArrayList<Method>();
        for (Class<?> clazz : hierarchy) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic()
                        || method.getParameterTypes().length != 0) {
                    continue;
                }
                if (hasAnnotation(method, annotationNames)) {
                    try {
                        method.setAccessible(true);
                    } catch (SecurityException ignore) {
                        // 保持原样，invoke 时会给出明确异常
                    }
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static boolean hasAnnotation(Method method, String[] annotationNames) {
        for (Annotation annotation : method.getAnnotations()) {
            String name = annotation.annotationType().getName();
            for (String expected : annotationNames) {
                if (expected.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 容器管理 API
    // ------------------------------------------------------------------

    /**
     * 启动容器：实例化所有 {@link BeanPostProcessor} 定义。
     * <p>
     * 普通单例保持惰性创建（首次 {@code getBean} 时），避免拖慢启动。
     */
    public void refresh() {
        for (BeanDefinition definition : definitions.values()) {
            if (!definition.isSingleton()) {
                continue;
            }
            try {
                if (BeanPostProcessor.class.isAssignableFrom(definition.resolveClass())) {
                    BeanPostProcessor processor = (BeanPostProcessor) doGetBean(definition);
                    if (processor != null && !postProcessors.contains(processor)) {
                        postProcessors.add(processor);
                    }
                }
            } catch (Throwable e) {
                LogUtil.error("[Tarn] BeanPostProcessor 初始化失败 " + definition.getName()
                        + ": " + e.getMessage(), e);
            }
        }
        LogUtil.info("[Tarn] ApplicationContext 就绪，Bean 定义 " + definitions.size()
                + " 个，BeanPostProcessor " + postProcessors.size() + " 个");
    }

    @Override
    public void addBeanPostProcessor(BeanPostProcessor postProcessor) {
        if (postProcessor != null && !postProcessors.contains(postProcessor)) {
            postProcessors.add(postProcessor);
        }
    }

    @Override
    public boolean containsBean(String name) {
        return name != null && definitions.containsKey(name);
    }

    @Override
    public boolean isSingleton(String name) {
        BeanDefinition definition = definitions.get(name);
        return definition != null && definition.isSingleton();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return definitions.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return namesForType(type).toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Map<String, T> result = new java.util.LinkedHashMap<String, T>();
        for (String name : namesForType(type)) {
            Object bean = getBean(name);
            if (bean != null) {
                result.put(name, (T) bean);
            }
        }
        return result;
    }

    @Override
    public int getBeanDefinitionCount() {
        return definitions.size();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<String> names = new ArrayList<String>(creationOrder);
        Collections.reverse(names);
        Set<String> destroyed = new LinkedHashSet<String>();
        for (String name : names) {
            if (!destroyed.add(name)) {
                continue;
            }
            Object bean = singletonObjects.remove(name);
            if (bean != null) {
                destroyBean(bean, name);
            }
        }
        earlySingletonObjects.clear();
        singletonFactories.clear();
        definitions.clear();
        creationOrder.clear();
        postProcessors.clear();
        LogUtil.info("[Tarn] ApplicationContext 已关闭");
    }
}
