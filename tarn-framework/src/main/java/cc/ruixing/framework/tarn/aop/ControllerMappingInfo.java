package cc.ruixing.framework.tarn.aop;


import cc.ruixing.framework.tarn.aop.annotation.RequestMethod;

import java.io.Serializable;

/**
 * 控制器请求mapping实例
 * Created by dfr on 14-1-10.
 */
public class ControllerMappingInfo implements Serializable {
    private static final long serialVersionUID = 6392376146163510046L;
    private String requestMapping;
    private String mappingName;
    private String methodName;
    private RequestMethod viewType;
    private String path;

    private boolean isPathVariable = false;

    private String pathVers;


    public ControllerMappingInfo() {
    }

    public ControllerMappingInfo(String mappingName, String methodName, RequestMethod viewType, boolean isPathVariable) {
        this.mappingName = mappingName;
        this.methodName = methodName;
        this.viewType = viewType;
        this.isPathVariable = isPathVariable;
        this.path = path;
    }

    public ControllerMappingInfo(String requestMapping, String mappingName, RequestMethod viewType, String methodName, boolean isPathVariable, String pathVers) {
        this.requestMapping = requestMapping;
        this.mappingName = mappingName;
        this.viewType = viewType;
        this.methodName = methodName;
        this.isPathVariable = isPathVariable;
        this.pathVers = pathVers;

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public RequestMethod getViewType() {
        return viewType;
    }

    public void setViewType(RequestMethod viewType) {
        this.viewType = viewType;
    }

    public String getRequestMapping() {
        return requestMapping;
    }

    public void setRequestMapping(String requestMapping) {
        this.requestMapping = requestMapping;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public boolean isPathVariable() {
        return isPathVariable;
    }

    public void setPathVariable(boolean isPathVariable) {
        this.isPathVariable = isPathVariable;
    }

    public String getPathVers() {
        return pathVers;
    }

    public void setPathVers(String pathVers) {
        this.pathVers = pathVers;
    }
}
