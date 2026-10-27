package cc.ruixing.framework.tarn.aop.inject;

import cc.ruixing.framework.tarn.aop.service.LoadAOP;
import cc.ruixing.framework.tarn.collection.Mapx;
import cc.ruixing.framework.tarn.collection.RunMode;
import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.util.ClassUtil;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.PackageUtil;
import cc.ruixing.framework.tarn.util.StringUtil;
import cc.ruixing.framework.tarn.util.TextKit;
import cc.ruixing.framework.tarn.util.xml.XMLDocument;
import cc.ruixing.framework.tarn.util.xml.XMLElement;
import cc.ruixing.framework.tarn.util.xml.XMLWriter;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * 组件扫描器（Tarn 2.1）：把「读 context-mapping.xml → 扫描包/jar → 交给 LoadAOP 注册 → 写只读清单」的模板固化下来。
 * <p>
 * {@code Inject}（Controller）、{@code ServiceInject}、{@code RepositoryInject} 原先各自复制了这段逻辑，
 * 三份实现只在根节点名与 LoadAOP 实现上不同，现统一由此类承载。
 *
 * @author dfr
 * @since 2.1
 */
public final class ComponentScanner {

    private ComponentScanner() {
    }

    /**
     * 按 context-mapping.xml 的包声明扫描组件。
     *
     * @param reportTitle 清单文件注释首行（说明扫的是什么）
     * @param packages    context-mapping.xml 中的 package 节点集合
     * @param loader      具体的扫描注册实现（{@code ControllerAOP} / {@code ServiceAOP} / {@code RepositoryAOP}）
     * @param containerTag 清单中承载 package 节点的父元素名（controllers / packages）
     * @return 只读清单文档（供 {@link #write(XMLDocument, String)} 落盘）
     */
    public static XMLDocument scan(String reportTitle, List<XMLElement> packages, LoadAOP loader,
                                   String containerTag) {
        XMLDocument doc = new XMLDocument();
        XMLElement root = doc.createRoot("framework");
        doc.addComment("程序自动生成的文件，该文件只是只读的。\r\n"
                + "更改此文件不会对应用程序造成任何影响，它存在的意义只是为了让你能够了解到目前你的应用部署"
                + reportTitle + "清单\nTime:" + TextKit.parseDate(null, null, true));
        XMLElement container = root.addElement(containerTag);
        if (packages == null || packages.isEmpty()) {
            return doc;
        }
        for (XMLElement xml : packages) {
            try {
                scanPackage(xml, loader, container);
            } catch (Throwable e) {
                // 单个包扫描失败不应阻断框架启动（老实现会直接抛出导致启动失败）
                LogUtil.error("[Tarn] 组件包扫描失败 " + xml.getAttributes().getString("package")
                        + ": " + e.getMessage());
            }
        }
        return doc;
    }

    private static void scanPackage(XMLElement xml, LoadAOP loader, XMLElement container) throws Exception {
        Mapx mapx = xml.getAttributes();
        String packageRaw = mapx.getString("package");
        if (StringUtil.isEmpty(packageRaw)) {
            return;
        }
        String remark = mapx.getString("remark");
        String author = mapx.getString("author");
        XMLElement packageNode = container.addElement("package");
        packageNode.addAttribute("package", packageRaw.replace('.', '/'));
        packageNode.addAttribute("name", mapx.getString("name"));
        if (StringUtil.isNotEmpty(remark)) {
            packageNode.addAttribute("remark", remark);
        }
        packageNode.addAttribute("author", StringUtil.isNotEmpty(author) ? author : System.getProperty("user.name"));

        if (mapx.getBoolean("isJar")) {
            String path = Config.getWEBINFPath() + mapx.getString("jarName");
            LogUtil.info("component Load jar directory:" + path);
            List<String> classes = PackageUtil.getClassInPackageByJar(packageRaw, path);
            loader.load(classes, xml, packageNode);
        } else {
            Set<Class<?>> classes = ClassUtil.scanPackage(packageRaw.replace('/', '.'));
            loader.load(classes, xml, packageNode);
        }
    }

    /**
     * 写出只读清单：jar 运行模式落到应用根目录，否则落到 classpath 根目录。
     */
    public static void write(XMLDocument doc, String fileName) {
        try {
            File target = RunMode.getInstance().isJarStatus()
                    ? new File(Config.getContextRealPath() + fileName)
                    : new File(classpathRoot() + fileName);
            XMLWriter.writeTo(doc, target, true);
        } catch (Exception e) {
            // 清单仅用于人工排查，写失败不影响运行
            LogUtil.warn("[Tarn] 组件清单写出失败 " + fileName + ": " + e.getMessage());
        }
    }

    private static String classpathRoot() {
        java.net.URL url = ComponentScanner.class.getResource("/");
        return url == null ? "" : url.getPath();
    }
}
