package cc.ruixing.framework.tarn.aop.annotation;
/**
 * 配置映射服务类实现标记
 * */
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface EService {
    /**
     * 服务名称
     * */
    String value() default "";
}
