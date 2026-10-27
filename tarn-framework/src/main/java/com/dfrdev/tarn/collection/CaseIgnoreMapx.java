package com.dfrdev.tarn.collection;

import java.util.Map;

public class CaseIgnoreMapx<K, V> extends Mapx<K, V> {
    private static final long serialVersionUID = 1L;

    public CaseIgnoreMapx() {
    }

    public CaseIgnoreMapx(boolean threadSafe) {
        super(threadSafe);
    }

    public CaseIgnoreMapx(int initCapacity, boolean threadSafe) {
        super(initCapacity, threadSafe);
    }

    public CaseIgnoreMapx(Map<? extends K, ? extends V> map) {
        putAll(map);
    }

    public static int caseIgnoreHash(String str) {
        int h = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            h = 31 * h + ((c > '@') && (c < '[') ? c + ' ' : c);
        }
        return hash(h);
    }

    int hash(Object key) {
        if ((key instanceof String)) {
            return caseIgnoreHash((String) key);
        }
        return hash(key.hashCode());
    }

    boolean eq(Object x, int hash, Mapx.Entry<K, V> e) {
        if (x == e.key) {
            return true;
        }
        if ((x instanceof String)) {
            return hash == e.hash;
        }
        return x.equals(e.key);
    }
}

