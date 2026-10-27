package com.dfrdev.tarn.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 前后端分离注解（Tarn 2.0+，作用等同 Spring MVC 的 @ResponseBody）。
 * <p>
 * 标注在 Controller 方法上，方法返回值将自动序列化为 JSON 输出，
 * 返回类型建议使用 {@link com.dfrdev.tarn.json.JsonResult} 或任意 POJO。
 * <pre>
 *     {@code @Json}
 *     {@code @ERequestMapping("/api/user/save")}
 *     public JsonResult save(User user) {
 *         return JsonResult.ok(service.save(user));
 *     }
 * </pre>
 * 兼容性说明：老项目无此注解的方法行为完全不变（String 返回值仍然走视图渲染）。
 *
 * @author dfr
 * @since 2.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Json {

    /**
     * 是否强制返回，默认 true。false 时不启用 JSON 输出（等同于不标注）
     */
    boolean value() default true;
}
