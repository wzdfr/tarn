package cc.ruixing.framework.tarn.util.xml;


import cc.ruixing.framework.tarn.util.FastStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class XMLDocument {
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_VERSION = "1.0";
    private XMLElement root;
    private List<XMLNode> children = new ArrayList(2);
    private String encoding = "UTF-8";
    private String version = "1.0";
    private String docType;

    public List<XMLElement> elements(String path) {
        String prefix = path;
        int index = path.indexOf(".");
        if (index > 0) {
            prefix = path.substring(0, index);
            path = path.substring(index + 1);
        }
        if ((!"*".equals(prefix)) && (!this.root.getQName().equals(prefix))) {
            return new ArrayList();
        }
        return this.root.elements(path);
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getDocType() {
        return this.docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public XMLElement createRoot(String QName) {
        this.root = new XMLElement(QName);
        this.children.add(this.root);
        return this.root;
    }

    public void addComment(String comment) {
        this.children.add(new XMLComment(comment));
    }

    public void addInstruction(String instruction) {
        this.children.add(new XMLInstruction(instruction));
    }

    public void setRoot(XMLElement root) {
        this.root = root;
        this.children.add(root);
    }

    public XMLElement getRoot() {
        return this.root;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String asXML() {
        return toString();
    }

    public String toString() {
        FastStringBuilder sb = new FastStringBuilder();
        sb.append("<?xml version=\"" + this.version + "\" encoding=\"" + this.encoding + "\"?>");
        if (this.docType != null) {
            sb.append("\n<!DOCTYPE ");
            sb.append(this.docType);
            sb.append(">");
        }
        for (XMLNode node : this.children) {
            sb.append("\n");
            node.toString("", sb);
        }
        return sb.toStringAndClose();
    }

    public XMLElement addElement(String path, String QName) {
        String[] arr = path.split("\\.");
        if (!this.root.getQName().equals(arr[0])) {
            return null;
        }
        XMLElement current = this.root;
        for (int i = 1; i < arr.length; i++) {
            String segment = arr[i];
            XMLElement ele = current.element(segment);
            if (ele == null) {
                ele = current.addElement(segment);
            }
            current = ele;
        }
        return current.addElement(QName);
    }

    void repack() {
        if (this.docType != null) {
            this.docType = new String(this.docType.toCharArray());
        }
        this.version = new String(this.version.toCharArray());
        this.encoding = new String(this.encoding.toCharArray());
        for (XMLNode node : this.children)
            node.repack();
    }
}
