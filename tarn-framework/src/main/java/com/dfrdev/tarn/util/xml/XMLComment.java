package com.dfrdev.tarn.util.xml;

import com.dfrdev.tarn.util.text.FastStringBuilder;

public class XMLComment extends XMLNode {
    String comment;

    public XMLComment(String comment) {
        this.comment = comment;
    }

    public void toString(String prefix, FastStringBuilder sb) {
        sb.append(prefix).append("<!--").append(this.comment).append("-->");
    }

    public String getText() {
        return "";
    }

    public int getType() {
        return 5;
    }

    void repack() {
        this.comment = new String(this.comment.toCharArray());
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
