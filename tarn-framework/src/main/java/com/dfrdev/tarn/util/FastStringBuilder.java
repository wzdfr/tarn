package com.dfrdev.tarn.util;

import com.alibaba.fastjson.util.IOUtils;

import java.lang.ref.SoftReference;

public class FastStringBuilder {
    private static ThreadLocal<SoftReference<char[]>> local = new ThreadLocal();
    private char[] values;
    private int count;

    public FastStringBuilder() {
        if (local == null) {
            local = new ThreadLocal();
        }
        SoftReference sr = (SoftReference) local.get();
        if (sr != null) {
            this.values = ((char[]) sr.get());
            local.set(null);
        }
        if (this.values == null)
            this.values = new char[5120];
    }

    public int length() {
        return this.count;
    }

    void expandCapacity(int newCount) {
        int newCapacity = this.values.length * 2;
        if (newCapacity < newCount) {
            newCapacity = newCount;
        }
        char[] newValues = new char[newCapacity];
        System.arraycopy(this.values, 0, newValues, 0, this.count);
        this.values = newValues;
    }

    public FastStringBuilder append(char c) {
        int newCount = this.count + 1;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        this.values[this.count] = c;
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(char[] cs) {
        int newCount = this.count + cs.length;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        System.arraycopy(cs, 0, this.values, this.count, cs.length);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(char[] cs, int offset, int length) {
        int newCount = this.count + length;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        System.arraycopy(cs, offset, this.values, this.count, length);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(String s, int offset, int length) {
        int newCount = this.count + length;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        s.getChars(offset, offset + length, this.values, this.count);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(int i) {
        if (i == -2147483648) {
            return append("-2147483648");
        }
        int size = i < 0 ? IOUtils.stringSize(-i) + 1 : IOUtils.stringSize(i);
        int newCount = this.count + size;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        IOUtils.getChars(i, newCount, this.values);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(long i) {
        if (i == -9223372036854775808L) {
            return append("-9223372036854775808");
        }
        int size = i < 0L ? IOUtils.stringSize(-i) + 1 : IOUtils.stringSize(i);
        int newCount = this.count + size;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        IOUtils.getChars(i, newCount, this.values);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(String str) {
        if (str == null) {
            return append("null");
        }
        int length = str.length();
        int newCount = this.count + length;
        if (newCount > this.values.length) {
            expandCapacity(newCount);
        }
        str.getChars(0, length, this.values, this.count);
        this.count = newCount;
        return this;
    }

    public FastStringBuilder append(Object obj) {
        if (obj == null) {
            return append("null");
        }
        if ((obj instanceof Long)) {
            return append(((Long) obj).longValue());
        }
        if ((obj instanceof Integer)) {
            return append(((Integer) obj).intValue());
        }
        if ((obj instanceof Character)) {
            return append(((Character) obj).charValue());
        }
        return append(String.valueOf(obj));
    }

    public void clear() {
        this.count = 0;
    }

    public String toStringAndClose() {
        String v = toString();
        close();
        return v;
    }

    public String toString() {
        return new String(this.values, 0, this.count);
    }

    public void close() {
        local.set(new SoftReference(this.values));
    }
}