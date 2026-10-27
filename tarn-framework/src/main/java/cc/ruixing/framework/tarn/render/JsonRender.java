package cc.ruixing.framework.tarn.render;

import cc.ruixing.framework.tarn.json.JsonKit;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * JSON 渲染器（Tarn 2.0+，前后端分离支持）。
 * <p>
 * 配合 Controller 方法上的 {@code @Json} 注解或直接返回 {@link cc.ruixing.framework.tarn.json.JsonResult} 使用，
 * 输出 Content-Type: application/json;charset=UTF-8。
 *
 * @author dfr
 * @since 2.0
 */
public class JsonRender extends Render {

    private static final long serialVersionUID = 1L;

    private final Object data;

    public JsonRender(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void render() {
        PrintWriter writer = null;
        try {
            response.setContentType("application/json;charset=" + getEncoding());
            response.setCharacterEncoding(getEncoding());
            writer = response.getWriter();
            writer.write(JsonKit.toJson(data));
            writer.flush();
        } catch (IOException e) {
            throw new RenderException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
