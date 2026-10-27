package cc.ruixing.framework.tarn.aop.annotation;


import java.lang.annotation.*;

/**
 * 控制器请求映射器
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ERequestMapping {
   /**
    *请求映射路径
    * 出于复杂度的考虑，该功能目前没有是实现
    * */
   public String value() default "";
   /**
    *
    * */
   public RequestMethod method() default RequestMethod.AUTO;
   /**
    * 是否有路径变量配置
    * */
   public boolean isPathVariable() default  false;
    /**
     * 指定包含的参数
     * */
   public boolean params() default  false;

   /**
    * 当包含路径变量匹配时，需要自定义正则匹配规则情况
    * /a/{a} regex=“([0-9_]+)”
    * */
   public String regex() default "";
}
