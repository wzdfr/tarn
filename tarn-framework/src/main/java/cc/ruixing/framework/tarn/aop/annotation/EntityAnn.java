package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface EntityAnn {
    /**
     * 对应数据库表名
     * */
    String table() default "";
    /**
     * 对应数据库主键
     * */
    public String key() default "id";
    /**
     * 排除字段
     * */
    boolean exclude() default true;
   enum fieldType{key,Date,inter,string};
}
