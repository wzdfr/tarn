package cc.ruixing.framework.tarn.render;


import cc.ruixing.framework.tarn.aop.annotation.EViewType;
import cc.ruixing.framework.tarn.core.Tarn;
import cc.ruixing.framework.tarn.core.PageContext;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.PathKit;
import cc.ruixing.framework.tarn.util.StringUtil;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

import javax.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Vector;

/**
 * RenderFactory
 * User: dfr
 * Date: 13-12-29
 * Time: 下午5:35
 */
public class RenderFactory {
    private static MainRenderFactory mainRenderFactory;
    private static ErrorRenderFactory errorRenderFactory;
    private static final RenderFactory rf = new RenderFactory();
    private static final String defaultViewType = Tarn.sysDevConf("defaultViewType");
    private static PageContext context;

    private RenderFactory() {
        init();
    }

    public static RenderFactory getInstance() {
        return rf;
    }

    public void init() {
        context = Tarn.getPageContext();
        Render.init(Tarn.sysDevConf("encoding"), Boolean.parseBoolean(Tarn.sysDevConf("devMode")), Tarn.sysDevConf("templateBaseDirectory"));
        if (Tarn.sysDevConf("isFreeMarkerMultiDir") != null && Tarn.sysDevConf("isFreeMarkerMultiDir").equalsIgnoreCase("true")) {
            Vector<TemplateLoader> vector = FreeMarkerMultiDirFactory.getMultiDirPool();
            if (vector.size() == 0) {
                LogUtil.error("FreeMarker templateLoader is Empty.Please use FreeMarkerMultiDirFactory.register()");
                throw new RenderException("FreeMarker templateLoader is Empty.Please use FreeMarkerMultiDirFactory.register()");
            }
            TemplateLoader templateLoader[] = new TemplateLoader[vector.size()];
            for (int i = 0, n = vector.size(); i < n; i++) {
                templateLoader[i] = vector.get(i);
            }
            Render.setMultiTemplateLoader(new MultiTemplateLoader(templateLoader));
            Render.setTemplateMulitState(true);
        }
        initFreeMarkerRender();
        initVelocityRender();
        initFileRender(context.getRequest().getServletContext());
        // create errorRenderFactory
        if (errorRenderFactory == null) {
            errorRenderFactory = new ErrorRenderFactory();
        }
    }


    private void initFileRender(ServletContext servletContext) {
        FileRender.init(servletContext);
    }

    private void initFreeMarkerRender() {
        try {
            Class.forName("freemarker.template.Template");    // detect freemarker.jar
            // Tarn 2.1 修复：配置缺失时老实现 Integer.parseInt(null) 抛 NumberFormatException，
            // 导致仅使用 application.yml（未写 DEFAULT_FREEMARKER_TEMPLATE_UPDATE_DELAY）的项目启动失败。
            // 缺省 3600 秒（与老 dev.properties 注释默认一致）；devMode=true 时该值不参与（FreeMarkerRender 内部固定为 0）。
            int updateDelay = parseSecond(
                    Tarn.sysDevConf("DEFAULT_FREEMARKER_TEMPLATE_UPDATE_DELAY", "3600"), 3600);
            FreeMarkerRender.init(context.getRequest().getServletContext(), Locale.CHINA, updateDelay);
        } catch (ClassNotFoundException e) {
            LogUtil.error("freemarker can not be supported!");
            LogUtil.debug("freemarker can not be supported!");
        }
    }

    /**
     * 秒数解析：非法/缺省时回退默认值，不让配置问题中断启动
     */
    private static int parseSecond(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LogUtil.error("[Tarn-Config] freemarker template-update-delay 配置非法: " + value + "，已回退默认值 " + defaultValue);
            return defaultValue;
        }
    }

    private void initVelocityRender() {
        try {
            Class.forName("org.apache.velocity.VelocityContext");
            VelocityRender.init(context.getRequest().getServletContext());
        } catch (ClassNotFoundException e) {
            LogUtil.error("Velocity can not be supported!");
            // System.out.println("Velocity can not be supported!");
        }
    }

    // --------
    private static final class FreeMarkerRenderFactory implements MainRenderFactory {
        public Render getRender(String view) {
            return new FreeMarkerRender(view);
        }

        public String getViewExtension() {
            return ".html";
        }
    }

    private static final class JspRenderFactory implements MainRenderFactory {
        public Render getRender(String view) {
            return new JspRender(view);
        }

        public String getViewExtension() {
            return ".jsp";
        }
    }

    private static final class VelocityRenderFactory implements MainRenderFactory {
        public Render getRender(String view) {
            return new VelocityRender(view);
        }

        public String getViewExtension() {
            return ".html";
        }
    }

    private static final class TextRenderFactory implements MainRenderFactory {
        public Render getRender(String text) {
            return new TextRender(text);
        }

        public String getViewExtension() {
            return ".html";
        }
    }

    private static final class ErrorRenderFactory implements IErrorRenderFactory {
        public Render getRender(int errorCode, String view) {
            return new ErrorRender(errorCode, view);
        }
    }

    private String getFileRenderPath() {
        String result = null;
        if (result == null) {
            result = PathKit.getWebRootPath() + File.separator + "download" + File.separator;
        }
        if (!result.endsWith(File.separator) && !result.endsWith("/")) {
            result = result + File.separator;
        }
        return result;
    }


    private void checkSelectView(Method method) {
        if (method.isAnnotationPresent(EViewType.class)) {
            ViewType viewType = method.getAnnotation(EViewType.class).value();
            if (viewType == ViewType.FREE_MARKER)
                mainRenderFactory = new FreeMarkerRenderFactory();
            else if (viewType == ViewType.JSP)
                mainRenderFactory = new JspRenderFactory();
            else if (viewType == ViewType.VELOCITY)
                mainRenderFactory = new VelocityRenderFactory();
            else if (viewType == ViewType.TEXT)
                mainRenderFactory = new TextRenderFactory();
            else
                throw new RuntimeException("View Type can not be null.");
        } else {
            if (defaultViewType.equals("FREE_MARKER"))
                mainRenderFactory = new FreeMarkerRenderFactory();
            else if (defaultViewType.equals("JSP"))
                mainRenderFactory = new JspRenderFactory();
            else if (defaultViewType.equals("VELOCITY"))
                mainRenderFactory = new VelocityRenderFactory();
            else
                throw new RuntimeException("View Type can not be null.");
        }
    }

    /**
     * 返回试图渲染
     */
    public Render getRender(String view, Method method) {
        checkSelectView(method);
        if (view.indexOf("http://") != -1 || view.indexOf("https://") != -1) {
            return getRedirectRender(view);
        } else {
            return mainRenderFactory.getRender(view);
        }
    }
    /**
     * 返回试图渲染
     */
    public Render getRender(String view, Method method,String viewRootPath) {
        checkSelectView(method);
        if (view.indexOf("http://") != -1 || view.indexOf("https://") != -1) {
            return getRedirectRender(view);
        } else {
            view=view.trim();
            if(StringUtil.isNotEmpty(viewRootPath)){
                viewRootPath=viewRootPath.trim();
                if(!viewRootPath.substring(viewRootPath.length()-1).equals("/")) viewRootPath+="/";
                if(view.substring(0,1).equals("/")) view=view.substring(1,view.length());
                view=viewRootPath+view;
            }
            return mainRenderFactory.getRender(view);
        }
    }

    /**
     * 调转渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getRedirectRender(String view) {
        return new RedirectRender(view);
    }

    /**
     * FreeMarker渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getFreeMarkerRender(String view) {
        return new FreeMarkerRender(view);
    }

    /**
     * Jsp渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getJspRender(String view) {
        return new JspRender(view);
    }

    /**
     * Velocity渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getVelocityRender(String view) {
        return new VelocityRender(view);
    }

    /**
     * text文本渲染
     *
     * @param text 内容
     * @return render
     */
    public Render getTextRender(String text) {
        return new TextRender(text);
    }
    /**
     * text文本渲染
     *
     * @param text 内容
     * @param contentType 指定contentType
     * @return render
     */
    public Render getTextRender(String text,RenderContentType contentType){
        return new TextRender(text,contentType);
    }

    /**
     * 错误页面渲染
     *
     * @param errorCode 错误代码
     * @param view      页面地址
     * @return Render
     */
    public Render getErrorRender(int errorCode, String view) {
        return errorRenderFactory.getRender(errorCode, view);
    }

    /**
     * 错误页面渲染
     *
     * @param errorCode 错误代码
     * @return Render
     */
    public Render getErrorRender(int errorCode) {
        return errorRenderFactory.getRender(errorCode, Tarn.sysDevConf("error_page_" + errorCode));
    }

    /**
     * 文件渲染
     *
     * @param filePath 文件名
     * @return Render
     */
    public Render getFileRender(String filePath) {
        return new FileRender(filePath);
    }

    /**
     * 文件渲染
     *
     * @param file 文件
     * @return Render
     */
    public Render getFileRender(File file) {
        return new FileRender(file);
    }
}
