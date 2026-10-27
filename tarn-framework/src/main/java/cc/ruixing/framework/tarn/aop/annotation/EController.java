package cc.ruixing.framework.tarn.aop.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EController {
    /**
     * controller name
     * */
    String value() default "action";
    /**
     * false : 类所有的方法,包括自身的所有public方法，和从基类继承的、从接口实现的所有public方法
     * true  :  类自身声明的所有方法，包含public、protected和private方法。
     * */
    boolean selfMethods() default true;
    /**
     * 当包含路径变量匹配时，需要自定义正则匹配规则情况
     * 当等于${mapping} 时候匹配
     * /a/{a} regex=“([0-9_]+)”
     * */
    String regex() default "";
}
