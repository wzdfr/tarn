package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.util.FastStringBuilder;

public final class XMLCDATA extends XMLNode {
    String text;

    public XMLCDATA(String text) {
        this.text = text;
    }

    public void toString(String prefix, FastStringBuilder sb) {
        sb.append(prefix).append("<![CDATA[").append(this.text == null ? "" : this.text).append("]]>");
    }

    public String getText() {
        return this.text;
    }

    public int getType() {
        return 1;
    }

    void repack() {
        this.text = new String(this.text.toCharArray());
    }

    public void setText(String text) {
        this.text = text;
    }
}