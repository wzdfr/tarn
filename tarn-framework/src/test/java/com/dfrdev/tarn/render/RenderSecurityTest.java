package com.dfrdev.tarn.render;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 视图路径与重定向安全校验测试（S6/S7）。
 */
public class RenderSecurityTest {

    @Test
    public void checkViewPathRejectsTraversal() {
        try {
            ViewPathKit.checkViewPath("../../etc/passwd");
            fail("应拒绝路径穿越");
        } catch (RenderException expected) {
            // ok
        }
        try {
            ViewPathKit.checkViewPath("a/../b");
            fail("应拒绝路径穿越");
        } catch (RenderException expected) {
            // ok
        }
    }

    @Test
    public void checkViewPathAcceptsNormalView() {
        assertEquals("/WEB-INF/views/a.html", ViewPathKit.checkViewPath(" /WEB-INF/views/a.html "));
        assertEquals("a.html", ViewPathKit.checkViewPath("a.html"));
    }

    @Test
    public void redirectViewRequiresAbsoluteHttpUrl() {
        assertTrue(ViewPathKit.isRedirectView("https://example.com/a"));
        assertTrue(ViewPathKit.isRedirectView("http://example.com/a"));
        assertFalse(ViewPathKit.isRedirectView("/login"));
        assertFalse(ViewPathKit.isRedirectView("foo/http://example.com"));
    }

    @Test
    public void redirectHostMustBeWhitelisted() {
        Set<String> hosts = new HashSet<String>();
        assertFalse(ViewPathKit.isAllowedRedirect("https://evil.example.com/a", hosts));

        hosts.add("example.com");
        hosts.add("*.trusted.com");
        assertTrue(ViewPathKit.isAllowedRedirect("https://example.com/a", hosts));
        assertTrue(ViewPathKit.isAllowedRedirect("https://api.trusted.com/a", hosts));
        assertFalse(ViewPathKit.isAllowedRedirect("https://eviltrusted.com/a", hosts));
    }

    @Test
    public void jspViewLimitedToTemplateBase() {
        assertEquals("/WEB-INF/views/test.jsp", JspRender.resolveJspView("/WEB-INF/views/test.jsp"));
        assertNull(JspRender.resolveJspView("/WEB-INF/views/../web.xml"));
        assertNull(JspRender.resolveJspView("/index.jsp"));
    }
}
