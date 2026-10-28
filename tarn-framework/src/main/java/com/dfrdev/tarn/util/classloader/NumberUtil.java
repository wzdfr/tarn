package com.dfrdev.tarn.util.classloader;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

import com.dfrdev.tarn.util.text.StringUtil;
public class NumberUtil {
    private static Pattern numberPatter = Pattern.compile("^[\\d\\.E\\,\\+\\-]*$");

    private static Random rand = new Random();

    public static boolean isNumber(String str) {
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        return numberPatter.matcher(str).find();
    }

    public static boolean isInt(String str) {
        if (StringUtil.isEmpty(str))
            return false;
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isInteger(String str) {
        return isInt(str);
    }

    public static boolean isLong(String str) {
        if (StringUtil.isEmpty(str))
            return false;
        try {
            Long.parseLong(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isFloat(String str) {
        if (StringUtil.isEmpty(str))
            return false;
        try {
            Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String str) {
        if (StringUtil.isEmpty(str))
            return false;
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static double round(double v, int scale) {
        BigDecimal b = new BigDecimal(Double.toString(v));
        return b.setScale(scale, 4).doubleValue();
    }

    public static int getRandomInt(int max) {
        return rand.nextInt(max);
    }

    public static int toInt(byte[] bs) {
        return toInt(bs, 0);
    }

    public static int toInt(byte[] bs, int start) {
        int i = 0;
        i += ((bs[start] & 0xFF) << 24);
        i += ((bs[(start + 1)] & 0xFF) << 16);
        i += ((bs[(start + 2)] & 0xFF) << 8);
        i += (bs[(start + 3)] & 0xFF);
        return i;
    }

    public static byte[] toBytes(int i) {
        byte[] bs = new byte[4];
        bs[0] = (byte) (i >> 24);
        bs[1] = (byte) (i >> 16);
        bs[2] = (byte) (i >> 8);
        bs[3] = (byte) (i & 0xFF);
        return bs;
    }

    public static void toBytes(int i, byte[] bs, int start) {
        bs[start] = (byte) (i >> 24);
        bs[(start + 1)] = (byte) (i >> 16);
        bs[(start + 2)] = (byte) (i >> 8);
        bs[(start + 3)] = (byte) (i & 0xFF);
    }

    public static short toShort(byte[] bs) {
        return toShort(bs, 0);
    }

    public static short toShort(byte[] bs, int start) {
        short i = 0;
        i = (short) (i + ((bs[(start + 0)] & 0xFF) << 8));
        i = (short) (i + (bs[(start + 1)] & 0xFF));
        return i;
    }

    public static byte[] toBytes(short i) {
        byte[] bs = new byte[2];
        bs[0] = (byte) (i >> 8);
        bs[1] = (byte) (i & 0xFF);
        return bs;
    }

    public static void toBytes(short i, byte[] bs, int start) {
        bs[(start + 0)] = (byte) (i >> 8);
        bs[(start + 1)] = (byte) (i & 0xFF);
    }

    public static byte[] toBytes(long i) {
        byte[] bs = new byte[8];
        bs[0] = (byte) (int) (i >> 56);
        bs[1] = (byte) (int) (i >> 48);
        bs[2] = (byte) (int) (i >> 40);
        bs[3] = (byte) (int) (i >> 32);
        bs[4] = (byte) (int) (i >> 24);
        bs[5] = (byte) (int) (i >> 16);
        bs[6] = (byte) (int) (i >> 8);
        bs[7] = (byte) (int) (i & 0xFF);
        return bs;
    }

    public static void toBytes(long l, byte[] bs, int start) {
        byte[] arr = toBytes(l);
        for (int i = 0; i < 8; i++)
            bs[(start + i)] = arr[i];
    }

    public static long toLong(byte[] bs) {
        return toLong(bs, 0);
    }

    public static long toLong(byte[] bs, int index) {
        return (bs[index] & 0xFF) << 56 | (bs[(index + 1)] & 0xFF) << 48 | (bs[(index + 2)] & 0xFF) << 40 |
                (bs[(index + 3)] & 0xFF) << 32 | (bs[(index + 4)] & 0xFF) << 24 | (bs[(index + 5)] & 0xFF) << 16 |
                (bs[(index + 6)] & 0xFF) << 8 | (bs[(index + 7)] & 0xFF) << 0;
    }

    public static String format(Number n, String format) {
        DecimalFormat df = new DecimalFormat(format);
        return df.format(n);
    }

    public static ArrayList<Integer> getBinaryList(int num) {
        ArrayList list = new ArrayList();
        String binStr = Integer.toBinaryString(num);
        for (int i = 0; i < binStr.length(); i++) {
            if (binStr.charAt(i) == '1') {
                int intpow = (int) Math.pow(2.0D, binStr.length() - i - 1);
                list.add(Integer.valueOf(intpow));
            }
        }
        return list;
    }

    public static String getBinaryString(int num) {
        ArrayList list = getBinaryList(num);
        return StringUtil.join(list.toArray());
    }
}