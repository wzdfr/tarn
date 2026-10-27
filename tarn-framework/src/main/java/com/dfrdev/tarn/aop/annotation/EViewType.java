package com.dfrdev.tarn.aop.annotation;


import com.dfrdev.tarn.render.ViewType;

import java.lang.annotation.*;

/**
 * 返回视图类型，通常用于控制层
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EViewType {
   public ViewType value() default ViewType.FREE_MARKER;
}
