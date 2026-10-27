package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 * 数据映射标记主键，主键的常用操作
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Ekey {
   public String key() default "id";
   public String bkey() default "id";
    /**
      * 是否是自动增长字段
      * false:不是 true:是
      * */
   public boolean isAutoIncrement() default false;
}
