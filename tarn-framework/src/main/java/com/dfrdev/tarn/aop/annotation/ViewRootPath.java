package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ViewRootPath {
    /**
     * view root path
     */
    String value() default "/";
}
