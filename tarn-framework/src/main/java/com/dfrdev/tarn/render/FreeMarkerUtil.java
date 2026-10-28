package com.dfrdev.tarn.render;


import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.util.LogUtil;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * FreeMarker试图的一些辅助静态方法
 * Created by dfr on 14-2-20.
 */
public class FreeMarkerUtil {

    private static int CACHE_SECOND = 5 * 1000 * 60;

    static {
        String confCache = Tarn.sysDevConf("freemarker_stringLoadTemplate_cache", "");
        if (!confCache.trim().isEmpty()) {
            try {
                CACHE_SECOND = Integer.parseInt(confCache.trim()) * 1000;
            } catch (NumberFormatException e) {
                // Tarn 2.1：配置非法时保留默认 5 分钟，不因静态块异常导致类加载失败
                LogUtil.error("[Tarn-Config] freemarker_stringLoadTemplate_cache 配置非法: " + confCache + "，已回退默认值");
            }
        }
    }


    public static Configuration getConfiguration() {
        return FreeMarkerRender.getConfiguration();
    }

    public static Configuration getConfigurationStringLoader() {
        return FreeMarkerRender.getConfig_stringLoader();
    }

    public static StringTemplateLoader getStringTemplateLoader() {
        return FreeMarkerRender.getStringTemplateLoader();
    }

    /**
     * 指定模板文件生成
     *
     * @param view 模板页面文件路径
     * @param root 数据map
     * @return String   返回生成解析的字符串
     */
    public static String fileLoader(String view, Map root) {
        String str = "";
        StringWriter out = null;
        try {
            Template template = FreeMarkerRender.getConfiguration().getTemplate(view);
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

    public static String fileLoader(String view, Map root, String path) {
        String str = "";
        StringWriter out = null;
        try {
            Template template = FreeMarkerRender.getConfiguration().getTemplate(view);
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
        Configuration config = FreeMarkerRender.getConfig_stringLoader();
        StringTemplateLoader stringLoader = FreeMarkerRender.getStringTemplateLoader();
        try {
            if (stringLoader == null) {
                stringLoader = new StringTemplateLoader();
                FreeMarkerRender.setStringTemplateLoader(stringLoader);
            }
            if (config == null) {
                config = new Configuration();
                config.setTemplateLoader(stringLoader);
                initStringLoader(config);
                FreeMarkerRender.setConfig_stringLoader(config);
            }

            if (stringLoader.findTemplateSource(templateName) == null || stringLoader.findTemplateSource(templateName).equals("")) {
                stringLoader.putTemplate(templateName, stringTemplate);
                config.setTemplateLoader(stringLoader);
            } else {
                long lastMod = stringLoader.getLastModified(stringLoader.findTemplateSource(templateName));
                if ((System.currentTimeMillis() - lastMod) >= CACHE_SECOND) {
                    stringLoader.putTemplate(templateName, stringTemplate, System.currentTimeMillis());
                    config.setTemplateLoader(stringLoader);
                }
            }

            Template template = config.getTemplate(templateName, FreeMarkerRender.getEncoding() == null ? "UTF-8" : FreeMarkerRender.getEncoding());
            out = new StringWriter(2048);
            template.process(root, out);
            if (out != null) str = out.toString();
        } catch (IOException e) {
            LogUtil.error(e.getMessage());
            return "[IO 解析错误]" + stringTemplate;
        } catch (TemplateException e) {
            LogUtil.error(e.getMessage());
            return "[模板标签解析错误,请检查是否有误]" + stringTemplate;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                    LogUtil.error(e.getMessage());
                    return "[模板标签解析错误,请检查是否有误]" + stringTemplate;
                }
            }
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
    public static String stringLoaderThrow(String stringTemplate, Map root, String templateName) throws IOException, TemplateException {
        String str = "";
        Writer out = null;
        Configuration config = FreeMarkerRender.getConfig_stringLoader();
        StringTemplateLoader stringLoader = FreeMarkerRender.getStringTemplateLoader();
        if (stringLoader == null) {
            stringLoader = new StringTemplateLoader();
            FreeMarkerRender.setStringTemplateLoader(stringLoader);
        }
        if (config == null) {
            config = new Configuration();
            config.setTemplateLoader(stringLoader);
            initStringLoader(config);
            FreeMarkerRender.setConfig_stringLoader(config);
        }

        if (stringLoader.findTemplateSource(templateName) == null || stringLoader.findTemplateSource(templateName).equals("")) {
            stringLoader.putTemplate(templateName, stringTemplate);
            config.setTemplateLoader(stringLoader);
        } else {
            long lastMod = stringLoader.getLastModified(stringLoader.findTemplateSource(templateName));
            if ((System.currentTimeMillis() - lastMod) >= CACHE_SECOND) {
                stringLoader.putTemplate(templateName, stringTemplate, System.currentTimeMillis());
                config.setTemplateLoader(stringLoader);
            }
        }

        Template template = config.getTemplate(templateName, FreeMarkerRender.getEncoding() == null ? "UTF-8" : FreeMarkerRender.getEncoding());
        out = new StringWriter(2048);
        template.process(root, out);
        if (out != null) str = out.toString();
        out.close();
        return str;
    }

    private static void initStringLoader(Configuration configuration) {
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        // - Use beans wrapper (recommmended for most applications)
        configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
        // - Set the default charset of the template files
        configuration.setDefaultEncoding(FreeMarkerRender.getEncoding());        // config.setDefaultEncoding("ISO-8859-1");
        // - Set the charset of the output. This is actually just a hint, that
        //   templates may require for URL encoding and for generating META element
        //   that uses http-equiv="Content-type".
        configuration.setOutputEncoding(FreeMarkerRender.getEncoding());            // config.setOutputEncoding("UTF-8");
        // - Set the default locale
        configuration.setLocalizedLookup(false);
        configuration.setClassicCompatible(true); //值为空时

        // 去掉int型输出时的逗号, 例如: 123,456
        // config.setNumberFormat("#");		//config.setNumberFormat("0"); 也可以
        configuration.setNumberFormat("#0.#####");
    }

}
