package cc.ruixing.framework.tarn.core;


import cc.ruixing.framework.tarn.aop.annotation.*;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.TextKit;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AppendReflectSQLUtils {
    private static Logger logger = Logger.getLogger(AppendReflectSQLUtils.class);

    public enum FieldType {
        PRIMITIVE,      // 基本类型：int、boolean等
        WRAPPER,        // 包装类：Integer、Boolean等
        STRING,         // java.lang.String
        LIST,           // 实现List接口的类（包括List本身）
        SET,            // 实现Set接口的类
        MAP,            // 实现Map接口的类
        ARRAY,          // 数组类型（如String[]、int[]）
        ENUM,           // 枚举类型
        JAVA_CLASS,     // 其他Java标准库中的类（如Date、BigDecimal）
        CUSTOM_CLASS    // 用户自定义的类
    }

    public static FieldType determineType(Class<?> clazz) {
        // 1. 检查数组类型（优先处理）
        if (clazz.isArray()) {
            return FieldType.ARRAY;
        }

        // 2. 检查枚举类型
        if (clazz.isEnum()) {
            return FieldType.ENUM;
        }

        // 3. 检查基本类型
        if (clazz.isPrimitive()) {
            return FieldType.PRIMITIVE;
        }

        // 4. 检查String类型
        if (clazz == String.class) {
            return FieldType.STRING;
        }

        // 5. 检查包装类（如Integer、Boolean）
        if (isWrapperType(clazz)) {
            return FieldType.WRAPPER;
        }

        // 6. 检查集合接口
        if (List.class.isAssignableFrom(clazz)) {
            return FieldType.LIST;
        }
        if (Set.class.isAssignableFrom(clazz)) {
            return FieldType.SET;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return FieldType.MAP;
        }

        // 7. 检查是否属于Java标准库
        Package pkg = clazz.getPackage();
        if (pkg != null) {
            String pkgName = pkg.getName();
            if (pkgName.startsWith("java.") || pkgName.startsWith("javax.")) {
                return FieldType.JAVA_CLASS;
            }
        }

        // 8. 剩余情况视为自定义类
        return FieldType.CUSTOM_CLASS;
    }

    // 判断是否是包装类
    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Character.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == Void.class;
    }


    /**
     * 解析封装保存对象方式查询
     * <p>
     * Tarn 2.0+: 内部委托 {@link cc.ruixing.framework.tarn.orm.EntityMapper}（反射元数据缓存），
     * 输出语义与老实现完全一致：含 null 值、主键非自增时入 map、复杂类型 JSON 序列化。
     *
     * @param clazz
     */
    public static AppendReflectSQLEntity parsePackagingSave(Object clazz) {
        cc.ruixing.framework.tarn.orm.EntityMapper.Meta meta =
                cc.ruixing.framework.tarn.orm.EntityMapper.meta(clazz.getClass());
        HashMap map = new HashMap();
        List list = new ArrayList();
        String uk = "ID";
        for (cc.ruixing.framework.tarn.orm.EntityMapper.Column col : meta.columns) {
            Object value = cc.ruixing.framework.tarn.orm.EntityMapper.readValue(col, clazz);
            if (value != null && col.jsonSerialize) {
                value = cc.ruixing.framework.tarn.json.JsonKit.toJson(value);
            }
            if (col.primaryKey) {
                uk = col.columnName;
                if (!col.pkAutoIncrement) {
                    map.put(col.columnName, value);
                    list.add(col.columnName);
                }
            } else {
                map.put(col.columnName, value);
                list.add(col.columnName);
            }
        }
        return new AppendReflectSQLEntity(list, meta.tableName, map, uk);
    }

    /**
     * 解析封装更新对象方式查询
     * <p>
     * Tarn 2.0+: 内部委托 {@link cc.ruixing.framework.tarn.orm.EntityMapper}。
     * 语义与老实现一致：仅收录非 null 值（@EUpdateNull(true) 字段例外），主键非空时设 uk。
     *
     * @param clazz
     */
    public static AppendReflectSQLEntity parsePackagingUpdate(Object clazz) {
        cc.ruixing.framework.tarn.orm.EntityMapper.Meta meta =
                cc.ruixing.framework.tarn.orm.EntityMapper.meta(clazz.getClass());
        HashMap map = new HashMap();
        List list = new ArrayList();
        String uk = "ID";
        for (cc.ruixing.framework.tarn.orm.EntityMapper.Column col : meta.columns) {
            Object value = cc.ruixing.framework.tarn.orm.EntityMapper.readValue(col, clazz);
            if (value != null) {
                if (col.jsonSerialize) {
                    value = cc.ruixing.framework.tarn.json.JsonKit.toJson(value);
                }
                if (col.primaryKey) {
                    uk = col.columnName;
                }
                map.put(col.columnName, value);
                list.add(col.columnName);
            } else if (!col.primaryKey && col.updateIncludeNull) {
                map.put(col.columnName, null);
                list.add(col.columnName);
            }
        }
        return new AppendReflectSQLEntity(list, meta.tableName, map, uk);
    }

    public static AppendReflectSQLEntity parsePackagingUpdate(Object clazz, String updateFieldKey) {
        Class o = clazz.getClass();
        HashMap map = new HashMap();
        Field[] field = o.getDeclaredFields();
        String tableName = AppendDaoSQLUtils.classNameToupp(o.getName());
        boolean hasAnnotation = o.isAnnotationPresent(Etable.class);
        if (hasAnnotation) {
            Etable ann = (Etable) o.getAnnotation(Etable.class);
            tableName = ann.value();
        }
        List list = new ArrayList();
        for (Field f : field) {
            if (!f.isAnnotationPresent(ORMFilter.class)) {
                boolean hasFieldAnn = f.isAnnotationPresent(Ekey.class);
                String fieldName = f.getName();
                Object value = null;
                try {
                    value = o.getMethod("get" + TextKit.firstLetterToUpper(fieldName)).invoke(clazz, null);
                } catch (IllegalAccessException e) {
                    LogUtil.error("安全权限异常:反射调用Bean getXXXX()发送异常,请检查访问权限或其它BUG。属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    LogUtil.error("被调用方法发生异常,属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    LogUtil.error("getXXX() 方法未找到.属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                }


                if (value != null) {
                    Class fieldType = f.getType();
                    FieldType fieldEm = determineType(fieldType);
                    if (fieldEm == FieldType.LIST || fieldEm == FieldType.SET
                            || fieldEm == FieldType.MAP
                            || fieldEm == FieldType.CUSTOM_CLASS
                            || fieldEm == FieldType.ARRAY
                    ) {
                        value = JSON.toJSONString(value);
                    }


                    if (!f.isAnnotationPresent(EFilter.class) || !f.getAnnotation(EFilter.class).value()) {
                        if (!updateFieldKey.equalsIgnoreCase(fieldName)) {
                            map.put(fieldName.toUpperCase(), value);
                            list.add(fieldName.toUpperCase());
                        }
                    }
                } else if (f.isAnnotationPresent(EUpdateNull.class)) {
                    if (f.getAnnotation(EUpdateNull.class).value()) {
                        if (!f.isAnnotationPresent(EFilter.class) || !f.getAnnotation(EFilter.class).value()) {
                            map.put(fieldName.toUpperCase(), value);
                            list.add(fieldName.toUpperCase());
                        }
                    }
                }
            }
        }
        AppendReflectSQLEntity arse = new AppendReflectSQLEntity(list, tableName, map, updateFieldKey);
        return arse;
    }

    /**
     * 解析封装查询对象方式查询
     *
     * @param clazz
     */
    public static AppendReflectSQLEntity parsePackagingSelect(Object clazz) {
        Class o = clazz.getClass();
        HashMap map = new HashMap();
        Field[] field = o.getDeclaredFields();
        String tableName = AppendDaoSQLUtils.classNameToupp(o.getName());
        boolean hasAnnotation = o.isAnnotationPresent(Etable.class);
        if (hasAnnotation) {
            Etable ann = (Etable) o.getAnnotation(Etable.class);
            tableName = ann.value();
        }
        List list = new ArrayList();
        String uk = "ID";
        String orderByStr = "";
        for (Field f : field) {
            if (!f.isAnnotationPresent(ORMFilter.class)) {
                boolean hasFieldAnn = f.isAnnotationPresent(Ekey.class);
                String fieldName = f.getName();
                Object value = null;
                try {
                    value = o.getMethod("get" + TextKit.firstLetterToUpper(fieldName)).invoke(clazz, null);
                } catch (IllegalAccessException e) {
                    LogUtil.error("安全权限异常:反射调用Bean getXXXX()发送异常,请检查访问权限或其它BUG。属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    LogUtil.error("被调用方法发生异常,属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    LogUtil.error("getXXX() 方法未找到.属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                }
                if (value != null) {
                    Class fieldType = f.getType();
                    FieldType fieldEm = determineType(fieldType);
                    if (fieldEm == FieldType.LIST || fieldEm == FieldType.SET
                            || fieldEm == FieldType.MAP
                            || fieldEm == FieldType.CUSTOM_CLASS
                            || fieldEm == FieldType.ARRAY
                    ) {
                        value = JSON.toJSONString(value);
                    }

                    if (hasFieldAnn) {
                        Ekey ekey = f.getAnnotation(Ekey.class);
                        uk = ekey.key().toUpperCase();
                        map.put(uk, value);
                    } else {
                        if (!f.isAnnotationPresent(EFilter.class) || !f.getAnnotation(EFilter.class).value()) {
                            map.put(fieldName.toUpperCase(), value);
                        }
                    }
                }
                boolean hasFieldOrder = f.isAnnotationPresent(EOrderBy.class);
                if (hasFieldOrder) {
                    EOrderBy sort = f.getAnnotation(EOrderBy.class);
                    orderByStr += "," + (sort.name().equals("") ? fieldName : sort.name()) + " " + sort.order();
                }
            }
        }
        if (!orderByStr.equals("")) orderByStr = orderByStr.substring(1);
        AppendReflectSQLEntity arse = new AppendReflectSQLEntity(map, tableName, orderByStr);
        arse.setPk(uk);
        return arse;
    }


    /**
     * 解析封装按主键查询方式
     *
     * @param clazz
     */
    public static AppendReflectSQLEntity parsePackagingSelectById(Object clazz) {

        Class o = clazz.getClass();
        HashMap map = new HashMap();
        Field[] field = o.getDeclaredFields();
        String tableName = AppendDaoSQLUtils.classNameToupp(o.getName());
        boolean hasAnnotation = o.isAnnotationPresent(Etable.class);
        if (hasAnnotation) {
            Etable ann = (Etable) o.getAnnotation(Etable.class);
            tableName = ann.value();
        }
        List list = new ArrayList();
        String uk = "ID";
        for (Field f : field) {
            if (!f.isAnnotationPresent(ORMFilter.class)) {
                boolean hasFieldAnn = f.isAnnotationPresent(Ekey.class);
                String fieldName = f.getName();
                Object value = null;
                try {
                    value = o.getMethod("get" + TextKit.firstLetterToUpper(fieldName)).invoke(clazz, null);
                } catch (IllegalAccessException e) {
                    LogUtil.error("安全权限异常:反射调用Bean getXXXX()发送异常,请检查访问权限或其它BUG。属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    LogUtil.error("被调用方法发生异常,属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    LogUtil.error("getXXX() 方法未找到.属性:" + fieldName + ".类:" + o.getName());
                    e.printStackTrace();
                }
                if (value != null) {
                    Class fieldType = f.getType();
                    FieldType fieldEm = determineType(fieldType);
                    if (fieldEm == FieldType.LIST || fieldEm == FieldType.SET
                            || fieldEm == FieldType.MAP
                            || fieldEm == FieldType.CUSTOM_CLASS
                            || fieldEm == FieldType.ARRAY
                    ) {
                        value = JSON.toJSONString(value);
                    }

                    if (hasFieldAnn) {
                        Ekey ekey = f.getAnnotation(Ekey.class);
                        uk = ekey.key().toUpperCase();
                        map.put(uk, value);
                        break;
                    }
                }
            }
        }
        AppendReflectSQLEntity arse = new AppendReflectSQLEntity(map, tableName);
        arse.setPk(uk);
        return arse;
    }

    /**
     *
     */
    public static AppendReflectSQLEntity parsePackagingSelectByKey(Class o, Object obj) {
        HashMap map = new HashMap();
        Field[] field = o.getDeclaredFields();
        String tableName = AppendDaoSQLUtils.classNameToupp(o.getName());
        boolean hasAnnotation = o.isAnnotationPresent(Etable.class);
        if (hasAnnotation) {
            Etable ann = (Etable) o.getAnnotation(Etable.class);
            tableName = ann.value();
        }
        String uk = "ID";
        for (Field f : field) {
            if (!f.isAnnotationPresent(ORMFilter.class)) {
                boolean hasFieldAnn = f.isAnnotationPresent(Ekey.class);
                if (hasFieldAnn) {
                    Ekey ekey = f.getAnnotation(Ekey.class);
                    uk = ekey.key().toUpperCase();
                    map.put(uk, obj);
                    break;
                }
            }
        }
        AppendReflectSQLEntity arse = new AppendReflectSQLEntity(map, tableName);
        arse.setPk(uk);
        return arse;
    }
}
