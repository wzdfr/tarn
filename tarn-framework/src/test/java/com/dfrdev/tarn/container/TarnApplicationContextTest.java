package com.dfrdev.tarn.container;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EPostConstruct;
import com.dfrdev.tarn.aop.annotation.EPreDestroy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link TarnApplicationContext} 核心能力测试。
 *
 * @author dfr
 * @since 2.2
 */
public class TarnApplicationContextTest {

    // ------------------------------------------------------------------
    // 测试用 Bean
    // ------------------------------------------------------------------

    public static class ServiceA {
        @EAutowired
        private ServiceB serviceB;
        boolean initialized;

        @EPostConstruct
        public void init() {
            initialized = true;
        }

        public ServiceB getServiceB() {
            return serviceB;
        }
    }

    public static class ServiceB {
        @EAutowired
        private ServiceA serviceA;

        public ServiceA getServiceA() {
            return serviceA;
        }
    }

    public static class JsrLifecycleBean {
        boolean postConstructCalled;
        boolean preDestroyCalled;

        @javax.annotation.PostConstruct
        public void init() {
            postConstructCalled = true;
        }

        @javax.annotation.PreDestroy
        public void destroy() {
            preDestroyCalled = true;
        }
    }

    public static class DestroyableBean {
        boolean destroyed;

        @EPreDestroy
        public void bye() {
            destroyed = true;
        }
    }

    public static class ProtoBean {
        @EAutowired
        private ServiceB serviceB;
    }

    public static class BaseService {
        @EAutowired
        protected ServiceB serviceB;
    }

    public static class ChildService extends BaseService {
    }

    public static class RecordingPostProcessor implements BeanPostProcessor {
        final List<String> events = new ArrayList<String>();

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            events.add("before:" + beanName);
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            events.add("after:" + beanName);
            return bean;
        }
    }

    public static class SelfInitializingBean {
        boolean initialized;

        @EPostConstruct
        public void init() {
            initialized = true;
        }
    }

    // ------------------------------------------------------------------
    // 用例
    // ------------------------------------------------------------------

    @Test
    public void circularDependencyShouldResolveWithThreeLevelCache() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceA", ServiceA.class);
        context.registerSingleton("serviceB", ServiceB.class);

        ServiceA a = context.getBean("serviceA", ServiceA.class);
        ServiceB b = context.getBean("serviceB", ServiceB.class);

        assertNotNull(a.getServiceB());
        assertNotNull(b.getServiceA());
        assertSame(b, a.getServiceB());
        assertSame(a, b.getServiceA());
        assertTrue(a.initialized);
    }

    @Test
    public void postConstructAndPreDestroyShouldBeInvoked() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("jsrLifecycleBean", JsrLifecycleBean.class);

        JsrLifecycleBean bean = context.getBean(JsrLifecycleBean.class);
        assertTrue(bean.postConstructCalled);

        context.close();
        assertTrue(bean.preDestroyCalled);
    }

    @Test
    public void beanPostProcessorShouldWrapInitialization() {
        TarnApplicationContext context = new TarnApplicationContext();
        RecordingPostProcessor processor = new RecordingPostProcessor();
        context.addBeanPostProcessor(processor);
        context.registerSingleton("selfInitializingBean", SelfInitializingBean.class);

        context.getBean("selfInitializingBean");

        assertEquals(2, processor.events.size());
        assertEquals("before:selfInitializingBean", processor.events.get(0));
        assertEquals("after:selfInitializingBean", processor.events.get(1));
    }

    @Test
    public void prototypeShouldCreateNewInstanceAndInjectEachTime() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceB", ServiceB.class);
        context.registerPrototype("protoBean", ProtoBean.class);

        ProtoBean first = context.getBean("protoBean", ProtoBean.class);
        ProtoBean second = context.getBean("protoBean", ProtoBean.class);

        assertNotSame(first, second);
        assertNotNull(first.serviceB);
        assertSame(first.serviceB, second.serviceB);
    }

    @Test
    public void inheritedAutowiredFieldShouldBeInjected() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceB", ServiceB.class);
        context.registerSingleton("childService", ChildService.class);

        ChildService child = context.getBean("childService", ChildService.class);
        assertNotNull(child.serviceB);
    }

    @Test
    public void defaultBeanNameShouldBeDecapitalizedSimpleName() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("", ServiceB.class);

        assertTrue(context.containsBean("serviceB"));
        assertNotNull(context.getBean("serviceB"));
    }

    @Test
    public void getBeanByTypeShouldReturnRegisteredBean() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceB", ServiceB.class);

        ServiceB bean = context.getBean(ServiceB.class);
        assertSame(bean, context.getBean("serviceB"));
    }

    @Test
    public void injectBeanShouldProcessExternalInstance() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceB", ServiceB.class);

        ServiceA external = new ServiceA();
        context.injectBean(external);

        assertNotNull(external.getServiceB());
        assertTrue(external.initialized);
    }

    @Test
    public void createBeanShouldNotCachePrototype() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("serviceB", ServiceB.class);

        Object first = context.createBean(ProtoBean.class);
        Object second = context.createBean(ProtoBean.class);

        assertNotSame(first, second);
        assertFalse(context.containsBean(ProtoBean.class.getName()));
    }

    @Test
    public void closeShouldInvokeDisposableAnnotation() {
        TarnApplicationContext context = new TarnApplicationContext();
        context.registerSingleton("destroyableBean", DestroyableBean.class);

        DestroyableBean bean = context.getBean(DestroyableBean.class);
        assertFalse(bean.destroyed);
        context.close();
        assertTrue(bean.destroyed);
    }
}
