package com.dfrdev.tarn.aop.annotation;

/**
 * 配置映射数据访问层Bean类实现标记
 * */
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface ERepository {
    /**
     * 名称
     * */
    String value() default "";
}
