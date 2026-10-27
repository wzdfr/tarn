package cc.ruixing.framework.tarn.render;


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
		try {
			request.getRequestDispatcher(view).forward(request, response);
		} catch (Exception e) {
			throw new RenderException(e);
		}
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



