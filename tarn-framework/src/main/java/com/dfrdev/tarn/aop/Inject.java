package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.inject.ComponentScanner;
import com.dfrdev.tarn.aop.service.ComponentAOP;
import com.dfrdev.tarn.aop.service.ControllerAOP;
import com.dfrdev.tarn.container.TarnApplicationContext;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.util.xml.XMLDocument;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制器扫描入口：读取 context-mapping.xml 的 controller 包，注册到 {@link #controllers}。
 * <p>
 * Tarn 2.1 重构：扫描与清单写出统一交给 {@link ComponentScanner}；
 * 静态初始化链路保持不变（本类 → {@code ServiceInject} → {@code RepositoryInject}）。
 *
 * @author dfr
 */
public class Inject implements Serializable {
    private static final long serialVersionUID = 4055676662365675029L;

    public static List<ControllerInfo> controllers = null;

    static {
        List<XMLElement> packages = Config.getConfigContextMappingController();
        controllers = new ArrayList<ControllerInfo>();
        XMLDocument report = ComponentScanner.scan("Controller（控制器）", packages, new ControllerAOP(), "controllers");
        new ServiceInject();
        List<XMLElement> components = Config.getConfigContextMappingComponents();
        if (components != null && !components.isEmpty()) {
            XMLDocument componentReport = ComponentScanner.scan("Component（通用组件）", components,
                    new ComponentAOP(), "packages");
            ComponentScanner.write(componentReport, "framework-mapping-components.xml");
        }
        TarnApplicationContext.getInstance().refresh();
        ComponentScanner.write(report, "framework-mapping-controllers.xml");
    }

    public static List<ControllerInfo> getControllers() {
        return controllers;
    }

    public static List<ControllerInfo> setController(ControllerInfo propertiesInfo) {
        if (propertiesInfo == null) {
            return controllers;
        }
        controllers.add(propertiesInfo);
        return controllers;
    }
}
