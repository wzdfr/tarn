package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 路径变量注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface EPathVariable {
    public String value();
}
