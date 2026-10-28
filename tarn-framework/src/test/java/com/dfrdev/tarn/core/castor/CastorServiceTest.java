package com.dfrdev.tarn.core.castor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CastorService 类型转换服务单元测试。
 */
public class CastorServiceTest {

    // ==================== 基本类型转换 ====================

    @Test
    public void testConvertToInt() throws CastorNotFoundException {
        Object result = CastorService.toType("123", int.class);
        assertNotNull("转换结果不应为 null", result);
        assertEquals("字符串转 int", 123, result);
    }

    @Test
    public void testConvertToLong() throws CastorNotFoundException {
        Object result = CastorService.toType("456789", long.class);
        assertNotNull("转换结果不应为 null", result);
        assertEquals("字符串转 long", 456789L, result);
    }

    @Test
    public void testConvertToDouble() throws CastorNotFoundException {
        Object result = CastorService.toType("3.14", double.class);
        assertNotNull("转换结果不应为 null", result);
        assertEquals("字符串转 double", 3.14, (Double) result, 0.001);
    }

    @Test
    public void testConvertToBoolean() throws CastorNotFoundException {
        Object result = CastorService.toType("true", boolean.class);
        assertNotNull("转换结果不应为 null", result);
        assertEquals("字符串转 boolean", true, result);
    }

    @Test
    public void testConvertToString() throws CastorNotFoundException {
        Object result = CastorService.toType(123, String.class);
        assertNotNull("转换结果不应为 null", result);
        assertEquals("int 转 String", "123", result);
    }

    @Test
    public void testConvertNullValue() throws Exception {
        // null 值对 String.class 应返回 null（toType 中 null + String.class 走 obj==null 分支，返回默认值 0）
        Object result = CastorService.toType((Object) null, String.class);
        // null + String.class: 先走 obj==null 分支，String.class 不是数值类型，返回 obj 即 null
        assertNull("null 值转换应返回 null", result);
    }
}
