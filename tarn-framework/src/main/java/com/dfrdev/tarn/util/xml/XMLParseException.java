package com.dfrdev.tarn.util.xml;


import com.dfrdev.tarn.core.TarnException;

public class XMLParseException extends TarnException {
    private static final long serialVersionUID = 1L;

    public XMLParseException(String message) {
        super(message);
    }

    public XMLParseException(Throwable t) {
        super(t);
    }
}

