package com.dfrdev.tarn.aop;

import com.dfrdev.tarn.aop.annotation.ERequestMapping;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Created by dfr on 14-9-28.
 */
public class ControllerMethodInfo implements Serializable {

    private String name;
    private boolean isParameters = false;
    private Class returnType;
    private Map<String, MethodParam> parameters;

    private Class<?>[] paramTypes;
    private boolean isComment;
    private Annotation[] annotations;

    private Object[] methodParam;

    private ERequestMapping requestMapping;




    public ControllerMethodInfo() {
    }

    public ControllerMethodInfo(String name, boolean isParameters, Class returnType, Map<String, MethodParam> parameters) {
        this.name = name;
        this.isParameters = isParameters;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isParameters() {
        return isParameters;
    }

    public void setParameters(boolean isParameters) {
        this.isParameters = isParameters;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    public Map<String, MethodParam> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, MethodParam> parameters) {
        this.parameters = parameters;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }


    public MethodParam bulidMethodPram(Class typeClass) {
        return new MethodParam(typeClass);
    }
    public MethodParam bulidMethodPram(Annotation annotation) {
        return new MethodParam(annotation);
    }

    public Object[] getMethodParam() {
        return methodParam;
    }

    public void setMethodParam(Object[] methodParam) {
        this.methodParam = methodParam;
    }

    public boolean isComment() {
        return isComment;
    }

    public void setComment(boolean isComment) {
        this.isComment = isComment;
    }

    public ERequestMapping getRequestMapping() {
        return requestMapping;
    }

    public void setRequestMapping(ERequestMapping requestMapping) {
        this.requestMapping = requestMapping;
    }
}
