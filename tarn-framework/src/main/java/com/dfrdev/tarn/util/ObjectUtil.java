package com.dfrdev.tarn.util;


import com.dfrdev.tarn.collection.Filter;
import com.dfrdev.tarn.core.DAO;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

public class ObjectUtil {
    public static Comparator<String> ASCStringComparator = new Comparator() {

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                }
                return -1;
            }

            if (o1 == null) {
                return 1;
            }
            return o1.toString().compareTo(o2.toString());
        }
    };

    public static Comparator<String> DESCStringComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            return -ObjectUtil.ASCStringComparator.compare(o1.toString(), o2.toString());
        }
    };

    public static boolean in(Object[] args) {
        if ((args == null) || (args.length < 2)) {
            return false;
        }
        Object arg1 = args[0];
        for (int i = 1; i < args.length; i++) {
            if (arg1 == null) {
                if (args[i] == null)
                    return true;
            } else {
                if (arg1.equals(args[i])) {
                    return true;
                }

                if ((!arg1.getClass().isArray()) && (args[i] != null) && (args[i].getClass().isArray())) {
                    for (Object obj : (Object[]) args[i]) {
                        if (arg1.equals(obj)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static boolean notIn(Object[] args) {
        return !in(args);
    }

    public static boolean empty(Object obj) {
        if (obj == null) {
            return true;
        }
        if ((obj instanceof String)) {
            return obj.equals("");
        }
        if ((obj instanceof Number)) {
            return ((Number) obj).doubleValue() == 0.0D;
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if ((obj instanceof Collection)) {
            return ((Collection) obj).size() == 0;
        }
        if ((obj instanceof Map)) {
            return ((Map) obj).size() == 0;
        }
        if ((obj instanceof DAO)) {
            return ((DAO) obj).isNull();
        }
        return false;
    }

    public static boolean notEmpty(Object obj) {
        return !empty(obj);
    }

    public static boolean equal(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null) {
            if (obj2 == null) {
                return true;
            }
            return false;
        }

        return obj1.equals(obj2);
    }

    public static boolean notEqual(Object obj1, Object obj2) {
        return !equal(obj1, obj2);
    }

    public static Number minNumber(double[] args) {
        if ((args == null) || (args.length == 0)) {
            return null;
        }
        Number minus = null;
        double[] arrayOfDouble = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            double t = arrayOfDouble[i];
            if (minus == null) {
                minus = Double.valueOf(t);
            } else if (minus.doubleValue() > t) {
                minus = Double.valueOf(t);
            }
        }
        return minus;
    }

    public static Number maxNumber(double[] args) {
        if ((args == null) || (args.length == 0)) {
            return null;
        }
        Number max = null;
        double[] arrayOfDouble = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            double t = arrayOfDouble[i];
            if (max == null) {
                max = Double.valueOf(t);
            } else if (max.doubleValue() < t) {
                max = Double.valueOf(t);
            }
        }
        return max;
    }

    public static <T extends Comparable<T>> T min(T[] args) {
        if ((args == null) || (args.length == 0)) {
            return null;
        }
        Comparable minus = null;
        Comparable[] arrayOfComparable = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            Comparable t = arrayOfComparable[i];
            if (minus == null) {
                minus = t;
            } else if (t != null) {
                if (minus.compareTo(t) > 0)
                    minus = t;
            }
        }
        return (T) minus;
    }

    public static <T extends Comparable<T>> T max(T[] args) {
        if ((args == null) || (args.length == 0)) {
            return null;
        }
        Comparable max = null;
        Comparable[] arrayOfComparable = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            Comparable t = arrayOfComparable[i];
            if (max == null) {
                max = t;
            } else if (t != null) {
                if (max.compareTo(t) < 1)
                    max = t;
            }
        }
        return (T) max;
    }

    public static <T> T ifEmpty(T obj1, T obj2) {
        return empty(obj1) ? obj2 : obj1;
    }

    public static String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static <T> List<T> toList(T[] args) {
        ArrayList list = new ArrayList(args.length);
        Object[] arrayOfObject = args;
        int j = args.length;
        for (int i = 0; i < j; i++) {
            Object t = arrayOfObject[i];
            list.add(t);
        }
        return list;
    }

    public static <T> List<T> toList(Collection<T> c) {
        if ((c instanceof List)) {
            return (List) c;
        }
        List list = new ArrayList(c.size());
        for (Object t : c) {
            list.add(t);
        }
        return list;
    }

    public static List<String> toStringList(Collection<?> list) {
        List r = new ArrayList();
        for (Iterator localIterator = list.iterator(); localIterator.hasNext(); ) {
            Object obj = localIterator.next();
            r.add(obj == null ? null : obj.toString());
        }
        return r;
    }

    public static List<String> toStringList(Object[] arr) {
        List r = new ArrayList();
        Object[] arrayOfObject = arr;
        int j = arr.length;
        for (int i = 0; i < j; i++) {
            Object obj = arrayOfObject[i];
            r.add(obj == null ? null : obj.toString());
        }
        return r;
    }

    public static Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        HashMap r = new HashMap();
        for (Entry e : map.entrySet()) {
            r.put(e.getKey() == null ? null : e.getKey().toString(), e.getValue());
        }
        return r;
    }

    public static Map<String, String> toStringStringMap(Map<?, ?> map) {
        HashMap r = new HashMap();
        for (Entry e : map.entrySet()) {
            r.put(e.getKey() == null ? null : e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString());
        }
        return r;
    }

    public static <K, V> Map<K, V> filter(Map<K, V> map, Filter<Entry<K, V>> filter) {
        if ((map == null) || (map.size() == 0)) {
            return map;
        }
        try {
            Map map2 = (Map) map.getClass().newInstance();
            for (Entry t : map.entrySet()) {
                if (filter.filter(t)) {
                    map2.put(t.getKey(), t.getValue());
                }
            }
            return map2;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int[] toIntArray(Object[] arr) throws Exception {
        int[] r = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = Primitives.getInteger(arr[i]);
        }
        return r;
    }

    public static long[] toLongArray(Object[] arr) throws Exception {
        long[] r = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = Primitives.getLong(arr[i]);
        }
        return r;
    }

    public static float[] toFloatArray(Object[] arr) throws Exception {
        float[] r = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = Primitives.getFloat(arr[i]);
        }
        return r;
    }

    public static double[] toDoubleArray(Object[] arr) throws Exception {
        double[] r = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = Primitives.getDouble(arr[i]);
        }
        return r;
    }

    public static boolean[] toBooleanArray(Object[] arr) throws Exception {
        boolean[] r = new boolean[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = Primitives.getBoolean(arr[i]);
        }
        return r;
    }

    public static String[] toStringArray(Collection<?> list) {
        String[] r = new String[list.size()];
        int i = 0;
        for (Iterator localIterator = list.iterator(); localIterator.hasNext(); ) {
            Object obj = localIterator.next();
            r[(i++)] = (obj == null ? null : obj.toString());
        }
        return r;
    }

    public static String[] toStringArray(Object[] arr) {
        String[] r = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = (arr[i] == null ? null : arr[i].toString());
        }
        return r;
    }

    public static <T> Object[] toObjectArray(T[] arr) {
        Object[] r = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = arr[i];
        }
        return r;
    }

    public static Object[] toObjectArray(Object arrayObject) {
        if (arrayObject == null) {
            return null;
        }
        if (!arrayObject.getClass().isArray()) {
            throw new RuntimeException("Not an array!");
        }
        if ((arrayObject instanceof Object[])) {
            return (Object[]) arrayObject;
        }
        Object[] r = new Object[Array.getLength(arrayObject)];
        for (int i = 0; i < r.length; i++) {
            r[i] = Array.get(arrayObject, i);
        }
        return r;
    }

    public static <T> T[] sort(T[] arr, Comparator<T> c) {
        if ((arr == null) || (arr.length == 0)) {
            return arr;
        }

        Object[] a = (Object[]) Array.newInstance(arr.getClass().getComponentType(), arr.length);
        Object[] arrayOfObject1 = arr;
        int j = arr.length;
        for (int i = 0; i < j; i++) {
            Object t = arrayOfObject1[i];
            a[(i++)] = t;
        }
        Arrays.sort((T[]) a, c);
        return (T[]) a;
    }


    public static String getCurrentStack() {
        return getStack(new Throwable());
    }

    public static String getStack(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : stack) {
            if (ste.getClassName().indexOf("ObjectUtil.getCurrentStack") == -1) {
                sb.append("\tat ");
                sb.append(ste.getClassName());
                sb.append(".");
                sb.append(ste.getMethodName());
                sb.append("(");
                sb.append(ste.getFileName());
                sb.append(":");
                sb.append(ste.getLineNumber());
                sb.append(")\n");
            }
        }
        return sb.toString();
    }

    public static <T> T[] copyOf(T[] arr, int length) {
        Object[] copy = (Object[]) Array.newInstance(arr.getClass().getComponentType(), length);
        System.arraycopy(arr, 0, copy, 0, Math.min(arr.length, length));
        return (T[]) copy;
    }

    public static byte[] copyOf(byte[] arr, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(arr, 0, copy, 0, Math.min(arr.length, length));
        return copy;
    }

    public static boolean[] copyOf(boolean[] arr, int length) {
        boolean[] copy = new boolean[length];
        System.arraycopy(arr, 0, copy, 0, Math.min(arr.length, length));
        return copy;
    }

    public static float[] copyOf(float[] arr, int length) {
        float[] copy = new float[length];
        System.arraycopy(arr, 0, copy, 0, Math.min(arr.length, length));
        return copy;
    }

    public static int[] copyOf(int[] arr, int length) {
        int[] copy = new int[length];
        System.arraycopy(arr, 0, copy, 0, Math.min(arr.length, length));
        return copy;
    }

    public static boolean isBoolean(String v) {
        return ("true".equals(v)) || ("false".equals(v));
    }

    public static boolean isTrue(Object v) {
        if (v == null) {
            return false;
        }
        if ((v instanceof Boolean)) {
            return ((Boolean) v).booleanValue();
        }
        return "true".equals(v.toString());
    }

    /**
     * 如果给定对象为{@code null}返回默认值
     *
     * <pre>
     * ObjectUtil.defaultIfNull(null, null)      = null
     * ObjectUtil.defaultIfNull(null, "")        = ""
     * ObjectUtil.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtil.defaultIfNull("abc", *)        = "abc"
     * ObjectUtil.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param <T>          对象类型
     * @param object       被检查对象，可能为{@code null}
     * @param defaultValue 被检查对象为{@code null}返回的默认值，可以为{@code null}
     * @return 被检查对象为{@code null}返回默认值，否则返回原值
     * @since 3.0.7
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return (null != object) ? object : defaultValue;
    }

}