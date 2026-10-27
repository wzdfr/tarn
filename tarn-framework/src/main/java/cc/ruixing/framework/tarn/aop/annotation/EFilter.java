package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 数据映射过滤字段
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EFilter {
    /**
      * 保存修改时是否过滤此字段
      * false:不是 true:是
      * */
   public boolean value() default true;
}
