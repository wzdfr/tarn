package com.dfrdev.tarn.aop.annotation;

import com.dfrdev.tarn.aop.Interceptor;

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
