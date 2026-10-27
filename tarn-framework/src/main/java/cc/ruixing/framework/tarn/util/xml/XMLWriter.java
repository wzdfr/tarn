package cc.ruixing.framework.tarn.util.xml;


import cc.ruixing.framework.tarn.util.FileUtil;

import java.io.File;

public class XMLWriter {
    public static void writeTo(XMLDocument doc, File f) {
        FileUtil.writeText(f.getAbsolutePath(), doc.toString(), doc.getEncoding());
    }
    public static void writeTo(XMLDocument doc, File f,boolean bomFlag) {
        FileUtil.writeText(f.getAbsolutePath(), doc.toString(), doc.getEncoding(), bomFlag);
    }
}

