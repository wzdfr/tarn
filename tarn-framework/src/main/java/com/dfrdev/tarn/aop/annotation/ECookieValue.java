package com.dfrdev.tarn.aop.annotation;

/**
 * Created by dfr on 20-10-26.
 */

import java.lang.annotation.*;

/**
 * 请求参数
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ECookieValue {
    public String value();

}
