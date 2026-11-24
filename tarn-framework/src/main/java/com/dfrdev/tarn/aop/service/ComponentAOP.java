package com.dfrdev.tarn.aop.service;

import com.dfrdev.tarn.aop.annotation.EComponent;
import com.dfrdev.tarn.container.TarnApplicationContext;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.classloader.ClassUtil;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 通用组件扫描（{@code @EComponent}）。
 * <p>
 * 与 {@link ServiceAOP} / {@link RepositoryAOP} 结构一致，仅注册注解不同。
 *
 * @author dfr
 * @since 2.2
 */
public class ComponentAOP implements LoadAOP {

    @Override
    public boolean load(File dirs, XMLElement xmlElement, XMLElement writerXml) {
        ArrayList<String> list = new ArrayList<String>();
        File[] files = dirs.listFiles();
        if (files != null) {
            for (File file : files) {
                list.add(file.getName());
            }
        }
        return process(list, xmlElement, writerXml, false);
    }

    @Override
    public boolean load(List<String> packageAndClassList, XMLElement xmlElement, XMLElement writerXml) {
        return process(packageAndClassList, xmlElement, writerXml, true);
    }

    @Override
    public boolean load(Set<Class<?>> packageAndClassList, XMLElement xmlElement, XMLElement writerXml) {
        for (Class<?> type : packageAndClassList) {
            register(type, writerXml);
        }
        return true;
    }

    private boolean process(List<String> packageAndClassList, XMLElement xmlElement,
                            XMLElement writerXml, boolean isCompletePackageName) {
        for (String classAllName : packageAndClassList) {
            boolean status = isCompletePackageName || classAllName.indexOf(".class") != -1;
            if (!status || classAllName.indexOf("$") != -1) {
                continue;
            }
            String className = isCompletePackageName ? classAllName
                    : xmlElement.getAttributes().getString("package").replace('/', '.')
                    + "." + classAllName.substring(0, classAllName.lastIndexOf("."));
            try {
                register(Class.forName(className), writerXml);
            } catch (Throwable e) {
                LogUtil.error("[Tarn] 组件扫描失败 " + className + ": " + e.getMessage());
            }
        }
        return true;
    }

    private void register(Class<?> type, XMLElement writerXml) {
        if (type == null || type.getName().indexOf("$") != -1) {
            return;
        }
        EComponent component = type.getAnnotation(EComponent.class);
        if (component == null) {
            return;
        }
        TarnApplicationContext.getInstance().registerSingleton(component.value(), type);
        writerXml.addElement("component").addAttribute("class", type.getName());
        LogUtil.info("[Tarn] component Scanning success >> " + ClassUtil.getShortClassName(type.getName()));
    }
}
