package com.dfrdev.tarn.aop;

import com.dfrdev.tarn.aop.inject.BeanProperties;
import com.dfrdev.tarn.aop.inject.BeanProperty;
import com.dfrdev.tarn.core.boot.Tarn;

import javax.servlet.jsp.PageContext;


/**
 * 框架内置对象注入（Tarn 2.1 重构）。
 * <p>
 * 为控制器/服务中类型为 {@link Tarn} 与 {@link PageContext} 的字段注入实例。
 * 属性查找与写值统一走 {@link BeanProperties} / {@link BeanProperty}，不再逐个反射 + 打印堆栈。
 *
 * @author dfr
 */
public class TarnInject {

    /**
     * 完成 set API 和 PageContext
     */
    public static void handle(Object o) {
        if (o == null) {
            return;
        }
        for (BeanProperty property : BeanProperties.of(o.getClass())) {
            if (property.getType().equals(Tarn.class)) {
                property.set(o, new Tarn());
            } else if (property.getType().equals(PageContext.class)) {
                property.set(o, Tarn.getPageContext());
            }
        }
    }
}
