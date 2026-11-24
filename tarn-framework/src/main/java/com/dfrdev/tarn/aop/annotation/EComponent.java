package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通用组件标记（对标 Spring 的 {@code @Component}）。
 * <p>
 * 标注的类会由 Tarn 容器创建为单例，并自动完成 {@code @EAutowired} / {@code @EResource}
 * 注入及生命周期回调。扫描包需在 {@code context-mapping.xml} 的 {@code <components>} 中声明。
 *
 * @author dfr
 * @since 2.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EComponent {

    /**
     * Bean 名称（为空时按类名首字母小写推导）
     */
    String value() default "";
}
