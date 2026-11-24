package com.dfrdev.tarn.core.web;

import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 安全响应头测试（S10）。
 */
public class SecurityHeadersTest {

    private static HttpServletResponse recordingResponse(final Map<String, String> headers) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("setHeader".equals(method.getName())) {
                    headers.put((String) args[0], (String) args[1]);
                }
                return null;
            }
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                SecurityHeadersTest.class.getClassLoader(),
                new Class[]{HttpServletResponse.class}, handler);
    }

    @Test
    public void addsBaselineSecurityHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        TarnDispatcherFilter.applySecurityHeaders(null, recordingResponse(headers));

        assertEquals("nosniff", headers.get("X-Content-Type-Options"));
        assertEquals("SAMEORIGIN", headers.get("X-Frame-Options"));
        assertEquals("no-referrer", headers.get("Referrer-Policy"));
        assertNull(headers.get("Strict-Transport-Security"));
    }
}
