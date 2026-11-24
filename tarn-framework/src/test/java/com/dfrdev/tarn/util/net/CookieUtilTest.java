package com.dfrdev.tarn.util.net;

import org.junit.Test;

import javax.servlet.http.Cookie;

import static org.junit.Assert.assertTrue;

/**
 * Cookie 安全属性测试（S8）。
 */
public class CookieUtilTest {

    @Test
    public void headerContainsSecurityAttributes() {
        Cookie cookie = new Cookie("token", "abc");
        cookie.setPath("/app");
        cookie.setMaxAge(60);
        cookie.setHttpOnly(true);

        String header = CookieUtil.buildHeader(cookie);

        assertTrue(header.startsWith("token=abc"));
        assertTrue(header.contains("Path=/app"));
        assertTrue(header.contains("Max-Age=60"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
    }

    @Test
    public void deletedCookieUsesMaxAgeZero() {
        Cookie cookie = new Cookie("token", "");
        cookie.setMaxAge(0);

        String header = CookieUtil.buildHeader(cookie);

        assertTrue(header.contains("Max-Age=0"));
    }
}
