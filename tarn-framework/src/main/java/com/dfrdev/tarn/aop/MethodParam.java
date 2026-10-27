package com.dfrdev.tarn.aop;

import java.lang.annotation.Annotation;

/**
 * Created by dfr on 14-9-28.
 */
public class MethodParam {
    private String annotationName;
    private Class annotationClass;
    private Class typeClass;
    private boolean isAnnotation = false;
    private int index;
    private Annotation annotation;

    MethodParam(Annotation annotation) {
        this.annotation = annotation;
    }

    MethodParam(Class typeClass) {
        this.typeClass = typeClass;
    }

    MethodParam(int index, Class annotationClass, Class typeClass) {
        this.index = index;
        this.annotationClass = annotationClass;
        this.typeClass = typeClass;
    }

    MethodParam(int index, String annotationName, Class annotationClass, Class typeClass) {
        this.index = index;
        this.annotationName = annotationName;
        this.annotationClass = annotationClass;
        this.typeClass = typeClass;
    }

    MethodParam(int index, String annotationName, Class annotationClass, Class typeClass, boolean isAnnotation) {
        this.index = index;
        this.annotationName = annotationName;
        this.annotationClass = annotationClass;
        this.typeClass = typeClass;
        this.isAnnotation = isAnnotation;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(boolean isAnnotation) {
        this.isAnnotation = isAnnotation;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public void setAnnotationName(String annotationName) {
        this.annotationName = annotationName;
    }

    public Class getAnnotationClass() {
        return annotationClass;
    }

    public void setAnnotationClass(Class annotationClass) {
        this.annotationClass = annotationClass;
    }

    public Class getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class typeClass) {
        this.typeClass = typeClass;
    }

}
