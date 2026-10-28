package com.dfrdev.tarn.util.io;


import com.dfrdev.tarn.core.boot.Config;
import com.sun.xml.internal.ws.util.UtilException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import com.dfrdev.tarn.util.LogUtil;

import com.dfrdev.tarn.util.text.StringUtil;
public class IOUtil {


    public static byte[] getBytesFromStream(InputStream is)
            throws IOException {
        return getBytesFromStream(is, 2147483647);
    }

    public static byte[] getBytesFromStream(InputStream is, int max) throws IOException {
        byte[] buffer = new byte[1024];
        int read = -1;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = (byte[]) null;
        try {
            while ((read = is.read(buffer)) != -1) {
                if (bos.size() > max) {
                    throw new IOException("InputStream length is out of range,max=" + max);
                }
                if (read > 0) {
                    byte[] chunk = (byte[]) null;
                    if (read == 1024) {
                        chunk = buffer;
                    } else {
                        chunk = new byte[read];
                        System.arraycopy(buffer, 0, chunk, 0, read);
                    }
                    bos.write(chunk);
                }
            }
            data = bos.toByteArray();
        } finally {
            if (bos != null) {
                bos.close();
                bos = null;
            }
        }
        return data;
    }

    public static void download(HttpServletRequest request, HttpServletResponse response, String fileName, InputStream is) {
        try {
            setDownloadFileName(request, response, fileName);
            if (is == null) {
                return;
            }
            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int read = -1;
            try {
                while ((read = is.read(buffer)) != -1)
                    if (read > 0) {
                        byte[] chunk = (byte[]) null;
                        if (read == 1024) {
                            chunk = buffer;
                        } else {
                            chunk = new byte[read];
                            System.arraycopy(buffer, 0, chunk, 0, read);
                        }
                        os.write(chunk);
                        os.flush();
                    }
            } finally {
                is.close();
            }
            os.flush();
            os.close();
        } catch (IOException e) {
            LogUtil.warn("IOUtil.download:IO ends by user!");
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
    }

    public static void setDownloadFileName(HttpServletRequest request, HttpServletResponse response, String fileName) {
        try {
            response.reset();
            response.setContentType("application/octet-stream");
            String userAgent = request.getHeader("User-Agent");
            if ((StringUtil.isNotEmpty(userAgent)) && (
                    (userAgent.toLowerCase().indexOf("msie") >= 0) || (userAgent.toLowerCase().indexOf("trident") >= 0)))
                fileName = new String(fileName.getBytes("GBK"), "ISO-8859-1");
            else {
                fileName = new String(fileName.getBytes(Config.getGlobalCharset()), "ISO-8859-1");
            }
            response.addHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
    }
}
