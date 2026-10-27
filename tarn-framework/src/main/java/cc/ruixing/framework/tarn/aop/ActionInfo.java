package cc.ruixing.framework.tarn.aop;

import cc.ruixing.framework.tarn.core.Controller;

/**
 * Created by dfr on 15-5-5.
 */
public class ActionInfo {

    private Controller controller;
    private ControllerInfo controllerInfo;
    private ControllerMappingInfo controllerMappingInfo;
    private ControllerMethodInfo controllerMethodInfo;

    protected ActionInfo(Controller controller, ControllerInfo propertiesInfo, ControllerMappingInfo controllerMapping, ControllerMethodInfo controllerMethodInfo) {
        this.controller = controller;
        this.controllerInfo = propertiesInfo;
        this.controllerMappingInfo = controllerMapping;
        this.controllerMethodInfo = controllerMethodInfo;
    }

    ActionInfo(Controller controller, ControllerInfo controllerInfo, ControllerMappingInfo controllerMappingInfo) {
        this.controller = controller;
        this.controllerInfo = controllerInfo;
        this.controllerMappingInfo = controllerMappingInfo;
    }


    public ControllerMethodInfo getControllerMethodInfo() {
        return controllerMethodInfo;
    }

    public String getControlerKey() {
        return controllerInfo.getKey();
    }

    public String getMethodKey() {
        return controllerMappingInfo.getMappingName();
    }

    public Controller getController() {
        return controller;
    }

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public ControllerMappingInfo getControllerMappingInfo() {
        return controllerMappingInfo;
    }
}
