package com.dfrdev.tarn.util.net;

import com.dfrdev.tarn.core.boot.Tarn;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cookie 安全写入工具（Tarn 2.2+）。
 * <p>
 * 统一补齐 {@code HttpOnly}、HTTPS 下的 {@code Secure} 与 {@code SameSite}（S8）。
 * Servlet 3.x 未提供 SameSite API，因此直接构造 {@code Set-Cookie} 响应头。
 *
 * @author dfr
 */
public final class CookieUtil {

    private CookieUtil() {
    }

    /**
     * 安全地写入 Cookie：默认 HttpOnly，HTTPS 请求自动加 Secure，
     * SameSite 取配置 {@code cookieSameSite}（缺省 Lax，可选 Strict/None）。
     */
    public static void addCookie(HttpServletRequest request, HttpServletResponse response, Cookie cookie) {
        if (response == null || cookie == null) {
            return;
        }
        cookie.setHttpOnly(true);
        if (request != null && request.isSecure()) {
            cookie.setSecure(true);
        }
        response.addHeader("Set-Cookie", buildHeader(cookie));
    }

    /**
     * 构造带安全属性的 Set-Cookie 头。
     */
    static String buildHeader(Cookie cookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append('=');
        sb.append(cookie.getValue() == null ? "" : cookie.getValue());
        if (cookie.getPath() != null) {
            sb.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            sb.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getMaxAge() >= 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.getSecure()) {
            sb.append("; Secure");
        }
        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        sb.append("; SameSite=").append(sameSite());
        return sb.toString();
    }

    private static String sameSite() {
        String value = null;
        try {
            value = Tarn.sysDevConf("cookieSameSite");
        } catch (Exception ignore) {
            // 配置尚未初始化时按默认值处理
        }
        if (value == null) {
            return "Lax";
        }
        value = value.trim();
        if ("Strict".equalsIgnoreCase(value)) {
            return "Strict";
        }
        if ("None".equalsIgnoreCase(value)) {
            return "None";
        }
        return "Lax";
    }
}
