package cc.ruixing.framework.tarn.aop;


import cc.ruixing.framework.tarn.aop.inject.ComponentScanner;
import cc.ruixing.framework.tarn.aop.service.ControllerAOP;
import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.util.xml.XMLDocument;
import cc.ruixing.framework.tarn.util.xml.XMLElement;

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
