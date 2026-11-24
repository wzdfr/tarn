package com.dfrdev.tarn.aop;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 请求参数绑定白名单/框架属性保护测试（S5）。
 */
public class AttrInjectBindableTest {

    public static class Target {
        private String title;
        private String request;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }
    }

    @After
    public void tearDown() {
        AttrInject.clearBindableFields(Target.class);
    }

    @Test
    public void allowsNormalField() {
        assertTrue(AttrInject.isBindable(new Target(), "title"));
    }

    @Test
    public void deniesFrameworkOwnedFieldName() {
        assertFalse(AttrInject.isBindable(new Target(), "request"));
        assertFalse(AttrInject.isBindable(new Target(), "viewRootPath"));
    }

    @Test
    public void deniesUnknownField() {
        assertFalse(AttrInject.isBindable(new Target(), "notExists"));
    }

    @Test
    public void whitelistLimitsBindableFields() {
        AttrInject.setBindableFields(Target.class, "title");
        assertTrue(AttrInject.isBindable(new Target(), "title"));
        assertFalse(AttrInject.isBindable(new Target(), "other"));
    }
}
