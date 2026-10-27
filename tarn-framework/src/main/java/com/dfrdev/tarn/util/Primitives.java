package com.dfrdev.tarn.util;


import com.dfrdev.tarn.core.castor.*;

public class Primitives {
    public static final Class<?>[] primitiveClass = {Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE,
            Short.TYPE, Integer.class, Long.class, Boolean.class, Character.class, Byte.class, Short.class};
    public static final Class<?>[] primitiveArrClass = {Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE,
            Short.TYPE, Integer.class, Long.class, Boolean.class, Character.class, Byte.class, Short.class};

//    public static final Class<?>[] primitiveArrClass =
//    [Byte.TYPE,Short.TYPE,java.lang.Integer.class,java.lang.Long.class,java.lang.Boolean.class,java.lang.Character.class,java.lang.Byte.class,java.lang.Short.class]
//;


    public static boolean isPrimitives(Object obj) {
        if (!obj.getClass().isArray()) {
            return ObjectUtil.in(new Object[]{obj.getClass(), primitiveClass});
        }
        return ObjectUtil.in(new Object[]{obj.getClass(), primitiveArrClass});
    }

    public static double getDouble(Object obj) throws Exception {
        return ((Number) DoubleCastor.getInstance().cast(obj, Double.class)).doubleValue();
    }

    public static float getFloat(Object obj) throws Exception {
        return ((Number) FloatCastor.getInstance().cast(obj, Float.class)).floatValue();
    }

    public static long getLong(Object obj) throws Exception {
        return ((Number) LongCastor.getInstance().cast(obj, Long.class)).longValue();
    }

    public static int getInteger(Object obj) throws Exception {
        return ((Number) IntCastor.getInstance().cast(obj, Integer.class)).intValue();
    }

    public static Boolean getBoolean(boolean flag) {
        return flag ? Boolean.TRUE : Boolean.FALSE;
    }

    public static boolean getBoolean(Object obj) throws Exception {
        return ((Boolean) BooleanCastor.getInstance().cast(obj, Boolean.class)).booleanValue();
    }
}