package com.dfrdev.tarn.core;


import com.dfrdev.tarn.aop.Inject;
import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.collection.RunMode;
import com.dfrdev.tarn.core.connection.ConnectionFactory;
import com.dfrdev.tarn.plugin.IPlugin;
import com.dfrdev.tarn.plugin.ehcache.EhCachePlugin;
import com.dfrdev.tarn.plugin.ehcache.TarnCacheKit;
import com.dfrdev.tarn.upload.OreillyCos;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.PathKit;
import com.dfrdev.tarn.util.StringUtil;
import com.dfrdev.tarn.util.xml.XMLElement;
import com.dfrdev.tarn.util.xml.XMLMultiLoader;

import javax.servlet.FilterConfig;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    protected static Map<String, Object> configMap = new HashMap<String, Object>();
    private static final Plugins plugins = new Plugins();
    private static final Filters filters = new Filters();
    public static int OnlineUserCount = 0;

    public static int LoginUserCount = 0;

    public static boolean isInstalled = false;

    public static boolean isAllowLogin = true;

    public static boolean isPluginContext = false;

    private static String AppCode = null;

    private static String AppName = null;

    static boolean ComplexDepolyMode = false;

    static boolean FrontDeploy = false;
    public static int ServletMajorVersion;
    public static int ServletMinorVersion;
    protected static String GlobalCharset;
    private static String ClassesPath;
    private static Boolean debugModeFlag;
    private static String uploadedFileSaveDirectory;

    private static int DEFAULT_MAX_POST_SIZE = 1024 * 1024 * 10;            // Default max post size of multipart request: 10 Meg

    protected static void init() {
        if (!configMap.containsKey("System.JavaVersion")) {
            ConfigLoader.load();
            configMap.put("App.ContextRealPath", getContextRealPath());
            configMap.put("System.JavaVersion", System.getProperty("java.version"));
            configMap.put("System.JavaVendor", System.getProperty("java.vendor"));
            configMap.put("System.JavaHome", System.getProperty("java.home"));
            configMap.put("System.OSPatchLevel", System.getProperty("sun.os.patch.level"));
            configMap.put("System.OSArch", System.getProperty("os.arch"));
            configMap.put("System.OSVersion", System.getProperty("os.version"));
            configMap.put("System.OSName", System.getProperty("os.name"));
            if ((System.getProperty("os.name").toLowerCase().indexOf("windows") > 0) && (System.getProperty("os.name").equals("6.1"))) {
                configMap.put("System.OSName", "Windows 7");
            }
            configMap.put("System.OSUserLanguage", System.getProperty("user.language"));
            configMap.put("System.OSUserName", System.getProperty("user.name"));
            configMap.put("System.LineSeparator", System.getProperty("line.separator"));
            configMap.put("System.FileSeparator", System.getProperty("file.separator"));
            configMap.put("System.FileEncoding", System.getProperty("file.encoding"));

            List<XMLElement> datas = ConfigLoader.getElements("framework.controllers.mapping");
            if (datas == null) {
                LogUtil.warn("File context-mapping.xml not found");
                isInstalled = false;
                return;
            }
            for (XMLElement data : datas) {
                configMap.put("controllers." + (String) data.getAttributes().get("package"), data.getAttributes().get("name"));
            }
            datas = ConfigLoader.getElements("tarncms.allowUploadExt.config");
            for (XMLElement data : datas) {
                configMap.put((String) data.getAttributes().get("name"), data.getText());
            }
            datas = ConfigLoader.getElements("data.config");
            for (XMLElement data : datas) {
                configMap.put((String) data.getAttributes().get("name"), (String) data.getAttributes().get("value"));
            }
            ComplexDepolyMode = "true".equals(configMap.get("App.ComplexDepolyMode"));
            FrontDeploy = "true".equals(configMap.get("App.FrontDeploy"));

            if (datas.size() > 0)
                isInstalled = true;
            else {
                isInstalled = false;
            }
            LogUtil.info("----" + getAppCode() + "(" + getAppName() + "): Config Initialized----");
        }
    }

    /**
     * 配置Tarn相关
     */
    static void configTarn(TarnConfig tarnConfig, FilterConfig filterConfig) {
        ConnectionFactory.init();
        new Inject();
        if (tarnConfig != null) tarnConfig.configFilter(filters);
        PluginInit.init(filterConfig);
        if (tarnConfig != null) tarnConfig.configPlugin(plugins);

        PluginInit.startPlugins(plugins.getPluginList());
        // Tarn 2.0+: YAML 声明式装配插件（tarn.redis.enabled / tarn.mybatis.enabled / tarn.plugins）
        com.dfrdev.tarn.plugin.PluginAutoConfig.configure();
        // Tarn 2.0+: 启动新版插件（PluginManager 管理的新式插件，与旧 IPlugin 并存兼容）
        com.dfrdev.tarn.plugin.PluginManager.me().startAll();
        ExpandFilterFactory.init(filterConfig);
        initOreillyCos();
        TarnCacheKit.init();
    }


    private static void initOreillyCos() {
        if (OreillyCos.isMultipartSupported()) {
            String uploadedFileSaveDirectory = Config.getUploadedFileSaveDirectory();
            if (uploadedFileSaveDirectory == null || "".equals(uploadedFileSaveDirectory.trim())) {

                if(RunMode.getInstance().isJarStatus()){
                    uploadedFileSaveDirectory = Config.getContextRealPath() + File.separator + "upload" + File.separator;
                    Config.setUploadedFileSaveDirectory(uploadedFileSaveDirectory);
                }else{
                    uploadedFileSaveDirectory = PathKit.getWebRootPath() + File.separator + "upload" + File.separator;
                    Config.setUploadedFileSaveDirectory(uploadedFileSaveDirectory);
                }


				/*File file = new File(uploadedFileSaveDirectory);
                if (!file.exists())
					file.mkdirs();*/
            }
            OreillyCos.init(uploadedFileSaveDirectory, Config.getMaxDefaultPostSize(), Config.getGlobalCharset());
        }
    }


    public static List getConfigContextMappingController() {
        List<XMLElement> list = (List<XMLElement>) Config.getValue("Context.controller.mapping");
        if (list == null || list.size() == 0) {
            XMLMultiLoader xmlMultiLoader = ConfigLoader.loadContextMapping();
            List<XMLElement> includes = xmlMultiLoader.elements("framework.controllers.include");
            list = new ArrayList<XMLElement>();
            if (includes != null && includes.size() > 0) {
                for (XMLElement inc : includes) {
                    Mapx mapx = inc.getAttributes();
                    if (!StringUtil.isEmpty(mapx.getString("path"))) {
                        InputStream inputStream = Config.class.getClass().getResourceAsStream(mapx.getString("path"));
                        XMLMultiLoader incLoad = new XMLMultiLoader();
                        incLoad.load(inputStream);
                        list.addAll(incLoad.elements("framework.controllers.mapping"));
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
            list.addAll(xmlMultiLoader.elements("framework.controllers.mapping"));
            Config.setValue("Context.controller.mapping", list);
        }
        return list;
    }

    public static List getConfigContextMappingServices() {
        List<XMLElement> list = (List<XMLElement>) Config.getValue("Context.services.mapping");
        if (list == null || list.size() == 0) {
            XMLMultiLoader xmlMultiLoader = ConfigLoader.loadContextMapping();
            List<XMLElement> includes = xmlMultiLoader.elements("framework.services.include");
            list = new ArrayList<XMLElement>();
            if (includes != null && includes.size() > 0) {
                for (XMLElement inc : includes) {
                    Mapx mapx = inc.getAttributes();
                    if (!StringUtil.isEmpty(mapx.getString("path"))) {
                        InputStream inputStream = Config.class.getClass().getResourceAsStream(mapx.getString("path"));
                        XMLMultiLoader incLoad = new XMLMultiLoader();
                        incLoad.load(inputStream);
                        list.addAll(incLoad.elements("framework.controllers.mapping"));
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            list.addAll(xmlMultiLoader.elements("framework.services.mapping"));
            Config.setValue("Context.services.mapping", list);
        }
        return list;
    }

    public static List getConfigContextMappingRepository() {
        List<XMLElement> list = (List<XMLElement>) Config.getValue("Context.repositorys.mapping");
        if (list == null || list.size() == 0) {
            XMLMultiLoader xmlMultiLoader = ConfigLoader.loadContextMapping();
            List<XMLElement> includes = xmlMultiLoader.elements("framework.repositorys.include");
            list = new ArrayList<XMLElement>();
            if (includes != null && includes.size() > 0) {
                for (XMLElement inc : includes) {
                    Mapx mapx = inc.getAttributes();
                    if (!StringUtil.isEmpty(mapx.getString("path"))) {
                        InputStream inputStream = Config.class.getClass().getResourceAsStream(mapx.getString("path"));
                        XMLMultiLoader incLoad = new XMLMultiLoader();
                        incLoad.load(inputStream);
                        list.addAll(incLoad.elements("framework.repositorys.mapping"));
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            list.addAll(xmlMultiLoader.elements("framework.repositorys.mapping"));
            Config.setValue("Context.repositorys.mapping", list);
        }
        return list;
    }

    public static Repositorys getConfigContextMappingRepositorys() {
        Repositorys repositorys = (Repositorys) Config.getValue("Context.repositorys.mappingInfo");
        if (repositorys == null) {
            repositorys = new Repositorys();
            List list = getConfigContextMappingRepository();
            repositorys.setRepositorys(list);
            XMLMultiLoader xmlMultiLoader = ConfigLoader.loadContextMapping();
            List<XMLElement> res = xmlMultiLoader.elements("framework.repositorys");
            if(res!=null){
               repositorys.setBase(res.get(0));
            }
            Config.setValue("Context.repositorys.mappingInfo", repositorys);
        }
        return repositorys;
    }


    public static List getConfigExpandFilter() {
        List<XMLElement> list = (List<XMLElement>) Config.getValue("Context.repositorys.mapping");
        if (list == null || list.size() == 0) {
            XMLMultiLoader xmlMultiLoader = ConfigLoader.loadContextMapping();
            list = xmlMultiLoader.elements("framework.repositorys.mapping");
            Config.setValue("Context.repositorys.mapping", list);
        }
        return list;
    }

    public static void loadConfig() {
        configMap.remove("System.JavaVersion");
        ConfigLoader.reload();
        init();
    }

    public static class Repositorys {

        private XMLElement base;
        private List repositorys;

        public Repositorys() {
        }

        public Repositorys(XMLElement base, List repositorys) {
            this.base = base;
            this.repositorys = repositorys;
        }

        public XMLElement getBase() {
            return base;
        }

        public void setBase(XMLElement base) {
            this.base = base;
        }

        public List getRepositorys() {
            return repositorys;
        }

        public void setRepositorys(List repositorys) {
            this.repositorys = repositorys;
        }
    }

    public static Map<String, Object> getMap() {
        return configMap;
    }

    public static boolean isFrontDeploy() {
        return FrontDeploy;
    }

    public static String getExcelVersion() {
        String ev = getValue("App.ExcelVersion").toString();
        if (StringUtil.isEmpty(ev)) {
            ev = "2014";
        }
        return ev;
    }

    public static Object getValue(String configName) {
        init();
        return configMap.get(configName);
    }

    public static void setValue(String configName, Object configValue) {
        init();
        configMap.put(configName, configValue);
    }

    public static String getWEBINFPath() {
        String path = getPluginsClassesPath();
        if (path.indexOf("WEB-INF") > 0) {
            path = path.substring(0, path.lastIndexOf("WEB-INF") + 8);
        }
        return path;
    }

    public static String getPluginsClassesPath() {
        if (ClassesPath == null) {
            URL url = Config.class.getClassLoader().getResource("cc/ruixing/framework/tarn/core/Config.class");
            if (url == null) {
                System.err.println("Config.getClassesPath() failed!");
                return "";
            }
            try {
                String path = URLDecoder.decode(url.getPath(), getFileEncode());
                if ((System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) &&
                        (path.startsWith("/"))) {
                    path = path.substring(1);
                }

                if (path.startsWith("file:/"))
                    path = path.substring(6);
                else if (path.startsWith("jar:file:/")) {
                    path = path.substring(10);
                }
                if (path.indexOf(".jar!") > 0) {
                    path = path.substring(0, path.indexOf(".jar!"));
                }
                path = path.replace('\\', '/');
                path = path.substring(0, path.lastIndexOf("/") + 1);
                if (path.indexOf("WEB-INF") >= 0) {
                    path = path.substring(0, path.lastIndexOf("WEB-INF") + 7) + "/plugins/classes/";
                }
                if ((System.getProperty("os.name").toLowerCase().indexOf("windows") < 0) &&
                        (!path.startsWith("/"))) {
                    path = "/" + path;
                }

                ClassesPath = path;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return ClassesPath;
    }

    public static final Plugins getPlugins() {
        return plugins;
    }

    public static final Filters getFilters() {
        return filters;
    }

    public static String getPluginPath() {
        File f = new File(getPluginsClassesPath());
        String path = f.getParentFile().getAbsolutePath() + "/";
        return path;
    }

    public static String getContextRealPath() {
        if (configMap != null) {
            String str = (String) configMap.get("App.ContextRealPath");
            if (str != null) {
                return str;
            }
        }
        String path = getPluginsClassesPath();
        int index = path.indexOf("WEB-INF");
        if (index > 0) {
            path = path.substring(0, index);
        }
        return path;
    }

    public static String getContextPath() {
        if (ComplexDepolyMode) {
//            String path = (String) User.getValue("App.ContextPath");
            //todo
            String path = "";
            if (StringUtil.isEmpty(path)) {
                path = getValue("App.ContextPath").toString();
            }
            return path;
        }
        return getValue("App.ContextPath").toString();
    }

    private static void initProduct() {
        if (AppCode == null) {
            if (configMap.get("App.Code") != null) {
                AppCode = (String) configMap.get("App.Code");
                AppName = (String) configMap.get("App.Name");
            }
            if (AppCode == null) {
                AppCode = "@{Product.Code}";
                AppName = "@{Product.Name}";
            }
        }
    }

    public static String getAppCode() {
        initProduct();
        return AppCode;
    }

    public static String getAppName() {
        initProduct();
        return AppName;
    }

    public static boolean isDebugMode() {
        if (debugModeFlag == null) {
            debugModeFlag = Boolean.valueOf("true".equalsIgnoreCase(getValue("App.DebugMode").toString()));
        }
        return debugModeFlag.booleanValue();
    }

    public static String getContainerInfo() {
        return getValue("System.ContainerInfo").toString();
    }

    public static String getContainerVersion() {
        String str = getValue("System.ContainerInfo").toString();
        if (str.indexOf("/") > 0) {
            return str.substring(str.lastIndexOf("/") + 1);
        }
        return "0";
    }

    /**
     * Set the save directory for upload file. You can use PathUtil.getWebRootPath()
     * to get the web root path of this application, then create a path based on
     * web root path conveniently.
     */
    public static void setUploadedFileSaveDirectory(String uploadedFileSaveDirectoryP) {
        if (StringUtil.isEmpty(uploadedFileSaveDirectoryP))
            throw new IllegalArgumentException("uploadedFileSaveDirectory can not be blank");

        if (uploadedFileSaveDirectoryP.endsWith("/") || uploadedFileSaveDirectoryP.endsWith("\\"))
            uploadedFileSaveDirectory = uploadedFileSaveDirectoryP;
        else
            uploadedFileSaveDirectory = uploadedFileSaveDirectoryP + File.separator;
    }

    public static String getUploadedFileSaveDirectory() {
        return uploadedFileSaveDirectory;
    }

    public static String getLineSeparator() {
        return getValue("System.LineSeparator").toString();
    }

    public static String getFileSeparator() {
        return getValue("System.FileSeparator").toString();
    }

    public static String getFileEncode() {
        return System.getProperty("file.encoding");
    }

    public static int getLoginUserCount() {
        return LoginUserCount;
    }

    public static boolean isTomcat() {
        if (StringUtil.isEmpty(getContainerInfo())) {
            getJBossInfo();
        }
        return getContainerInfo().toLowerCase().indexOf("tomcat") >= 0;
    }

    public static void getJBossInfo() {
        String jboss = System.getProperty("jboss.home.dir");
        if (StringUtil.isNotEmpty(jboss))
            try {
                Class c = Class.forName("org.jboss.Version");
                Method m = c.getMethod("getInstance", null);
                Object o = m.invoke(null, null);
                m = c.getMethod("getMajor", null);
                Object major = m.invoke(o, null);
                m = c.getMethod("getMinor", null);
                Object minor = m.invoke(o, null);
                m = c.getMethod("getRevision", null);
                Object revision = m.invoke(o, null);
                m = c.getMethod("getTag", null);
                Object tag = m.invoke(o, null);
                configMap.put("System.ContainerInfo", "JBoss/" + major + "." + minor + "." + revision + "." + tag);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static boolean isJboss() {
        if (StringUtil.isEmpty(getContainerInfo())) {
            getJBossInfo();
        }
        return getContainerInfo().toLowerCase().indexOf("jboss") >= 0;
    }

    public static boolean isWeblogic() {
        return getContainerInfo().toLowerCase().indexOf("weblogic") >= 0;
    }

    public static boolean isWebSphere() {
        return getContainerInfo().toLowerCase().indexOf("websphere") >= 0;
    }

    public static boolean isComplexDepolyMode() {
        return ComplexDepolyMode;
    }

    public static String getLoginPage() {
        String str = (String) configMap.get("App.LoginPage");
        if (StringUtil.isNotEmpty(str)) {
            return str;
        }
        return "login.ftl";
    }

    public static String getGlobalCharset() {
        if (GlobalCharset == null) {
            ConfigLoader.load();
        }
        if (GlobalCharset == null) {
            GlobalCharset = "UTF-8";
        }
        return GlobalCharset;
    }

    public static int getMaxDefaultPostSize() {
        return DEFAULT_MAX_POST_SIZE;
    }

    public static boolean isPluginContext() {
        return isPluginContext;
    }

    public static void setPluginContext(boolean isPluginContext) {
        isPluginContext = isPluginContext;
    }

    public static void setFrontDeploy(boolean flag) {
        FrontDeploy = flag;
    }

    public static String getOSName() {
        return getValue("System.OSName").toString();
    }
}

