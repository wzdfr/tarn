package com.dfrdev.tarn.core.sql;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SqlBuilder SQL 构建器单元测试。
 */
public class SqlBuilderTest {

    // ==================== 基本查询构建 ====================

    @Test
    public void testSimpleSelect() {
        ParameterizedSql ps = SqlBuilder.select("*").from("demo_user").build();
        assertNotNull("构建结果不应为 null", ps);
        assertNotNull("SQL 不应为 null", ps.getSql());
        assertTrue("SQL 应包含 SELECT", ps.getSql().toUpperCase().contains("SELECT"));
        assertTrue("SQL 应包含表名", ps.getSql().toLowerCase().contains("demo_user"));
    }

    @Test
    public void testSelectWithWhere() {
        ParameterizedSql ps = SqlBuilder.select("id, name")
                .from("demo_user")
                .where("status = ?", 1)
                .build();
        assertNotNull("构建结果不应为 null", ps);
        assertTrue("SQL 应包含 WHERE", ps.getSql().toUpperCase().contains("WHERE"));
        assertNotNull("参数列表不应为 null", ps.getParams());
        assertFalse("参数列表不应为空", ps.getParams().isEmpty());
        assertEquals("参数值应为 1", 1, ps.getParams().get(0));
    }

    @Test
    public void testSelectWithMultipleWhere() {
        ParameterizedSql ps = SqlBuilder.select("*")
                .from("demo_book")
                .where("stock > ?", 0)
                .where("price < ?", 100)
                .build();
        assertNotNull("构建结果不应为 null", ps);
        assertEquals("应有 2 个参数", 2, ps.getParams().size());
    }

    @Test
    public void testBuildReturnsParameterizedSql() {
        ParameterizedSql ps = SqlBuilder.select("count(*)")
                .from("demo_user")
                .where("age > ?", 18)
                .build();
        assertNotNull("ParameterizedSql 不应为 null", ps);
        assertNotNull("SQL 字符串不应为 null", ps.getSql());
        assertNotNull("参数列表不应为 null", ps.getParams());
    }
}
