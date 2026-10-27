package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * ORM字段解除映射
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ORMFilter {
    /**
      * ORM字段解除映射
      * false:否 true:是
      * */
   public boolean value() default false;
}
