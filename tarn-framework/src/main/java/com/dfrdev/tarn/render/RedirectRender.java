package com.dfrdev.tarn.render;



import java.io.IOException;


public class RedirectRender extends Render {
	
	private static final long serialVersionUID = 1812102713097864255L;
    public RedirectRender(String view) {
        this.view = view;
    }
	public void render() {
		try {
			response.sendRedirect(view);
		} catch (IOException e) {
			throw new RenderException(e);
		}
	}


}

