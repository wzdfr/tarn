package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.util.text.FastStringBuilder;

public final class XMLText extends XMLNode {
    String text;

    public XMLText(String text) {
        this.text = text;
    }

    public void toString(String prefix, FastStringBuilder sb) {
        if (this.text != null)
            encode(this.text, sb);
    }

    public String getText() {
        return this.text;
    }

    public int getType() {
        return 2;
    }

    void repack() {
        this.text = new String(this.text.toCharArray());
    }

    public void setText(String text) {
        this.text = text;
    }
}

