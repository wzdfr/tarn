package com.dfrdev.tarn.core.castor;


import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.classloader.ObjectUtil;

public class DoubleCastor extends AbstractCastor {
    private static DoubleCastor singleton = new DoubleCastor();

    public static DoubleCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return (Double.class == type) || (Double.TYPE == type);
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return Integer.valueOf(0);
        }
        if ((obj instanceof Number)) {
            return Double.valueOf(((Number) obj).doubleValue());
        }
        String str = obj.toString();
        if ((ObjectUtil.empty(str)) || (str.equals("null")) || (str.equals("undefined")))
            return Integer.valueOf(0);
        try {
            return Double.valueOf(Double.parseDouble(obj.toString()));
        } catch (Exception e) {
            LogUtil.warn("Double 类型转换失败: " + obj + " -> " + e.getMessage());
        }

        return Double.valueOf(0.0D);
    }
}

