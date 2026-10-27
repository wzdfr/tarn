package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 控制器层请求返回映射
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface EResponseType {
   /**
    * true:表示返回地址信息，如视图页面地址，跳转页面地址
    * false:表示除了响应地址以外的，如字符串，JSON...
    * */
   public boolean value() default true;
}
