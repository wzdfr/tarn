package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 销毁回调（对标 {@code javax.annotation.PreDestroy}）。
 * <p>
 * 标注的方法必须无参；容器关闭时按创建顺序逆序执行。
 *
 * @author dfr
 * @since 2.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EPreDestroy {
}
