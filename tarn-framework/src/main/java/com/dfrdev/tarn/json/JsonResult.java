package com.dfrdev.tarn.json;

import java.io.Serializable;

/**
 * 标准化 JSON 响应体（Tarn 2.0+，前后端分离支持）。
 * <p>
 * Controller 方法直接返回本类型并配合 {@code @Json} 注解，即可输出统一格式：
 * <pre>
 *     {@code @Json}
 *     {@code @ERequestMapping("/user/list")}
 *     public JsonResult list() {
 *         return JsonResult.ok(service.findAll());
 *     }
 * </pre>
 * 输出格式：{"code":200,"message":"success","data":...,"timestamp":1690000000000}
 *
 * @author dfr
 * @since 2.0
 */
public class JsonResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_ERROR = 500;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;

    private int code;
    private String message;
    private Object data;
    private long timestamp;

    public JsonResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public JsonResult(int code, String message, Object data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static JsonResult ok() {
        return new JsonResult(CODE_SUCCESS, "success", null);
    }

    public static JsonResult ok(Object data) {
        return new JsonResult(CODE_SUCCESS, "success", data);
    }

    public static JsonResult ok(String message, Object data) {
        return new JsonResult(CODE_SUCCESS, message, data);
    }

    public static JsonResult fail(String message) {
        return new JsonResult(CODE_ERROR, message, null);
    }

    public static JsonResult fail(int code, String message) {
        return new JsonResult(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }

    // ---------- getter / setter ----------

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
