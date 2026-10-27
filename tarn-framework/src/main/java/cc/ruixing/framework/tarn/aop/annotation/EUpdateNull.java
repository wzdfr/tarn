package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

/**
 *在实体类映射中配置执行更新语句时遇到字段值为null是否是将字段值设为null，或是不更新这个字段
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EUpdateNull {
    /**
     * 在执行更新语句时遇到字段值为null是否是将字段值设为null，或是不更新这个字段
     * true:  更新这个字段值设置null
     * false： 不更新这个字段
     */
    public boolean value() default true;
}
