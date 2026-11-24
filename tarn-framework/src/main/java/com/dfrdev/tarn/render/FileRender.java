package com.dfrdev.tarn.render;


import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.PathKit;

import javax.servlet.ServletContext;
import java.io.*;


/**
 * FileRender.
 */
public class FileRender extends Render {
	
	private static final long serialVersionUID = 4293616220202691369L;
	private File file;
	private String fileName;
//	private static String fileDownloadPath;
	private static ServletContext servletContext;
	private static String webRootPath;
	private static String DEFAULT_FILE_CONTENT_TYPE="application/octet-stream";

	public FileRender(File file) {
		this.file = file;
	}
	
	public FileRender(String fileName) {
		this.fileName = fileName;
	}
	
	static void init(ServletContext servletContext) {
//		FileRender.fileDownloadPath = fileDownloadPath;
		FileRender.servletContext = servletContext;
		webRootPath = PathKit.getWebRootPath();
	}
	
	/**
	 * 将下载路径解析到 Web 根目录内，拒绝路径穿越、绝对路径与敏感目录（WEB-INF/META-INF）。
	 *
	 * @param path 控制器传入的相对文件名
	 * @return 规范化后的文件；非法路径返回 null
	 */
	static File resolveDownloadFile(String path) {
		if (path == null || path.indexOf('\0') >= 0 || path.startsWith("~")) {
			return null;
		}
		String p = path.replace('\\', '/');
		for (String seg : p.split("/")) {
			if ("..".equals(seg) || "WEB-INF".equalsIgnoreCase(seg) || "META-INF".equalsIgnoreCase(seg)) {
				LogUtil.warn("[FileRender] 拒绝非法下载路径: " + path);
				return null;
			}
		}
		try {
			File root = new File(PathKit.getWebRootPath()).getCanonicalFile();
			File target = new File(root, p).getCanonicalFile();
			String rootPath = root.getPath();
			if (!target.getPath().equals(rootPath) && !target.getPath().startsWith(rootPath + File.separator)) {
				LogUtil.warn("[FileRender] 下载路径越出 Web 根目录: " + path);
				return null;
			}
			return target;
		} catch (IOException e) {
			LogUtil.warn("[FileRender] 下载路径规范化失败: " + path);
			return null;
		}
	}

	public void render() {
		if (fileName != null) {
            file = resolveDownloadFile(fileName);
		}
		
		if (file == null || !file.isFile() || file.length() > Integer.MAX_VALUE) {
            // response.sendError(HttpServletResponse.SC_NOT_FOUND);
            // return;
			// throw new RenderException("File not found!");
			RenderFactory.getInstance().getErrorRender(404).setContext(request, response).render();
			return ;
        }
		
		try {
			response.addHeader("Content-disposition", "attachment; filename=" + new String(file.getName().getBytes("GBK"), "ISO8859-1"));
		} catch (UnsupportedEncodingException e) {
			response.addHeader("Content-disposition", "attachment; filename=" + file.getName());
		}
		
        String contentType = servletContext.getMimeType(file.getName());
        if (contentType == null) {
        	contentType = DEFAULT_FILE_CONTENT_TYPE;		// "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLength((int)file.length());
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024];
            for (int n = -1; (n = inputStream.read(buffer)) != -1;) {
                outputStream.write(buffer, 0, n);
            }
            outputStream.flush();
        }
        catch (Exception e) {
        	throw new RenderException(e);
        }
        finally {
            if (inputStream != null) {
                try {
					inputStream.close();
				} catch (IOException e) {
					LogUtil.error("关闭输入流失败: " + e.getMessage(), e);
				}
            }
            if (outputStream != null) {
            	try {
					outputStream.close();
				} catch (IOException e) {
					LogUtil.error("关闭输出流失败: " + e.getMessage(), e);
				}
            }
        }
	}
}


