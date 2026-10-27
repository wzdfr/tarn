package cc.ruixing.framework.tarn.render;


public interface MainRenderFactory {
	
	/**
	 * 返回一个渲染
	 * @param view 渲染页面
	 */
	Render getRender(String view);
	
	/**
     * 渲染文件的扩展名
	 */
	String getViewExtension();
}


