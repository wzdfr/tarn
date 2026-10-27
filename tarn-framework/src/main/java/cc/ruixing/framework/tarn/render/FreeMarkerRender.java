package cc.ruixing.framework.tarn.render;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.*;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * FreeMarkerRender.
 */
public class FreeMarkerRender extends Render {

    private static final long serialVersionUID = -7649769283048920381L;
    private transient static String encoding = getEncoding();
    private transient static String contentType = "text/html; charset=" + encoding;
    private transient static final Configuration config = new Configuration();
    /**
     * 多任务试图模式
     */
    private transient static Configuration config_stringLoader = null;
    private transient static StringTemplateLoader stringTemplateLoader = null;

    private transient static boolean jarLoad = false;


    public FreeMarkerRender(String view) {
        this.view = view;
    }

    protected static Configuration getConfiguration() {
        return config;
    }

    protected static StringTemplateLoader getStringTemplateLoader() {
        return stringTemplateLoader;
    }

    protected static Configuration getConfig_stringLoader() {
        if (config_stringLoader == null) {
            config_stringLoader = new Configuration();
            initStringLoader();
        }
        return config_stringLoader;
    }


    public static void setStringTemplateLoader(StringTemplateLoader stringTemplateLoader) {
        FreeMarkerRender.stringTemplateLoader = stringTemplateLoader;
    }

    public static void setConfig_stringLoader(Configuration config_stringLoader) {
        FreeMarkerRender.config_stringLoader = config_stringLoader;
    }

    public static void setProperty(String propertyName, String propertyValue) {
        try {
            FreeMarkerRender.getConfiguration().setSetting(propertyName, propertyValue);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setProperties(Properties properties) {
        try {
            FreeMarkerRender.getConfiguration().setSettings(properties);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    static void init(ServletContext servletContext, Locale locale, int template_update_delay) {
        //加载并设置freemarker.properties
        URL url = FreeMarkerRender.class.getClassLoader().getResource("freemarker.properties");
        try {
            if (url != null) {
                File f = new File(url.getPath());
                if (f.exists()) {
                    Properties props = new Properties();
                    props.load(new FileInputStream(f));
                    config.setSettings(props);
                }
            } else {
                if (encoding == null || encoding.equals("")) {
                    encoding = getEncoding();
                    contentType = "text/html; charset=" + encoding;
                }
                // Initialize the FreeMarker configuration;
                // - Create a configuration instance
                // config = new Configuration();
                // - Templates are stoted in the WEB-INF/templates directory of the Web app.
//                new MultiTemplateLoader()
                if (isTemplateMulitState()) {
                    config.setTemplateLoader(getMultiTemplateLoader());
                } else {
                    config.setServletContextForTemplateLoading(servletContext, getTemplateBaseDirectory());    // "WEB-INF/templates"
                }
                // - Set update dealy to 0 for now, to ease debugging and testing.
                //   Higher value should be used in production environment.
                //MultiTemplateLoader multiTemplateLoader[]=new MultiTemplateLoader[]{};
                if (isDevMode()) {
                    //意思是不用每次都从硬盘上读出模版文件，而是从内存中读出。
                    config.setTemplateUpdateDelay(0);
                } else {
                    config.setTemplateUpdateDelay(template_update_delay);
                }
                // - Set an error handler that prints errors so they are readable with
                //   a HTML browser.
                // config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
                // - Use beans wrapper (recommmended for most applications)
                config.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
                // - Set the default charset of the template files
                config.setDefaultEncoding(encoding);        // config.setDefaultEncoding("ISO-8859-1");
                // - Set the charset of the output. This is actually just a hint, that
                //   templates may require for URL encoding and for generating META element
                //   that uses http-equiv="Content-type".
                config.setOutputEncoding(encoding);            // config.setOutputEncoding("UTF-8");
                // - Set the default locale
                config.setLocale(locale /* Locale.CHINA */);        // config.setLocale(Locale.US);
                config.setLocalizedLookup(false);
                config.setClassicCompatible(true); //值为空时

                // 去掉int型输出时的逗号, 例如: 123,456
                // config.setNumberFormat("#");		//config.setNumberFormat("0"); 也可以
                config.setNumberFormat("#0.#####");
            }
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render() {
        response.setContentType(contentType);
        Enumeration<String> attrs = request.getAttributeNames();
        Map root = new HashMap();
        while (attrs.hasMoreElements()) {
            String attrName = attrs.nextElement();
            root.put(attrName, request.getAttribute(attrName));
        }

        PrintWriter writer = null;
        try {
            Template template = config.getTemplate(view);

            writer = response.getWriter();
            template.process(root, writer);        // Merge the data-model and the template
        } catch (Exception e) {
            throw new RenderException(e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    /**
     * 指定模板文件生成
     *
     * @param view 模板页面文件路径
     * @param root 数据map
     * @return String   返回生成解析的字符串
     */
    public String fileLoader(String view, Map root) {
        response.setContentType(contentType);
        String str = "";
        StringWriter out = null;
        try {
            Template template = config.getTemplate(view);
            out = new StringWriter(2048);
            template.process(root, out);        // Merge the data-model and the template
            if (out != null) str = out.toString();
        } catch (Exception e) {
            throw new RenderException(e);
        } finally {
            if (out != null)
                out.flush();
        }
        return str;
    }

    /**
     * string字符串模板生成方式
     *
     * @param stringTemplate 字符串模板
     * @param root           数据map
     * @param templateName   模板名称
     * @return String  返回生成解析的字符串
     */
    public static String stringLoader(String stringTemplate, Map root, String templateName) {
        String str = "";
        Writer out = null;
        try {
            if (stringTemplateLoader == null) stringTemplateLoader = new StringTemplateLoader();
            if (config_stringLoader == null) {
                config_stringLoader = new Configuration();
                initStringLoader();
                config_stringLoader.setTemplateLoader(stringTemplateLoader);
            }

            if (stringTemplateLoader.findTemplateSource(templateName) == null) {
                stringTemplateLoader.putTemplate(templateName, stringTemplate);
                config_stringLoader.setTemplateLoader(stringTemplateLoader);
            }
            Template template = config_stringLoader.getTemplate(templateName, encoding == null ? "UTF-8" : encoding);
            out = new StringWriter(2048);
            template.process(root, out);
            if (out != null) str = out.toString();
        } catch (IOException e) {
            throw new RenderException(e);
        } catch (TemplateException e) {
            throw new RenderException(e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                    throw new RenderException(e);
                }
            }
        }

        return str;
    }

    private static void initStringLoader() {
        config_stringLoader.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        // - Use beans wrapper (recommmended for most applications)
        config_stringLoader.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
        // - Set the default charset of the template files
        config_stringLoader.setDefaultEncoding(encoding);        // config.setDefaultEncoding("ISO-8859-1");
        // - Set the charset of the output. This is actually just a hint, that
        //   templates may require for URL encoding and for generating META element
        //   that uses http-equiv="Content-type".
        config_stringLoader.setOutputEncoding(encoding);            // config.setOutputEncoding("UTF-8");
        config_stringLoader.setTemplateUpdateDelay(60);
        // - Set the default locale
        config_stringLoader.setLocalizedLookup(false);
        config_stringLoader.setClassicCompatible(true); //值为空时

        // 去掉int型输出时的逗号, 例如: 123,456
        // config.setNumberFormat("#");		//config.setNumberFormat("0"); 也可以
        config_stringLoader.setNumberFormat("#0.#####");
    }
}




