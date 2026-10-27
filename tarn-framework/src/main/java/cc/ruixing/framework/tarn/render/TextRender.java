package cc.ruixing.framework.tarn.render;


import java.io.IOException;
import java.io.PrintWriter;

/**
 * TextRender
 */
public class TextRender extends Render {

    private static final long serialVersionUID = 1812102713097864255L;

    private String text;
    private String type;

    public TextRender(String text) {
        this.text = text;
        this.type = "text/html";
    }

    public TextRender(String text, String renderContentType) {
        this.text = text;
        this.type = renderContentType;
    }

    public TextRender(String text, RenderContentType type) {
        this.view = text;
        this.text = text;
        //todo jdk1.6 switch 不支持String
        if (type == RenderContentType.json) {
            this.type = "application/json";
        } else if (type == RenderContentType.js) {
            this.type = "application/x-javascript";
        } else if (type == RenderContentType.css) {
            this.type = "text/css";
        } else if (type == RenderContentType.swf) {
            this.type = "application/x-shockwave-flash";
        } else if (type == RenderContentType.jpeg) {
            this.type = "image/jpeg";
        }
//        else if (type.equalsIgnoreCase("bmp") || type.equalsIgnoreCase("cgm")
//                || type.equalsIgnoreCase("gif") || type.equalsIgnoreCase("ief")
//                || type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("png")
//                || type.equalsIgnoreCase("svg+xml") || type.equalsIgnoreCase("tiff")) {
//            this.type = "image/" + type;
//        }
        else if (type == RenderContentType.doc) {
            this.type = "application/msword";
        } else if (type == RenderContentType.xls) {
            this.type = "application/vnd.ms-excel";
        } else {
            this.type = "text/html";
        }
    }

    public void render() {
        PrintWriter writer = null;
        try {
            response.setHeader("Pragma", "no-cache");    // HTTP/1.0 caches might not implement Cache-Control and might only implement Pragma: no-cache
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setContentType(type + ";charset=" + getEncoding());
            response.setCharacterEncoding(getEncoding());
            writer = response.getWriter();
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new RenderException(e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }


}

