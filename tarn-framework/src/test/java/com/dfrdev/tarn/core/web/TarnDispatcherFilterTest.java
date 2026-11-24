package com.dfrdev.tarn.core.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link TarnDispatcherFilter#normalizeControllerType(String)} 测试。
 * <p>{@code param} / {@code method} 已废弃，必须降级为 {@code auto}。
 *
 * @author dfr
 * @since 2.2
 */
public class TarnDispatcherFilterTest {

    @Test
    public void paramAndMethodDegradeToAuto() {
        assertEquals("auto", TarnDispatcherFilter.normalizeControllerType("param"));
        assertEquals("auto", TarnDispatcherFilter.normalizeControllerType("method"));
        assertEquals("auto", TarnDispatcherFilter.normalizeControllerType("PARAM"));
        assertEquals("auto", TarnDispatcherFilter.normalizeControllerType(" Method "));
    }

    @Test
    public void supportedTypesKeepUnchanged() {
        assertEquals("auto", TarnDispatcherFilter.normalizeControllerType("auto"));
        assertEquals("requestMapping", TarnDispatcherFilter.normalizeControllerType("requestMapping"));
        assertEquals("mapping", TarnDispatcherFilter.normalizeControllerType("mapping"));
    }

    @Test
    public void emptyTypeReturnsEmpty() {
        assertEquals("", TarnDispatcherFilter.normalizeControllerType(null));
        assertEquals("", TarnDispatcherFilter.normalizeControllerType(""));
    }
}
