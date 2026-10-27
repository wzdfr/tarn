package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.util.FileUtil;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class XMLParser {
    XMLDocument doc = null;
    XMLElement current = null;
    String xml;
    char[] cs;
    String tag;
    String cdata;
    String text;
    List<Integer> lineNumList = new ArrayList(128);

    public XMLParser(String xml) {
        this.xml = xml;
    }

    public XMLParser(InputStream is) {
        byte[] bs = FileUtil.readByte(is);
        try {
            this.xml = new String(bs, "UTF-8");
            this.cs = this.xml.toCharArray();
            int start = indexOf(this.cs, "encoding=", 0);
            if (start > 0) {
                start += 9;
                boolean isFinalQuote = true;
                for (int i = start; i < this.xml.length(); i++) {
                    char chr = this.xml.charAt(i);
                    if (Character.isSpaceChar(chr)) {
                        start++;
                    } else if ((chr == '\'') || (chr == '"')) {
                        start = i + 1;
                        isFinalQuote = chr == '"';
                        break;
                    }
                }

                int end = isFinalQuote ? indexOf(this.cs, '"', start) : indexOf(this.cs, '\'', start);
                if (end > 0) {
                    String encoding = this.xml.substring(start, end).trim();
                    if (!encoding.equalsIgnoreCase("UTF-8"))
                        this.xml = new String(bs, encoding);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new XMLParseException(e);
        }
    }

    public XMLDocument parse() {
        this.doc = new XMLDocument();
        this.cs = this.xml.toCharArray();
        this.lineNumList.clear();
        int end = expect(this.cs, 0, "<?xml");
        int pos = 0;
        if (end > 0) {
            pos = end;
            while (true) {
                ElementAttribute ea = expectAttribute(this.cs, pos, this.lineNumList);
                if (ea == null) {
                    break;
                }
                if (ea.Name.equals("version")) {
                    this.doc.setVersion(ea.Value);
                }
                if (ea.Name.equals("encoding")) {
                    this.doc.setEncoding(ea.Value);
                }
                pos = ea.EndCharIndex;
            }
            if ((end = expect(this.cs, pos, "?>")) < 0) {
                throw new XMLParseException("Invalid xml prolog!");
            }
            pos = end;
        }
        end = expectOthers(pos);
        pos = end > 0 ? end : pos;
        end = expect(this.cs, pos, "<!DOCTYPE");
        if (end > 0) {
            pos = end;
            end = expectGT(pos);
            if (end < 0) {
                throw new XMLParseException("DOCTYPE not complete correctly!");
            }
            this.doc.setDocType(this.xml.substring(pos, end - 1));
            pos = end;
        }
        end = expectOthers(pos);
        pos = end > 0 ? end : pos;
        end = expectElement(pos);
        if (end > 0) {
            pos = end;
        }
        expectOthers(pos);
        if (this.doc.getRoot() == null) {
            throw new XMLParseException("No root element in XML document!");
        }
        this.doc.repack();
        return this.doc;
    }

    private int expectElement(int start) {
        int index = expect(this.cs, start, "<");
        if (index < 0) {
            return -1;
        }
        start = index;
        for (int i = start; i < this.cs.length; i++) {
            char c = this.cs[i];
            if ((isSpace(c)) || (c == '>')) {
                index = i;
                break;
            }
        }
        this.tag = this.xml.substring(start, index);
        if (this.tag.charAt(this.tag.length() - 1) == '/') {
            this.tag = this.tag.substring(0, this.tag.length() - 1);
            index--;
        }
        if ((index == start) || (isInvalidName(this.cs, start, index))) {
            throw new XMLParseException("Element's name is invalid:" + this.tag + ", line number is " + getLineNum(this.lineNumList, this.cs, start));
        }

        XMLElement ele = new XMLElement(this.tag);
        ele.setLineNumber(getLineNum(this.lineNumList, this.cs, start));
        ele.setStartCharIndex(start - 1);
        if (this.current == null)
            this.doc.setRoot(ele);
        else {
            ele.setParent(this.current);
        }
        this.current = ele;
        start = index;
        while (true) {
            ElementAttribute ea = expectAttribute(this.cs, start, this.lineNumList);
            if (ea == null) {
                break;
            }
            ele.addAttribute(ea.Name, ea.Value);
            start = ea.EndCharIndex;
        }
        if ((index = expect(this.cs, start, "/>")) > 0) {
            this.current = this.current.getParent();
            return index;
        }
        if ((index = expect(this.cs, start, ">")) < 0) {
            throw new XMLParseException("Element not complete correctly:" + this.tag + ", line number is " + getLineNum(this.lineNumList, this.cs, start));
        }

        while (expect(this.cs, index, "</") <= 0) {
            int i = -1;
            i = expectOthers(index);
            if (i > 0) {
                index = i;
            } else {
                i = expectText(index);
                if (i > 0) {
                    this.current.addText(this.text);
                    index = i;
                } else {
                    i = expectCDATA(index);
                    if (i > 0) {
                        this.current.addCDATA(this.cdata);
                        index = i;
                    } else {
                        i = expectElement(index);
                        if (i <= 0) break;
                        index = i;
                    }
                }
            }
        }
        start = index;

        index = expect(this.cs, start, "</" + ele.QName);
        if (index < 0) {
            throw new XMLParseException("Element not complete correctly:" + ele.QName + ", line number is " +
                    getLineNum(this.lineNumList, this.cs, start));
        }
        start = ignoreSpace(this.cs, index);
        index = expect(this.cs, start, ">");
        if (index < 0) {
            throw new XMLParseException("Element not complete correctly:" + ele.QName + ", line number is " +
                    getLineNum(this.lineNumList, this.cs, start));
        }

        this.current.setEndCharIndex(index);
        this.current = this.current.getParent();
        return index;
    }

    public static boolean isInvalidName(char[] cs, int start, int end) {
        char first = cs[start];
        if (isSpace(first)) {
            return true;
        }
        if (Character.isDigit(first)) {
            return true;
        }
        if ((first == ':') || (first == '.') || (first == '-')) {
            return true;
        }
        for (int i = start; i < end; i++) {
            if (isInvalidChar(cs[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInvalidChar(char c) {
        switch (c) {
            case '!':
            case '"':
            case '#':
            case '$':
            case '%':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case '/':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '`':
            case '{':
            case '|':
            case '}':
            case '~':
                return true;
        }
        return false;
    }

    private int expectText(int start) {
        int i = indexOf(this.cs, '<', start);
        if (i < 0) {
            throw new XMLParseException("Element not complete correctly:" + this.current.QName + ", line number is " +
                    getLineNum(this.lineNumList, this.cs, start));
        }
        if (i == start) {
            return -1;
        }
        String v = this.xml.substring(start, i).trim();
        if (v.length() == 0) {
            return -1;
        }
        this.text = decode(v);
        return i;
    }

    private int expectCDATA(int start) {
        int i = expect(this.cs, start, "<![CDATA[");
        if (i < 0) {
            return -1;
        }
        start = i;
        i = indexOf(this.cs, "]]>", start);
        if (i < 0) {
            throw new XMLParseException("CDATA not end correctly:" + this.current.QName + ", line number is " +
                    getLineNum(this.lineNumList, this.cs, start));
        }
        this.cdata = this.xml.substring(start, i);
        return i + 3;
    }

    public static ElementAttribute expectAttribute(char[] cs, int start, List<Integer> lineNumList) {
        start = ignoreSpace(cs, start);
        if ((cs[start] == '>') || (cs[start] == '/') || (cs[start] == '?')) {
            return null;
        }
        String attrName = null;
        String attrValue = null;
        int index = -1;
        for (int i = start; i < cs.length; i++) {
            if (cs[i] == '=') {
                index = i;
                break;
            }
            if (isSpace(cs[i])) {
                i = ignoreSpace(cs, i + 1);
                if (cs[i] == '=') {
                    index = i;
                    break;
                }
                return null;
            }

        }

        String name = new String(cs, start, index - start).trim();
        if (isInvalidName(cs, start, start + name.length())) {
            throw new XMLParseException("XML attribute name is invalid: [" + name + "], line number is " +
                    getLineNum(lineNumList, cs, start));
        }
        start = index + 1;
        start = ignoreSpace(cs, start);
        char c = cs[start];
        if ((c != '"') && (c != '\'')) {
            throw new XMLParseException("XML attribute value not starts with \" or ', line number is " +
                    getLineNum(lineNumList, cs, start));
        }
        start++;
        for (int i = start; i < cs.length; i++) {
            if (cs[i] == c) {
                index = i;
                break;
            }
            if ((cs[i] == '<') || (cs[i] == '\n')) {
                throw new XMLParseException("XML attribute value should not contains \\n and <, line number is " +
                        getLineNum(lineNumList, cs, start));
            }
        }
        String value = new String(cs, start, index - start);
        value = decode(value);
        attrName = name;
        attrValue = value;
        return new ElementAttribute(index + 1, attrName, attrValue);
    }

    public static String decode(String value) {
        char[] buf = new char[value.length()];
        int j = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                throw new XMLParseException("XML attribute value is invalid: [" + value + "]");
            }
            if (c == '&') {
                if (value.startsWith("&quot;", i)) {
                    buf[(j++)] = '"';
                    i += 5;
                } else if (value.startsWith("&amp;", i)) {
                    buf[(j++)] = '&';
                    i += 4;
                } else if (value.startsWith("&lt;", i)) {
                    buf[(j++)] = '<';
                    i += 3;
                } else if (value.startsWith("&gt;", i)) {
                    buf[(j++)] = '>';
                    i += 3;
                } else if (value.startsWith("&apos;", i)) {
                    buf[(j++)] = '\'';
                    i += 5;
                } else if (value.charAt(i + 1) == '#') {
                    int k = i + 2;
                    boolean flag = false;
                    int radix = 10;
                    if ((value.charAt(k) == 'x') || (value.charAt(k) == 'X')) {
                        radix = 16;
                        k++;
                    }
                    for (; (k < i + 9) && (k < value.length()); k++) {
                        if (value.charAt(k) == ';') {
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        char ch = (char) Integer.parseInt(value.substring(radix == 10 ? i + 2 : i + 3, k), radix);
                        buf[(j++)] = ch;
                        i += k;
                    } else {
                        throw new XMLParseException("Attribute value or element body contains not supported entity: [" +
                                value.substring(i, i + 10 > value.length() ? value.length() : i + 10) + "]");
                    }
                } else {
                    throw new XMLParseException("Attribute value or element body contains not supported entity: [" +
                            value.substring(i, i + 10 > value.length() ? value.length() : i + 10) + "]");
                }
            } else buf[(j++)] = c;
        }

        return new String(buf, 0, j);
    }

    private int expectOthers(int start) {
        int end = -1;
        int old = start;
        while (true) {
            end = expect(this.cs, start, "<!--");
            if (end < 0) {
                end = expect(this.cs, start, "<?");
                if (end < 0) {
                    break;
                }
                start = end;
                end = indexOf(this.cs, "?>", start);
                if (end < 0) {
                    throw new XMLParseException("Processing instruction not complete correctly, line number is " +
                            getLineNum(this.lineNumList, this.cs, start));
                }
                String instruction = this.xml.substring(start, end).trim();
                if (instruction.indexOf('?') >= 0) {
                    throw new XMLParseException("Processing instruction contains illegal character:[?]");
                }
                if (instruction.indexOf('>') >= 0) {
                    throw new XMLParseException("Processing instruction contains illegal character:[>]");
                }
                if (this.current == null)
                    this.doc.addInstruction(instruction);
                else {
                    this.current.addInstruction(instruction);
                }
                start = end + 2;
            } else {
                start = end;
                end = indexOf(this.cs, "--", start);
                if (end < 0) {
                    throw new XMLParseException("Comment not complete correctly, line number is " + getLineNum(this.lineNumList, this.cs, start));
                }
                end = expect(this.cs, end + 2, ">");
                if (end < 0) {
                    throw new XMLParseException("Comment not complete correctly,  line number is " + getLineNum(this.lineNumList, this.cs, start));
                }
                String comment = this.xml.substring(start, end - 3);
                if (this.current == null)
                    this.doc.addComment(comment);
                else {
                    this.current.addComment(comment);
                }
                start = end;
            }
        }
        if (old == start) {
            return -1;
        }
        return start;
    }

    private int expectGT(int start) {
        int index = indexOf(this.cs, '>', start);
        if (index < 0) {
            return -1;
        }
        for (int i = start; i < index; i++) {
            if (this.cs[i] == '<') {
                return -1;
            }
        }
        return index + 1;
    }

    public static int ignoreSpace(char[] cs, int start) {
        for (int i = start; i < cs.length; i++) {
            if (!isSpace(cs[i])) break;
            start++;
        }

        return start;
    }

    public static boolean isSpace(char c) {
        return (c == ' ') || (c == ' ') || (c == '\r') || (c == '\n') || (c == '\t') || (c == '\b') || (c == '\f');
    }

    public static int expect(char[] cs, int start, String expected) {
        start = ignoreSpace(cs, start);
        int end = start + expected.length();
        if (end > cs.length) {
            return -1;
        }
        for (int j = 0; j < expected.length(); j++) {
            int i = start + j;
            if (cs[i] != expected.charAt(j)) {
                return -1;
            }
        }
        return end;
    }

    public XMLDocument getDocument() {
        return this.doc;
    }

    public static int indexOf(char[] cs, char c, int start) {
        for (int i = start; i < cs.length; i++) {
            if (cs[i] == c) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(char[] cs, String str, int start) {
        boolean hasSame = false;
        int length = str.length();
        for (int i = start; i < cs.length - length + 1; i++) {
            hasSame = true;
            for (int j = 0; j < length; j++) {
                if (str.charAt(j) != cs[(i + j)]) {
                    hasSame = false;
                    break;
                }
            }
            if (hasSame) {
                return i;
            }
        }
        return -1;
    }

    public static int getLineNum(List<Integer> lineNumList, char[] cs, int charIndex) {
        if (lineNumList.size() == 0) {
            for (int i = 0; i < cs.length; i++) {
                if (cs[i] == '\n') {
                    lineNumList.add(Integer.valueOf(i));
                }
            }
        }
        int start = new Double(charIndex * 1.0D / cs.length * lineNumList.size()).intValue();
        if ((lineNumList.size() > start) && (((Integer) lineNumList.get(start)).intValue() > charIndex)) {
            for (int i = start - 1; i >= 0; i--) {
                if (((Integer) lineNumList.get(i)).intValue() < charIndex) {
                    return i + 2;
                }
            }
            return 1;
        }
        for (int i = start + 1; i < lineNumList.size(); i++) {
            if (((Integer) lineNumList.get(i)).intValue() > charIndex) {
                return i + 1;
            }
        }
        return lineNumList.size() + 1;
    }

    public static class ElementAttribute {
        public int EndCharIndex;
        public String Name;
        public String Value;

        public ElementAttribute(int index, String name, String value) {
            this.EndCharIndex = index;
            this.Name = name;
            this.Value = value;
        }
    }
}

