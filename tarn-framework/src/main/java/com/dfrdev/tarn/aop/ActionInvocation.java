package com.dfrdev.tarn.aop;


import com.dfrdev.tarn.aop.annotation.Ekey;
import com.dfrdev.tarn.core.AppendReflectSQLUtils;
import com.dfrdev.tarn.core.castor.CastorNotFoundException;
import com.dfrdev.tarn.core.castor.CastorService;
import com.dfrdev.tarn.core.castor.DateCastor;
import com.dfrdev.tarn.util.DateUtil;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;
import com.dfrdev.tarn.util.TextKit;
import com.dfrdev.tarn.json.JsonKit;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ActionInvocation {

    public static String firstLetterToUpper(String string) {
        char[] buffer = string.toCharArray();
        buffer[0] = Character.toUpperCase(string.charAt(0));
        return new String(buffer);
    }

    public static String firstLetterToUppers(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 调用setXXX传值注入
     *
     * @param obj       执行对象
     * @param fieldName 属性
     * @param value     值
     */
    public static void invokeSet(Object obj, String fieldName, Object value) {
        Class clazz = obj.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Class classType = field.getType();
            fieldName = TextKit.firstLetterToUpper(fieldName);
            String invkeMethodName = "set" + fieldName;
            Method method = clazz.getMethod(invkeMethodName, classType);
            if (value != null) {
                Object convertedValue = convertValue(value, classType);
                method.invoke(obj, convertedValue);
            }
        } catch (IllegalAccessException e) {
            LogUtil.error("字段注入访问异常: " + fieldName + " -> " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LogUtil.error("字段注入调用异常: " + fieldName + " -> " + e.getMessage(), e);
        } catch (NoSuchFieldException e) {
            LogUtil.error("NoSuchFieldException Properties injection:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            LogUtil.error("NoSuchMethodException Method injection:" + e.getMessage());
        } catch (CastorNotFoundException e) {
            LogUtil.error("CastorNotFoundException Class injection:" + e.getMessage());
        }
    }


    public static String _t(String time, Integer length) {
        for (int i = 0; i < length; i++) {
            time += ":00";
        }
        return time;
    }

    /**
     * 将值转换为目标类型（提取公共逻辑，减少重复代码）
     *
     * @param value     原始值
     * @param classType 目标类型
     * @return 转换后的值；若无法转换返回原值
     */
    private static Object convertValue(Object value, Class<?> classType) throws CastorNotFoundException {
        if (value == null) {
            return null;
        }
        String classTypeName = classType.getSimpleName();
        String strValue = String.valueOf(value);
        
        if (StringUtil.isEmpty(strValue)) {
            return value;
        }
        
        // 基本类型转换
        if (classTypeName.equals("int") || classTypeName.equals("Integer") || classTypeName.equals("BigInt")) {
            return Integer.parseInt(strValue);
        } else if (classTypeName.equals("float") || classTypeName.equals("Float")) {
            return Float.parseFloat(strValue);
        } else if (classTypeName.equals("double") || classTypeName.equals("Double")) {
            return Double.parseDouble(strValue);
        } else if (classTypeName.equals("boolean") || classTypeName.equals("Boolean")) {
            return Boolean.parseBoolean(strValue);
        } else if (classTypeName.equals("BigDecimal")) {
            return new BigDecimal(strValue);
        } else if (classTypeName.equals("long") || classTypeName.equals("Long")) {
            return Long.parseLong(strValue);
        } else if (classTypeName.equals("String[]")) {
            return strValue.split(",");
        } 
        // 日期类型转换
        else if (classType.isAssignableFrom(Date.class)) {
            java.sql.Date date = new java.sql.Date(((java.util.Date) DateCastor.getInstance()
                    .cast(value, classType)).getTime());
            return date;
        } else if (classType.isAssignableFrom(java.util.Date.class)) {
            return DateCastor.getInstance().cast(value, classType);
        } else if (classType.isAssignableFrom(Timestamp.class)) {
            return new Timestamp(DateUtil.parseFull(strValue).getTime());
        } else if (classType.isAssignableFrom(Time.class)) {
            String time = strValue;
            if (time.split(":").length < 3) {
                time = _t(time, 3 - time.split(":").length);
            }
            return Time.valueOf(time);
        }
        // 默认返回原值
        return value;
    }


    /**
     * 调用setXXX传值注入用于ORM,无限级联设置
     *
     * @param obj       执行对象
     * @param fieldName 属性
     * @param value     值
     */
    public static void invokeSetORM(Object obj, String fieldName, Object value, HashMap aopRecursiveField) {
        if (fieldName.indexOf(".") != -1) {
            AttrInject.invokeRecursive(obj, fieldName, value, aopRecursiveField);
            return;
        }
        Class clazz = obj.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Class classType = field.getType();
            fieldName = TextKit.firstLetterToUpper(fieldName);
            String invkeMethodName = "set" + fieldName;
            Method method = clazz.getMethod(invkeMethodName, classType);
            AppendReflectSQLUtils.FieldType fieldType = AppendReflectSQLUtils.determineType(classType);
            if (value != null && StringUtil.isNotEmpty(String.valueOf(value))) {
                // 处理集合/数组类型（ORM 特有）
                if (fieldType == AppendReflectSQLUtils.FieldType.LIST ||
                        fieldType == AppendReflectSQLUtils.FieldType.ARRAY) {
                    Type genericType = field.getGenericType();
                    Class typeClass = null;
                    if (genericType instanceof ParameterizedType) {
                        Type[] actualTypes = ((ParameterizedType) genericType).getActualTypeArguments();
                        if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                            typeClass = (Class) actualTypes[0];
                        }
                    }
                    method.invoke(obj, typeClass == null ? JsonKit.parseArray(String.valueOf(value), Object.class) : JsonKit.parseArray(String.valueOf(value), typeClass));
                } else if (fieldType == AppendReflectSQLUtils.FieldType.MAP ||
                        fieldType == AppendReflectSQLUtils.FieldType.SET) {
                    if (fieldType == AppendReflectSQLUtils.FieldType.SET) {
                        List<Object> list = JsonKit.parseArray(String.valueOf(value), Object.class);
                        method.invoke(obj, new java.util.LinkedHashSet<Object>(list));
                    } else {
                        method.invoke(obj, JsonKit.parse(String.valueOf(value)));
                    }
                } else if (fieldType == AppendReflectSQLUtils.FieldType.CUSTOM_CLASS) {
                    method.invoke(obj, JsonKit.fromJson(String.valueOf(value), classType));
                } else {
                    // 使用公共类型转换方法
                    Object convertedValue = convertValue(value, classType);
                    method.invoke(obj, convertedValue);
                }
            }
        } catch (IllegalAccessException e) {
            LogUtil.error("IllegalAccessException injection field:" + fieldName + " Value:" + value + " " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LogUtil.error("InvocationTargetException injection field:" + fieldName + " Value:" + value + " " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LogUtil.error("IllegalArgumentException injection field:" + fieldName + " Value:" + value + " " + e.getMessage(), e);
        } catch (NoSuchFieldException e) {
            LogUtil.debug("NoSuchFieldException Properties injection  field:" + fieldName + " " + e.getMessage());
        } catch (NoSuchMethodException e) {
            LogUtil.error("NoSuchMethodException Method injection  field:" + fieldName + " " + e.getMessage());
        } catch (CastorNotFoundException e) {
            LogUtil.error("CastorNotFoundException Class injection  field:" + fieldName + " " + e.getMessage());
        }
    }


    /**
     * java反射bean的set方法
     *
     * @param objectClass class类。
     * @param fieldName   字段名
     * @return Method
     */
    public static Method getSetMethod(Class objectClass, String fieldName) {
        Field field = null;
        Method method = null;
        try {
            field = objectClass.getDeclaredField(fieldName);
            Class[] parameterTypes = new Class[1];
            parameterTypes[0] = field.getType();
            String tf = TextKit.firstLetterToUpper(fieldName);
            if (field.isAnnotationPresent(Ekey.class)) {
                tf = TextKit.firstLetterToUpper(field.getAnnotation(Ekey.class).key());
            }
            String str = "set" + tf;
            method = objectClass.getMethod(str, parameterTypes);
            return method;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }
}
