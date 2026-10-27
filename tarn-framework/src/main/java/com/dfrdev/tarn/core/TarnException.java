package com.dfrdev.tarn.core;


import com.dfrdev.tarn.util.TextKit;

public class TarnException extends RuntimeException {
    public TarnException(String errStr) {
        super(errStr);
    }
    public TarnException(String msg, Throwable cause) {
        super(msg);
        super.initCause(cause);
    }
    public TarnException(Throwable cause) {
        super(cause);
    }

    public TarnException(String messageTemplate, Object... params) {
        super(TextKit.format(messageTemplate, params));
    }
    private static final long serialVersionUID = 8996083616449366152L;
}
