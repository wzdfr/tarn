package com.dfrdev.tarn.collection;


import com.dfrdev.tarn.util.text.FastStringBuilder;

import java.lang.reflect.Array;

public abstract class Formatter {
    public static Formatter DefaultFormatter = new Formatter() {
        public String format(Object o) {
            if (o == null) {
                return null;
            }
            if (o.getClass().isArray()) {
                FastStringBuilder sb = new FastStringBuilder();
                sb.append("{");
                for (int i = 0; i < Array.getLength(o); i++) {
                    if (i != 0) {
                        sb.append(",");
                    }
                    sb.append(Array.get(o, i));
                }
                sb.append("}");
                return sb.toStringAndClose();
            }
            return o.toString();
        }
    };

    public abstract String format(Object paramObject);
}

