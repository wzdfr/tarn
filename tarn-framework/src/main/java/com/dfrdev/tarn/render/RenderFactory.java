package com.dfrdev.tarn.render;


import com.dfrdev.tarn.aop.annotation.EViewType;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.web.PageContext;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.PathKit;
import com.dfrdev.tarn.util.text.StringUtil;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

import javax.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

    /**
     * 允许跳转的外部主机白名单（支持 {@code *.example.com}）；未配置时拒绝所有外链重定向（S7）
     */
    private static final Set<String> redirectAllowedHosts = new HashSet<String>();

    static {
        String hosts = Tarn.sysDevConf("redirectAllowedHosts");
        if (hosts != null) {
            for (String host : hosts.split(",")) {
                if (host.trim().length() > 0) {
                    redirectAllowedHosts.add(host.trim());
                }
            }
        }
    }

    /**
     * 运行时设置重定向主机白名单（覆盖配置值）。
     */
    public static void setRedirectAllowedHosts(String... hosts) {
        redirectAllowedHosts.clear();
        if (hosts != null) {
            for (String host : hosts) {
                if (host != null && host.trim().length() > 0) {
                    redirectAllowedHosts.add(host.trim());
                }
            }
        }
    }

    /**
     * 校验并规范化视图路径：拒绝 NUL、反斜杠与 {@code ..} 路径段（S6）。
     */
    static String checkViewPath(String view) {
        return ViewPathKit.checkViewPath(view);
    }

    private static boolean isRedirectView(String view) {
        return ViewPathKit.isRedirectView(view);
    }

    /**
     * 外链跳转主机白名单校验（S7），未命中返回 false。
     */
    private static boolean isAllowedRedirect(String url) {
        return ViewPathKit.isAllowedRedirect(url, redirectAllowedHosts);
    }

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
        view = checkViewPath(view);
        if (isRedirectView(view)) {
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
        view = checkViewPath(view);
        if (isRedirectView(view)) {
            return getRedirectRender(view);
        } else {
            view=view.trim();
            if(StringUtil.isNotEmpty(viewRootPath)){
                viewRootPath=viewRootPath.trim();
                if(!viewRootPath.substring(viewRootPath.length()-1).equals("/")) viewRootPath+="/";
                if(view.substring(0,1).equals("/")) view=view.substring(1,view.length());
                view=viewRootPath+view;
            }
            return mainRenderFactory.getRender(checkViewPath(view));
        }
    }

    /**
     * 调转渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getRedirectRender(String view) {
        String url = view == null ? null : view.trim();
        if (!isRedirectView(url) || !isAllowedRedirect(url)) {
            LogUtil.warn("[Render] 拒绝未授权的重定向地址: " + view);
            throw new RenderException("非法的重定向地址: " + view);
        }
        return new RedirectRender(url);
    }

    /**
     * FreeMarker渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getFreeMarkerRender(String view) {
        return new FreeMarkerRender(checkViewPath(view));
    }

    /**
     * Jsp渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getJspRender(String view) {
        return new JspRender(checkViewPath(view));
    }

    /**
     * Velocity渲染
     *
     * @param view 路径
     * @return Render
     */
    public Render getVelocityRender(String view) {
        return new VelocityRender(checkViewPath(view));
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
