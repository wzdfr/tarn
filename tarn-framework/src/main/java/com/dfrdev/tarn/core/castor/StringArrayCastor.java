package com.dfrdev.tarn.core.castor;


import com.dfrdev.tarn.util.text.StringUtil;

public class StringArrayCastor extends AbstractCastor {
    private static StringArrayCastor singleton = new StringArrayCastor();

    public static StringArrayCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return String.class == type;
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return null;
        }
        if ((obj instanceof String[]))
            return obj;
        if (obj.getClass().isArray()) {
            Object[] os = (Object[]) obj;
            String[] arr = new String[os.length];
            for (int i = 0; i < os.length; i++) {
                arr[i] = (os[i] == null ? null : os[i].toString());
            }
            return arr;
        }
        if (((obj instanceof String)) && (obj.toString().indexOf(",") > 0)) {
            return StringUtil.splitEx(obj.toString(), ",");
        }
        return new String[]{obj.toString()};
    }
}

