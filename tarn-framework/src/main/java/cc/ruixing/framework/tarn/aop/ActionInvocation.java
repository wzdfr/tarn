package cc.ruixing.framework.tarn.aop;


import cc.ruixing.framework.tarn.aop.annotation.Ekey;
import cc.ruixing.framework.tarn.core.AppendReflectSQLUtils;
import cc.ruixing.framework.tarn.core.castor.CastorNotFoundException;
import cc.ruixing.framework.tarn.core.castor.CastorService;
import cc.ruixing.framework.tarn.core.castor.DateCastor;
import cc.ruixing.framework.tarn.util.DateUtil;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.StringUtil;
import cc.ruixing.framework.tarn.util.TextKit;
import cc.ruixing.framework.tarn.json.JsonKit;
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
            String classTypeName = classType.getSimpleName();
            if (value != null) {

//                    method.invoke(obj, CastorService.toType(value, classType));
                if (classTypeName.equals("int") || classTypeName.equals("Integer") || classTypeName.equals("BigInt")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Integer.parseInt(String.valueOf(value)));
                } else if (classTypeName.equals("float") || classTypeName.equals("Float")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Float.parseFloat(String.valueOf(value)));
                } else if (classTypeName.equals("double") || classTypeName.equals("Double")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Double.parseDouble(String.valueOf(value)));
                } else if (classTypeName.equals("boolean") || classTypeName.equals("Boolean")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Boolean.parseBoolean(String.valueOf(value)));
                } else if (classTypeName.equals("BigDecimal") || classTypeName.equals("BigDecimal")) {
                    if (StringUtil.isNotEmpty(value.toString())) method.invoke(obj, BigDecimal.valueOf((Double) value));
                } else if (classTypeName.equals("long") || classTypeName.equals("Long")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Long.parseLong(String.valueOf(value)));
                } else if (classTypeName.equals("String[]")) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        method.invoke(obj, value.toString().split(","));
                    }
                } else if (classType.isAssignableFrom(Date.class)) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        java.sql.Date date = new java.sql.Date(((java.util.Date) DateCastor.getInstance()
                                .cast(value, classType)).getTime());
                        method.invoke(obj, date);
                    }
                } else if (classType.isAssignableFrom(java.util.Date.class)) {
                    java.util.Date date = (java.util.Date) DateCastor.getInstance().cast(value, classType);
                    method.invoke(obj, date);
                } else if (classType.isAssignableFrom(Timestamp.class)) {
                    if (StringUtil.isNotEmpty(value.toString())) {
//                        method.invoke(obj, Timestamp.valueOf(TextKit.parseDate(String.valueOf(value), null, true)));
                        method.invoke(obj, new Timestamp(DateUtil.parseFull(value.toString()).getTime()));
                    }
                } else if (classType.isAssignableFrom(Time.class)) {
                    String time = String.valueOf(value);
                    if (time.split(":").length < 3) {
                        time = _t(time, 3 - time.split(":").length);
                    }
                    method.invoke(obj, Time.valueOf(time));
                } else {
                    method.invoke(obj, value);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
            String classTypeName = classType.getSimpleName();
            AppendReflectSQLUtils.FieldType fieldType = AppendReflectSQLUtils.determineType(classType);
            if (value != null) {
//                    method.invoke(obj, CastorService.toType(value, classType));
                if (classTypeName.equals("int") || classTypeName.equals("Integer") || classTypeName.equals("BigInt")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Integer.parseInt(String.valueOf(value)));
                } else if (classTypeName.equals("float") || classTypeName.equals("Float")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Float.parseFloat(String.valueOf(value)));
                } else if (classTypeName.equals("double") || classTypeName.equals("Double")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Double.parseDouble(String.valueOf(value)));
                } else if (classTypeName.equals("boolean") || classTypeName.equals("Boolean")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Boolean.parseBoolean(String.valueOf(value)));
                } else if (classTypeName.equals("BigDecimal")) {
                    if (StringUtil.isNotEmpty(value.toString())) method.invoke(obj, new BigDecimal(value.toString()));
                } else if (classTypeName.equals("long") || classTypeName.equals("Long")) {
                    if (StringUtil.isNotEmpty(value.toString()))
                        method.invoke(obj, Long.parseLong(String.valueOf(value)));
                } else if (classTypeName.equals("String[]")) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        method.invoke(obj, value.toString().split(","));
                    }
                } else if (classType.isAssignableFrom(Date.class)) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        java.sql.Date date = new java.sql.Date(((java.util.Date) DateCastor.getInstance()
                                .cast(value, classType)).getTime());
                        method.invoke(obj, date);
                    }
                } else if (classType.isAssignableFrom(java.util.Date.class)) {
                    java.util.Date date = (java.util.Date) DateCastor.getInstance().cast(value, classType);
                    method.invoke(obj, date);
                } else if (classType.isAssignableFrom(Timestamp.class)) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        method.invoke(obj, new Timestamp(DateUtil.parseFull(value.toString()).getTime()));

//                        method.invoke(obj, Timestamp.valueOf(TextKit.parseDate(String.valueOf(value), "%YYYY-%MM-%DD %hh:%mm:%ss", true)));
                    }
                } else if (classType.isAssignableFrom(Time.class)) {
                    String time = String.valueOf(value);
                    if (time.split(":").length < 3) {
                        time = _t(time, 3 - time.split(":").length);
                    }
                    method.invoke(obj, Time.valueOf(time));
                } else if (fieldType == AppendReflectSQLUtils.FieldType.LIST ||
                        fieldType == AppendReflectSQLUtils.FieldType.ARRAY) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        Type genericType = field.getGenericType();
                        Class typeClass = null;
                        if (genericType instanceof ParameterizedType) {
                            Type[] actualTypes = ((ParameterizedType) genericType).getActualTypeArguments();
                            // Tarn 2.1 修复：泛型实参本身即是 Class（如 List<String> 的 String.class）；
                            // 老实现误用 actualTypes[0].getClass()（得到 java.lang.Class）当元素类型，
                            // 导致 JSON 反序列化必然失败且被下方 catch 静默吞掉
                            if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                                typeClass = (Class) actualTypes[0];
                            }
                        }
                        // Tarn 2.0+: JSON 解析统一收口至 JsonKit
                        method.invoke(obj, typeClass == null ? JsonKit.parseArray(value.toString(), Object.class) : JsonKit.parseArray(value.toString(), typeClass));
                    }
                } else if (
                        fieldType == AppendReflectSQLUtils.FieldType.MAP
                                || fieldType == AppendReflectSQLUtils.FieldType.SET) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        // Tarn 2.1 修复：Map/Set 的完整泛型结构无法用单个 Class 表达
                        // （老实现传 key 泛型的 getClass() 即 java.lang.Class，反序列化必然失败且被静默吞掉）：
                        //   Map → JsonKit.parse 返回 JSONObject（实现 Map 接口，可直接赋给 Map 字段）
                        //   Set → parseArray 后包装为 LinkedHashSet
                        if (fieldType == AppendReflectSQLUtils.FieldType.SET) {
                            List<Object> list = JsonKit.parseArray(value.toString(), Object.class);
                            method.invoke(obj, new java.util.LinkedHashSet<Object>(list));
                        } else {
                            method.invoke(obj, JsonKit.parse(value.toString()));
                        }
                    }
                } else if (fieldType == AppendReflectSQLUtils.FieldType.CUSTOM_CLASS) {
                    if (StringUtil.isNotEmpty(value.toString())) {
                        method.invoke(obj, JsonKit.fromJson(value.toString(), classType));
                    }
                } else {
                    method.invoke(obj, value);
                }
            }
        } catch (IllegalAccessException e) {
            LogUtil.error("IllegalAccessException injection field:" + fieldName + " Value:" + value + " " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            LogUtil.error("InvocationTargetException injection field:" + fieldName + " Value:" + value + " " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            LogUtil.error("IllegalArgumentException injection field:" + fieldName + " Value:" + value + " " + e.getMessage());
            e.printStackTrace();
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
