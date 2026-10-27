package cc.ruixing.framework.tarn.core.connection;

import cc.ruixing.framework.tarn.collection.RunMode;
import cc.ruixing.framework.tarn.core.Tarn;
import cc.ruixing.framework.tarn.util.FileUtil;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.ResourceUtil;
import cc.ruixing.framework.tarn.util.resource.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dfr on 14-4-18.
 */
public abstract class ConnectionKit {
    private static Map<String, Properties> sysConfs = null;

    static {
        init();
    }

    public static void init() {
        /***
         * 加载程序所依赖的必备文件，并存至 sysConfs
         * */
        if (sysConfs == null) sysConfs = new HashMap<String, Properties>();
        // Tarn 2.1+: Spring Boot 等场景允许只用 application.yml，dev.properties 缺失不再抛异常
        InputStream in = ConnectionKit.class.getClassLoader().getResourceAsStream("dev.properties");
        Properties props = new Properties();
        if (in == null) {
            LogUtil.warn("[Tarn] 未找到 dev.properties，配置改由 application.yml / 默认值提供");
            sysConfs.put("dev", props);
            return;
        }
        try {
            props.load(in);
            sysConfs.put("dev", props);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static String getDevConf(String key) {
        // Tarn 2.0+: application.yml 优先，未命中时回退 dev.properties（老配置兼容）
        String yamlVal = cc.ruixing.framework.tarn.config.YamlConfigKit.getByLegacyKey(key);
        if (yamlVal != null && !yamlVal.isEmpty()) {
            return yamlVal;
        }
        // YAML 中也可能直接用老 key 书写
        String directYaml = cc.ruixing.framework.tarn.config.YamlConfigKit.get(key);
        if (directYaml != null && !directYaml.isEmpty()) {
            return directYaml;
        }
        return getConfVal(key, "dev");
    }

    public static Map<String, Properties> getSysConfs() {
        if (sysConfs == null) init();
//        System.out.println("sysConfs:" + sysConfs);
//        System.out.println("RunMode.getInstance().isJarStatus();:" + RunMode.getInstance().isJarStatus());
        return sysConfs;
    }

    public static String getConfVal(String key, String fileName) {
        // Spring Boot 等场景可能没有 dev.properties，此处返回 null 而不是抛 NPE
        Properties props = getSysConfs().get(fileName);
        return props == null ? null : props.getProperty(key);
    }
}
