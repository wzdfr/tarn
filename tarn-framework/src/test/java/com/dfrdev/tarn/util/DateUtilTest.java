package com.dfrdev.tarn.util;

import com.dfrdev.tarn.util.date.DateUtil;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * DateUtil 日期工具类单元测试。
 */
public class DateUtilTest {

    // ==================== parseDateTime ====================

    @Test
    public void testParseDateTimeStandard() {
        Date date = DateUtil.parseDateTime("2026-10-27 18:00:00");
        assertNotNull("标准格式应能解析", date);
    }

    @Test
    public void testParseDateTimeNull() {
        Date date = DateUtil.parseDateTime(null);
        assertNull("null 输入应返回 null", date);
    }

    @Test
    public void testParseDateTimeEmpty() {
        Date date = DateUtil.parseDateTime("");
        assertNull("空字符串应返回 null", date);
    }

    // ==================== toString ====================

    @Test
    public void testToStringFormat() {
        Date date = DateUtil.parseDateTime("2026-10-27 18:30:00");
        if (date != null) {
            String formatted = DateUtil.toString(date, "yyyy-MM-dd");
            assertNotNull("格式化结果不应为 null", formatted);
            assertTrue("格式化结果应包含日期", formatted.contains("2026"));
        }
    }

    // ==================== getCurrentDateTime ====================

    @Test
    public void testGetCurrentDateTime() {
        String current = DateUtil.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
        assertNotNull("当前时间不应为 null", current);
        assertTrue("当前时间应包含年份", current.length() >= 19);
    }
}
