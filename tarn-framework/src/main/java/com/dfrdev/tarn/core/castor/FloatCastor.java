package com.dfrdev.tarn.core.castor;

import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.classloader.ObjectUtil;

public class FloatCastor extends AbstractCastor {
    private static FloatCastor singleton = new FloatCastor();

    public static FloatCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return (Float.class == type) || (Float.TYPE == type);
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return Integer.valueOf(0);
        }
        if ((obj instanceof Number)) {
            return Float.valueOf(((Number) obj).floatValue());
        }
        String str = obj.toString();
        if ((ObjectUtil.empty(str)) || (str.equals("null")) || (str.equals("undefined")))
            return Float.valueOf(0.0F);
        try {
            return Float.valueOf(Float.parseFloat(obj.toString()));
        } catch (Exception e) {
            LogUtil.warn("Float 类型转换失败: " + obj + " -> " + e.getMessage());
        }

        return Float.valueOf(0.0F);
    }
}

