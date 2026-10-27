package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 服务层数据返回Bean自动注入
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EResource {
    String value() default "";
}
