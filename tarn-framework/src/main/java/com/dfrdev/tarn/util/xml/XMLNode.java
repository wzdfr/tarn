package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.util.text.FastStringBuilder;
import com.dfrdev.tarn.util.text.StringUtil;

import java.util.ArrayList;

public abstract class XMLNode {
    public static final int CDATA = 1;
    public static final int TEXT = 2;
    public static final int ELEMENT = 3;
    public static final int INSTRUCTION = 4;
    public static final int COMMENT = 5;
    XMLElement parent;

    public abstract void toString(String paramString, FastStringBuilder paramFastStringBuilder);

    public abstract String getText();

    public abstract int getType();

    abstract void repack();

    public void setParent(XMLElement parent) {
        this.parent = parent;
        if (parent.children == null) {
            parent.children = new ArrayList(4);
        }
        this.parent.children.add(this);
    }

    protected void encode(String value, FastStringBuilder sb) {
        if (StringUtil.isEmpty(value)) {
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<')
                sb.append("&lt;");
            else if (c == '>')
                sb.append("&gt;");
            else if (c == '"')
                sb.append("&quot;");
            else if (c == '\'')
                sb.append("&apos;");
            else if (c == '&')
                sb.append("&amp;");
            else
                sb.append(c);
        }
    }

    public XMLElement getParent() {
        return this.parent;
    }

    public String getXML() {
        return toString();
    }

    public String toString() {
        FastStringBuilder sb = new FastStringBuilder();
        toString("", sb);
        return sb.toStringAndClose();
    }
}
