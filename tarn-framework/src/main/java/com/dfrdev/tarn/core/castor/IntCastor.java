package com.dfrdev.tarn.core.castor;


import com.dfrdev.tarn.util.classloader.ObjectUtil;

public class IntCastor extends AbstractCastor {
    private static IntCastor singleton = new IntCastor();

    public static IntCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return (Integer.class == type) || (Integer.TYPE == type);
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return Integer.valueOf(0);
        }
        if ((obj instanceof Number))
            return Integer.valueOf(((Number) obj).intValue());
            String str = obj.toString();
            if ((ObjectUtil.empty(str)) || (str.equals("null")) || (str.equals("undefined"))) {
                return Integer.valueOf(0);
            }
            return Integer.valueOf(Integer.parseInt(obj.toString()));
    }
}

