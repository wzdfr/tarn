package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.util.io.FileUtil;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import com.dfrdev.tarn.util.LogUtil;

public class XMLMultiLoader {
    private XMLElement root = new XMLElement("_ROOT");

    public void load(String path) {
        File f = new File(path);
        load(f);

    }

    public void load(File f) {
        if (!f.exists()) {
            return;
        }
        if (f.isFile()) {
            loadOneFile(f);
        } else {
            File[] fs = f.listFiles();
            if ((fs == null) || (fs.length == 0)) {
                return;
            }
            for (File element : fs) {
                f = element;
                if ((f.isFile()) && ((f.getName().toLowerCase().endsWith(".xml")) || (f.getName().toLowerCase().endsWith(".plugin"))))
                    loadOneFile(f);
            }
        }
    }

    public void load(InputStream is) {
        loadOneFile(is);
    }

    private void loadOneFile(File f) {
        loadOneFile(FileUtil.readText(f));
    }

    public void clear() {
        this.root = new XMLElement("_ROOT");
    }

    private void loadOneFile(InputStream is) {
        XMLParser parser = new XMLParser(is);
        try {
            parser.parse();
            XMLElement singleRoot = parser.getDocument().getRoot();
            singleRoot.setParent(this.root);
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
    }

    private void loadOneFile(String xml) {
        XMLParser parser = new XMLParser(xml);
        parser.parse();
        XMLElement singleRoot = parser.getDocument().getRoot();
        singleRoot.setParent(this.root);
    }

    public XMLElement getRoot() {
        return this.root;
    }

    public XMLElement elements(String path, String attrName, String attrValue) {
        List list = this.root.elementsByAttribute(path, attrName, attrValue);
        if ((list == null) || (list.size() == 0)) {
            return null;
        }
        return (XMLElement) list.get(0);
    }

    public List<XMLElement> elements(String path) {
        return this.root.elements(path);
    }
}

