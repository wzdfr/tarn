package com.dfrdev.tarn.aop.annotation;

/**
 * Created by dfr on 14-9-28.
 */

import java.lang.annotation.*;

/**
 * 请求参数
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ERequestParam {
    public String value();

}
