package com.dfrdev.tarn.render;


/**
 * JspRender.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JspRender extends Render {
	
	private static final long serialVersionUID = -688478484751775667L;
	private transient static boolean isSupportActiveRecord = true;

	public JspRender(String view) {
		this.view = view;
	}
	
	public void render() {
		// 在 jsp 页面使用如下指令则无需再指字符集, 否则是重复指定了,与页面指定的不一致时还会出乱码
		// <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
		// response.setContentType(contentType);
		// response.setCharacterEncoding(encoding);
		String target = resolveJspView(view);
		if (target == null) {
			throw new RenderException("非法的 JSP 视图路径: " + view);
		}
		try {
			request.getRequestDispatcher(target).forward(request, response);
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	/**
	 * 将视图限定在模板根目录内，并拒绝 {@code ..} 等穿越路径（S6）。
	 * 模板根目录取 {@link Render#getTemplateBaseDirectory()}，未配置时默认 {@code /WEB-INF/views}。
	 */
	static String resolveJspView(String view) {
		if (view == null) {
			return null;
		}
		String v = view.trim().replace('\\', '/');
		if (v.indexOf('\0') >= 0) {
			return null;
		}
		for (String segment : v.split("/")) {
			if ("..".equals(segment)) {
				return null;
			}
		}
		if (!v.startsWith("/")) {
			v = "/" + v;
		}
		String base = Render.getTemplateBaseDirectory();
		if (base == null || base.trim().length() == 0 || "/".equals(base.trim())) {
			base = "/WEB-INF/views";
		}
		base = base.trim();
		if (!base.startsWith("/")) {
			base = "/" + base;
		}
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return v.startsWith(base) ? v : null;
	}
	
	private static int DEPTH = 8;
	

}

/*
	private void handleGetterMethod(Map<String, Object> result, Method[] methods) {
		for (Method method : methods) {
			String methodName = method.getName();
			if (methodName.startsWith("get") && method.getParameterTypes().length == 0) {
				throw new RuntimeException("Not finished!");
			}
		}
	}
*/



