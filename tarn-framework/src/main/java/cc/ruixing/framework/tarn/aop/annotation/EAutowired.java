package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 控制器层服务自动注入
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EAutowired {

    String value() default "";
}
