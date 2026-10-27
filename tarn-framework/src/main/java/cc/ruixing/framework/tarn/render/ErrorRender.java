package cc.ruixing.framework.tarn.render;


import cc.ruixing.framework.tarn.core.Tarn;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * ErrorRender.
 */
public class ErrorRender extends Render {
	
	private static final long serialVersionUID = -7175292712918557096L;
	protected static final String contentType = "text/html;charset=" + getEncoding();
	
	protected static final String html404 = "<html><head><title>404 Not Found</title></head><body bgcolor='white'><center><h1>404 Not Found</h1></center><hr><center><a href='http://www.ruixing.cc/__KEEP_Tarn_PATH__' target='_blank'>tarn/" + Tarn.TARN_VERSION + "</a></center></body></html>";
	protected static final String html500 = "<html><head><title>500 Internal Server Error</title></head><body bgcolor='white'><center><h1>500 Internal Server Error</h1></center><hr><center><a href='http://www.ruixing.cc/__KEEP_Tarn_PATH__' target='_blank'>tarn/" + Tarn.TARN_VERSION + "</a></center></body></html>";
	
	protected static final String html401 = "<html><head><title>401 Unauthorized</title></head><body bgcolor='white'><center><h1>401 Unauthorized</h1></center><hr><center><a href='http://www.ruixing.cc/__KEEP_Tarn_PATH__' target='_blank'>Tarn/" + Tarn.TARN_VERSION + "</a></center></body></html>";
	protected static final String html403 = "<html><head><title>403 Forbidden</title></head><body bgcolor='white'><center><h1>403 Forbidden</h1></center><hr><center><a href='http://www.ruixing.cc/__KEEP_Tarn_PATH__' target='_blank'>Tarn/" + Tarn.TARN_VERSION+ "</a></center></body></html>";
	
	protected int errorCode;
	
	public ErrorRender(int errorCode, String view) {
		this.errorCode = errorCode;
		this.view = view;
	}
	
	public void render() {
		response.setStatus(getErrorCode());	// HttpServletResponse.SC_XXX_XXX
		
		// render with view
		String view = getView();
		if (view != null) {
			RenderFactory.getInstance().getJspRender(view).setContext(request, response).render();
			return;
		}
		
		// render with html content
		PrintWriter writer = null;
		try {
			response.setContentType(contentType);
	        writer = response.getWriter();
	        writer.write(getErrorHtml());
	        writer.flush();
		} catch (IOException e) {
			throw new RenderException(e);
		}
		finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public String getErrorHtml() {
		int errorCode = getErrorCode();
		if (errorCode == 404)
			return html404;
		else if (errorCode == 500)
			return html500;
		else if (errorCode == 401)
			return html401;
		else if (errorCode == 403)
			return html403;
		else
			return "<html><head><title>" + errorCode + " Error</title></head><body bgcolor='white'><center><h1>" + errorCode + " Error</h1></center><hr><center><a href='http://www.ruixing.cc/__KEEP_Tarn_PATH__' target='_blank'>tarn/" + Tarn.TARN_VERSION + "</a></center></body></html>";
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}





