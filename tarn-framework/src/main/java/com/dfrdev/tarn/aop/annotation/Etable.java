package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 数据库表与实体类对应映射
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Etable {
    String value() default "dfr_table";
}
