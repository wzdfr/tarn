package cc.ruixing.framework.tarn.core;


import cc.ruixing.framework.tarn.collection.RunMode;
import cc.ruixing.framework.tarn.util.FileUtil;
import cc.ruixing.framework.tarn.util.StringUtil;
import cc.ruixing.framework.tarn.util.xml.XMLElement;
import cc.ruixing.framework.tarn.util.xml.XMLMultiLoader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigLoader {
    private static boolean loaded = false;

    private static ReentrantLock lock = new ReentrantLock();

    private static XMLMultiLoader loader = new XMLMultiLoader();
    private static XMLMultiLoader loaderContext = null;

    public static void load() {
        try {
            String path = Config.getPluginPath() + "classes/";
            load(path);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void load(String path) {
        if (!loaded) {
            lock.lock();
            try {
                if (!loaded) {
                    loader.clear();
                    File f = new File(path);
                    if (!f.exists()) {
                        return;
                    }
                    String file = path + "charset.config";
                    if (new File(file).exists()) {
                        String txt = FileUtil.readText(file, "UTF-8");
                        Map map = StringUtil.splitToMapx(txt, "\n", "=");
                        Config.GlobalCharset = "GBK".equalsIgnoreCase(map.get("global").toString()) ? "GBK" : "UTF-8";
                    } else {
                        throw new TarnException("File charset.config not found!");
                    }

                    loader.load(path);
                    XMLElement data = loader.elements("framework.application.config", "name", "ComplexDeployMode");
                    Config.ComplexDepolyMode = (data != null) && ("true".equals(data.getText()));
                }
                loaded = true;
            } finally {
                lock.unlock();
            }
            lock.unlock();
        }
    }

    public static XMLMultiLoader loadContextMapping() {
        if (loaderContext == null) {
            loaderContext = new XMLMultiLoader();
            // Tarn 2.1+: 优先从 classpath 读取，兼容 Spring Boot 可执行 jar（无解压后的目录）
            java.io.InputStream in = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("context-mapping.xml");
            if (in != null) {
                try {
                    loaderContext.load(in);
                } finally {
                    try {
                        in.close();
                    } catch (java.io.IOException ignore) {
                    }
                }
            } else if (RunMode.getInstance().isJarStatus()) {
                loaderContext.load(ConfigLoader.class.getClassLoader().getResourceAsStream("context-mapping.xml"));
            } else {
                String path = ConfigLoader.class.getResource("/").getPath() + "context-mapping.xml";
                loaderContext.load(path);
            }
        }
        return loaderContext;
    }

    public static void reload() {
        loaded = false;
        load();
    }

    public static List<XMLElement> getElements(String path) {
        return loader.elements(path);
    }
}

