package com.dfrdev.tarn.render;



/**
 * IErrorRenderFactory.
 */
public interface IErrorRenderFactory {
	public Render getRender(int errorCode, String view);
}
