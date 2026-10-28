package com.dfrdev.tarn.util;

import com.dfrdev.tarn.util.text.StringUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * StringUtil 字符串工具类单元测试。
 */
public class StringUtilTest {

    // ==================== isEmpty / isNotEmpty ====================

    @Test
    public void testIsEmpty() {
        assertTrue("null 应为空", StringUtil.isEmpty(null));
        assertTrue("空字符串应为空", StringUtil.isEmpty(""));
        assertFalse("非空字符串不应为空", StringUtil.isEmpty("hello"));
        assertFalse("含内容的字符串不应为空", StringUtil.isEmpty(" a "));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse("null 不应为非空", StringUtil.isNotEmpty(null));
        assertFalse("空字符串不应为非空", StringUtil.isNotEmpty(""));
        assertTrue("非空字符串应为非空", StringUtil.isNotEmpty("hello"));
    }

    // ==================== capitalize ====================

    @Test
    public void testCapitalize() {
        assertEquals("首字母应大写", "Hello", StringUtil.capitalize("hello"));
        assertEquals("单字母应大写", "A", StringUtil.capitalize("a"));
        assertEquals("已大写应不变", "Hello", StringUtil.capitalize("Hello"));
        assertEquals("空字符串应返回空", "", StringUtil.capitalize(""));
        assertNull("null 应返回 null", StringUtil.capitalize(null));
    }

    // ==================== isBlank ====================

    @Test
    public void testIsBlank() {
        assertTrue("null 应为 blank", StringUtil.isBlank(null));
        assertTrue("空字符串应为 blank", StringUtil.isBlank(""));
        assertTrue("纯空格应为 blank", StringUtil.isBlank("   "));
        assertFalse("含内容不应为 blank", StringUtil.isBlank("hello"));
    }
}
