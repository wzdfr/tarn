package com.dfrdev.tarn.render;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 视图路径与跳转地址安全校验（S6/S7）。
 * <p>
 * 独立于 {@link RenderFactory}，便于单测且不触发渲染器初始化。
 *
 * @author dfr
 */
public final class ViewPathKit {

    private ViewPathKit() {
    }

    /**
     * 校验并规范化视图路径：拒绝 NUL、反斜杠与 {@code ..} 路径段。
     *
     * @throws RenderException 路径非法时抛出
     */
    public static String checkViewPath(String view) {
        if (view == null) {
            return null;
        }
        String v = view.trim();
        if (v.indexOf('\0') >= 0 || v.indexOf('\\') >= 0) {
            throw new RenderException("非法的视图路径: " + view);
        }
        for (String segment : v.split("/")) {
            if ("..".equals(segment)) {
                throw new RenderException("非法的视图路径: " + view);
            }
        }
        return v;
    }

    /**
     * 是否外链跳转（仅识别以 http:// 或 https:// 开头的绝对地址）。
     */
    public static boolean isRedirectView(String view) {
        if (view == null) {
            return false;
        }
        String v = view.trim();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    /**
     * 跳转主机白名单校验，支持 {@code *.example.com} 通配子域。
     */
    public static boolean isAllowedRedirect(String url, Set<String> allowedHosts) {
        if (url == null || allowedHosts == null || allowedHosts.isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            host = host.toLowerCase(Locale.ROOT);
            for (String allowed : allowedHosts) {
                String a = allowed.toLowerCase(Locale.ROOT);
                if (a.startsWith("*.")) {
                    if (host.endsWith(a.substring(1))) {
                        return true;
                    }
                } else if (host.equals(a)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
