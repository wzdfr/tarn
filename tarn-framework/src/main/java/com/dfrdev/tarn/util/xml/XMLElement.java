package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.util.FastStringBuilder;
import com.dfrdev.tarn.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public final class XMLElement extends XMLNode {
    int lineNumber;
    int startCharIndex;
    int endCharIndex;
    Mapx<String, String> attributes = null;
    String QName;
    List<XMLNode> children;
    int pos;

    public XMLElement(String QName) {
        this.QName = QName;
    }

    public XMLElement addElement(String QName) {
        XMLElement node = new XMLElement(QName);
        node.setParent(this);
        return node;
    }

    public void addText(String text) {
        XMLText node = new XMLText(text);
        node.setParent(this);
    }

    public void addCDATA(String text) {
        XMLCDATA node = new XMLCDATA(text);
        node.setParent(this);
    }

    public void addComment(String comment) {
        XMLComment node = new XMLComment(comment);
        node.setParent(this);
    }

    public void addInstruction(String instruction) {
        XMLInstruction node = new XMLInstruction(instruction);
        node.setParent(this);
    }

    public XMLElement getParent() {
        return this.parent;
    }

    public Mapx<String, String> getAttributes() {
        return attributes();
    }

    public Mapx<String, String> attributes() {
        if (this.attributes == null) {
            this.attributes = new Mapx();
        }
        return this.attributes;
    }

    public String attributeValue(String attrName) {
        if (this.attributes == null) {
            return null;
        }
        return (String) this.attributes.get(attrName);
    }

    public void addAttribute(String attrName, String attrValue) {
        if (this.attributes == null) {
            this.attributes = new Mapx();
        }
        this.attributes.put(attrName, attrValue);
    }

    public String getQName() {
        return this.QName;
    }

    public int getPosition() {
        return this.pos;
    }

    public void setText(String text) {
        if (this.children == null)
            this.children = new ArrayList();
        else {
            this.children.clear();
        }
        addText(text);
    }

    public String getText() {
        if (this.children == null) {
            return "";
        }
        FastStringBuilder sb = new FastStringBuilder();
        for (XMLNode node : this.children) {
            if (ObjectUtil.in(new Object[]{Integer.valueOf(node.getType()), Integer.valueOf(2), Integer.valueOf(1)})) {
                sb.append(node.getText());
            }
        }
        return sb.toStringAndClose();
    }

    public List<XMLElement> elements() {
        List elements = new ArrayList();
        if (this.children == null) {
            return elements;
        }
        for (XMLNode node : this.children) {
            if (node.getType() == 3) {
                elements.add((XMLElement) node);
            }
        }
        return elements;
    }

    public List<XMLElement> getElements() {
        return elements();
    }

    private List<XMLElement> elements(List<XMLElement> list, String QName) {
        List result = new ArrayList();
        Iterator localIterator2 = null;
        for (Iterator localIterator1 = list.iterator(); localIterator1.hasNext(); localIterator2.hasNext()
                ) {
            XMLElement node = (XMLElement) localIterator1.next();
            localIterator2 = node.getChildren().iterator();
//            XMLNode child =null;
            while (localIterator2.hasNext()) {
                XMLNode child = (XMLNode) localIterator2.next();
                if (child.getType() == 3) {
                    XMLElement ele = (XMLElement) child;
                    if (("*".equals(QName)) || (ele.getQName().equalsIgnoreCase(QName))) {
                        result.add(ele);
                    }
                }
            }


        }
        return result;
    }

    public List<XMLElement> elements(String path) {
        String[] arr = path == null ? new String[0] : path.split("\\.");
        List list = new ArrayList();
        list.add(this);
        for (String element2 : arr) {
            list = elements(list, element2);
            if ((list == null) || (list.size() == 0)) {
                break;
            }
        }
        return list;
    }

    public String elementText(String path) {
        XMLElement child = element(path);
        if (child == null) {
            return "";
        }
        return child.getText();
    }

    public XMLElement element(String path) {
        List nodes = elements(path);
        return (nodes == null) || (nodes.size() == 0) ? null : (XMLElement) nodes.get(0);
    }

    public List<XMLElement> elementsByAttribute(String path, String attrName, String attrValue) {
        List<XMLElement> nodes = elements(path);
        if ((nodes == null) || (nodes.size() == 0)) {
            return null;
        }
        List result = new ArrayList();
        for (XMLElement n : nodes) {
            if (attrValue == null) {
                if (n.getAttributes().get(attrName) == null)
                    result.add(n);
            } else if (attrValue.equals(n.getAttributes().get(attrName))) {
                result.add(n);
            }
        }
        return result;
    }

    public String toString() {
        FastStringBuilder sb = new FastStringBuilder();
        toString("", sb);
        return sb.toStringAndClose();
    }

    public void toString(String prefix, FastStringBuilder sb) {
        sb.append(prefix);
        sb.append("<");
        sb.append(this.QName);
        if (this.attributes != null) {
            for (Entry entry : this.attributes.entrySet()) {
                sb.append(" ");
                sb.append((String) entry.getKey());
                sb.append("=\"");
                encode((String) entry.getValue(), sb);
                sb.append("\"");
            }
        }
        if ((this.children == null) || (this.children.size() == 0)) {
            sb.append(" />");
        } else {
            sb.append(">");
            if ((this.children.size() == 1) && (((XMLNode) this.children.get(0)).getType() == 2)) {
                ((XMLNode) this.children.get(0)).toString("", sb);
                sb.append("</");
                sb.append(this.QName);
                sb.append(">");
            } else {
                for (XMLNode child : this.children) {
                    sb.append("\n");
                    child.toString(prefix + "\t", sb);
                }
                sb.append("\n");
                sb.append(prefix);
                sb.append("</");
                sb.append(this.QName);
                sb.append(">");
            }
        }
    }

    public int getType() {
        return 3;
    }

    public boolean remove(XMLElement ele) {
        for (int i = 0; (this.children != null) && (i < this.children.size()); i++) {
            if (this.children.get(i) == ele) {
                this.children.remove(i);
                return true;
            }
        }
        return false;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public XMLElement addElement(String path, String QName) {
        String[] arr = path.split("\\.");
        XMLElement current = this;
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

    public List<XMLNode> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList(4);
        }
        return this.children;
    }

    public String getInnerXML() {
        FastStringBuilder sb = new FastStringBuilder();
        if ((this.children != null) && (this.children.size() > 0)) {
            for (XMLNode node : this.children)
                sb.append(node.getXML());
        } else {
            return "";
        }
        return sb.toStringAndClose();
    }

    public int getStartCharIndex() {
        return this.startCharIndex;
    }

    public void setStartCharIndex(int startCharIndex) {
        this.startCharIndex = startCharIndex;
    }

    public int getEndCharIndex() {
        return this.endCharIndex;
    }

    public void setEndCharIndex(int endCharIndex) {
        this.endCharIndex = endCharIndex;
    }

    void repack() {
        this.QName = new String(this.QName.toCharArray());
        Mapx map = new Mapx();
        if (this.attributes != null) {
            for (Entry entry : this.attributes.entrySet()) {
                String k = (String) entry.getKey();
                String v = (String) entry.getValue();
                v = new String(v.toCharArray());
                k = new String(k.toCharArray());
                map.put(k, v);
            }
            this.attributes = map;
        }
        if (this.children != null)
            for (XMLNode node : this.children)
                node.repack();
    }
}

