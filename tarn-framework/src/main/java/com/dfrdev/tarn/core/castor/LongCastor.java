package com.dfrdev.tarn.core.castor;


import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.classloader.ObjectUtil;

public class LongCastor extends AbstractCastor {
    private static LongCastor singleton = new LongCastor();

    public static LongCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return (Long.class == type) || (Long.TYPE == type);
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return Integer.valueOf(0);
        }
        if ((obj instanceof Number))
            return Long.valueOf(((Number) obj).longValue());
        try {
            String str = obj.toString();
            if ((ObjectUtil.empty(str)) || (str.equals("null")) || (str.equals("undefined"))) {
                return Integer.valueOf(0);
            }
            return Long.valueOf(Long.parseLong(obj.toString()));
        } catch (Exception e) {
            LogUtil.warn("Long 类型转换失败: " + obj + " -> " + e.getMessage());
        }

        return Long.valueOf(0L);
    }
}

