package cc.ruixing.framework.tarn.aop.annotation;

import cc.ruixing.framework.tarn.aop.Interceptor;

import java.lang.annotation.*;

/**
 * Created by dfr on 15-5-5.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Before {
    Class<? extends Interceptor>[] value();
}
