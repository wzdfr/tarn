package cc.ruixing.framework.tarn.render;

import freemarker.cache.MultiTemplateLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 页面渲染抽选类
 * */
public abstract class Render {
    private static final long serialVersionUID = 4055676662365675029L;

    protected String view;
    protected transient HttpServletRequest request;
    protected transient HttpServletResponse response;
    /**
     * 视图页面渲染编码
     */
    private transient static String encoding;
    /**
     *是否设置多路径模板加载方式
     * */
    private transient static boolean templateMulitState=false;

    private transient static MultiTemplateLoader multiTemplateLoader;

    /**
     * 开发模式
     */
    private transient static boolean devMode;


    /**
     * 视图模板文件目录
     */
    private transient static String templateBaseDirectory;

    static final void init(String encoding, boolean devMode, String templateBaseDirectory) {
        Render.encoding = encoding;
        Render.devMode = devMode;
        if (templateBaseDirectory == null || templateBaseDirectory.equals(""))
            Render.templateBaseDirectory = "/";
        else Render.templateBaseDirectory = templateBaseDirectory;
    }

    public Render setContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        return this;
    }

    /**
     * Render to client
     */
    public abstract void render();


    public static boolean isTemplateMulitState() {
        return templateMulitState;
    }

    public static void setTemplateMulitState(boolean templateMulitState) {
        Render.templateMulitState = templateMulitState;
    }

    public static MultiTemplateLoader getMultiTemplateLoader() {
        return multiTemplateLoader;
    }

    public static void setMultiTemplateLoader(MultiTemplateLoader multiTemplateLoader) {
        Render.multiTemplateLoader = multiTemplateLoader;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public static String getEncoding() {
        return encoding;
    }

    public static String getTemplateBaseDirectory() {
        return templateBaseDirectory;
    }

    public static void setTemplateBaseDirectory(String templateBaseDirectory) {
        Render.templateBaseDirectory = templateBaseDirectory;
    }

    public static boolean isDevMode() {
        return devMode;
    }

    public static void setDevMode(boolean devMode) {
        Render.devMode = devMode;
    }

    public static void setEncoding(String encoding) {
        Render.encoding = encoding;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }


}
