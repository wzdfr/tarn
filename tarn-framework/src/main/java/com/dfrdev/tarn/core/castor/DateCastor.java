package com.dfrdev.tarn.core.castor;

import com.dfrdev.tarn.util.DateUtil;

import java.util.Date;

public class DateCastor extends AbstractCastor {
    private static DateCastor singleton = new DateCastor();

    public static DateCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return Date.class == type;
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return null;
        }
        if ((obj instanceof Date) || obj instanceof  java.sql.Date)
            return obj;
        if ((obj instanceof Long)) {
            return new Date(((Long) obj).longValue());
        }
        if (obj instanceof String) {
            return DateUtil.parseFull(obj.toString());
        }
        return null;
    }
}

