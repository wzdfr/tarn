package com.dfrdev.tarn.aop;

import java.util.List;

public class ControllerInfo {
    private String key;
    private String value;

    private List<ControllerMethodInfo> methodInfoList;

    private String viewRootPath;

    private String regex;

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public ControllerInfo(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public List<ControllerMethodInfo> getMethodInfoList() {
        return methodInfoList;
    }

    public void setMethodInfoList(List<ControllerMethodInfo> methodInfoList) {
        this.methodInfoList = methodInfoList;
    }

    public String getViewRootPath() {
        return viewRootPath;
    }

    public void setViewRootPath(String viewRootPath) {
        this.viewRootPath = viewRootPath;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
