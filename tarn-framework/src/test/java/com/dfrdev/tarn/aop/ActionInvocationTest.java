package com.dfrdev.tarn.aop;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * ActionInvocation 类型转换方法单元测试。
 * 测试 convertValue() 方法的各种类型转换场景。
 */
public class ActionInvocationTest {

    // ==================== 基本类型转换 ====================

    @Test
    public void testConvertToInt() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "42", int.class);
        assertEquals("字符串转 int", 42, result);
    }

    @Test
    public void testConvertToLong() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "999999", long.class);
        assertEquals("字符串转 long", 999999L, result);
    }

    @Test
    public void testConvertToFloat() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "3.14", float.class);
        assertEquals("字符串转 float", 3.14f, (Float) result, 0.001f);
    }

    @Test
    public void testConvertToDouble() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "2.718", double.class);
        assertEquals("字符串转 double", 2.718, (Double) result, 0.001);
    }

    @Test
    public void testConvertToBoolean() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "true", boolean.class);
        assertEquals("字符串转 boolean", true, result);
    }

    @Test
    public void testConvertToBigDecimal() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "123.456", BigDecimal.class);
        assertNotNull("BigDecimal 转换不应为 null", result);
        assertEquals("BigDecimal 值", new BigDecimal("123.456"), result);
    }

    @Test
    public void testConvertNullReturnsNull() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, null, int.class);
        assertNull("null 值转换应返回 null", result);
    }

    @Test
    public void testConvertStringToString() throws Exception {
        java.lang.reflect.Method method = ActionInvocation.class.getDeclaredMethod(
                "convertValue", Object.class, Class.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "hello", String.class);
        assertEquals("String 到 String 不变", "hello", result);
    }
}
