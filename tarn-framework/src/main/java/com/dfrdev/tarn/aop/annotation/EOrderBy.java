package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 数据映射排序标记
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EOrderBy {
    public String name() default "";

    public boolean showFieldSort() default false;

    public String order() default "DESC";
}
