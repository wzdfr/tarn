package com.dfrdev.tarn.upload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.dfrdev.tarn.aop.ActionInvocation;
import com.dfrdev.tarn.aop.AttrInject;
import com.dfrdev.tarn.core.web.Controller;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

/**
 * MultipartRequest.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MultipartRequest extends HttpServletRequestWrapper {

    private static String saveDirectory;
    private static int maxPostSize;
    private static String encoding;
    private static boolean isMultipartSupported = false;
    private static final DefaultFileRenamePolicy fileRenamePolicy = new DefaultFileRenamePolicy();

    // ==================== 安全加固(Tarn 2.0+)：上传扩展名管控 ====================
    /**
     * 默认拒绝上传的可执行/脚本类扩展名（大小写不敏感，含双扩展名兜底）。
     * 历史版本仅拒绝 .jsp，可被 .jspx/.JSP 等绕过。
     */
    private static final String[] DEFAULT_DENIED_EXT = {
            "jsp", "jspx", "jspa", "jsw", "jsv", "jhtml", "shtml",
            "class", "jar", "war", "exe", "sh", "bat", "cmd", "vbs", "php", "asp", "aspx", "asax",
            "html", "htm", "svg", "js", "phtml", "phar", "cgi", "htaccess", "user.ini"
    };

    /**
     * 默认允许上传的扩展名（白名单，S9）。
     * 未调用 {@link #setAllowedExtensions(String[])} 时按此列表校验。
     */
    private static final String[] DEFAULT_ALLOWED_EXT = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "txt", "csv", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z"
    };

    /**
     * 可选白名单：非空时仅允许列表内扩展名（优先级高于黑名单），由应用按需设置
     */
    private static volatile String[] allowedExtensions = null;

    private Controller controller;


    private List<UploadFile> uploadFiles;
    private com.oreilly.servlet.MultipartRequest multipartRequest;

    static void init(String saveDirectory, int maxPostSize, String encoding) {
        MultipartRequest.saveDirectory = saveDirectory;
        MultipartRequest.maxPostSize = maxPostSize;
        MultipartRequest.encoding = encoding;
        MultipartRequest.isMultipartSupported = true;    // 在OreillyCos.java中保障了, 只要被初始化就一定为 true
    }

    public MultipartRequest(HttpServletRequest request, String saveDirectory, int maxPostSize, String encoding,Controller controller) {
        super(request);
        this.controller=controller;
        wrapMultipartRequest(request, saveDirectory, maxPostSize, encoding);
    }

    public MultipartRequest(HttpServletRequest request, String saveDirectory, int maxPostSize,Controller controller) {
        super(request);
        this.controller=controller;
        wrapMultipartRequest(request, saveDirectory, maxPostSize, encoding);
    }

    public MultipartRequest(HttpServletRequest request, String saveDirectory,Controller controller) {
        super(request);
        this.controller=controller;
        wrapMultipartRequest(request, saveDirectory, maxPostSize, encoding);
    }

    public MultipartRequest(HttpServletRequest request,Controller controller) {
        super(request);
        this.controller=controller;
        wrapMultipartRequest(request, saveDirectory, maxPostSize, encoding);
    }

    /**
     * 将请求指定的保存目录解析到配置的上传根目录内。
     * <p>
     * 安全约束：只接受相对路径，拒绝绝对路径（/、盘符）、盘符穿越（..）与 NUL；
     * 规范化后必须仍位于配置根目录内，防止写入任意目录（S2）。
     */
    private String handleSaveDirectory(String saveDirectory) {
        return resolveSaveDirectory(MultipartRequest.saveDirectory, saveDirectory);
    }

    /**
     * 在给定上传根目录内解析保存目录（包级可见，便于单测）。
     */
    static String resolveSaveDirectory(String rootPath, String saveDirectory) {
        String dir = saveDirectory == null ? "" : saveDirectory.replace('\\', '/');
        if (dir.indexOf('\0') >= 0 || dir.indexOf("..") >= 0 || dir.startsWith("/") || dir.matches("^[A-Za-z]:.*")) {
            LogUtil.warn("[文件上传] 拒绝非法保存目录: " + saveDirectory);
            throw new IllegalArgumentException("非法的上传保存目录: " + saveDirectory);
        }
        if (rootPath == null || rootPath.trim().length() == 0) {
            throw new IllegalStateException("上传根目录未配置，请先调用 OreillyCos.init 或 setSaveDirectory");
        }
        try {
            File root = new File(rootPath).getCanonicalFile();
            File target = new File(root, dir).getCanonicalFile();
            String rootCanonical = root.getPath();
            if (!target.getPath().equals(rootCanonical)
                    && !target.getPath().startsWith(rootCanonical + File.separator)) {
                LogUtil.warn("[文件上传] 保存目录越出上传根目录: " + saveDirectory);
                throw new IllegalArgumentException("上传保存目录越界: " + saveDirectory);
            }
            return target.getPath();
        } catch (IOException e) {
            throw new RuntimeException("上传保存目录规范化失败: " + saveDirectory, e);
        }
    }

    private void wrapMultipartRequest(HttpServletRequest request, String saveDirectory, int maxPostSize, String encoding) {
        if (!isMultipartSupported)
            throw new RuntimeException("Oreilly cos.jar is not found, Multipart post can not be supported.");

        saveDirectory = handleSaveDirectory(saveDirectory);

        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            // 仅允许在已存在的配置根目录下创建子目录，避免自动创建任意路径
            if (!new File(MultipartRequest.saveDirectory).isDirectory()) {
                throw new RuntimeException("Upload root directory not exists: " + MultipartRequest.saveDirectory);
            }
            if (!dir.mkdirs()) {
                throw new RuntimeException("Directory " + saveDirectory + " not exists and can not create directory.");
            }
        }

//		String content_type = request.getContentType();
//        if (content_type == null || content_type.indexOf("multipart/form-data") == -1) {
//        	throw new RuntimeException("Not multipart request, enctype=\"multipart/form-data\" is not found of form.");
//        }

        uploadFiles = new ArrayList<UploadFile>();

        try {
            multipartRequest = new com.oreilly.servlet.MultipartRequest(request, saveDirectory, maxPostSize, encoding, fileRenamePolicy);
            Tarn.getPageContext().setMultipartRequest(multipartRequest);
            if(this.controller!=null){
                HashMap<String, Object> AOP_FIELD_ = new HashMap<String, Object>();
                Enumeration paramNames = multipartRequest.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String fieldName = (String) paramNames.nextElement();
                    String parameterValue = multipartRequest.getParameter(fieldName);
                    String vals[] = multipartRequest.getParameterValues(fieldName);
                    if (fieldName.indexOf(".") != -1) {
                        AttrInject.invokeRecursiveChecked(controller, fieldName, parameterValue, AOP_FIELD_);
                    } else {
                        if (StringUtil.isNotEmpty(parameterValue) && AttrInject.isBindable(controller, fieldName))
                            ActionInvocation.invokeSet(controller, fieldName, parameterValue);
                    }
                }
            }

            Enumeration files = multipartRequest.getFileNames();
            while (files.hasMoreElements()) {
                String name = (String) files.nextElement();
                String filesystemName = multipartRequest.getFilesystemName(name);
                // 文件没有上传则不生成 UploadFile, 这与 cos的解决方案不一样
                if (filesystemName != null) {
                    String originalFileName = multipartRequest.getOriginalFileName(name);
                    String contentType = multipartRequest.getContentType(name);
                    UploadFile uploadFile = new UploadFile(name, saveDirectory, filesystemName, originalFileName, contentType);
                    if (isSafeFile(uploadFile)) {
                        // 随机化落盘文件名，原始文件名仅作为展示（S9）
                        String ext = extractExtension(
                                StringUtil.isNotEmpty(originalFileName) ? originalFileName : filesystemName);
                        String randomName = randomFileName(ext);
                        File src = new File(saveDirectory, filesystemName);
                        File dest = new File(saveDirectory, randomName);
                        if (src.renameTo(dest)) {
                            uploadFiles.add(new UploadFile(name, saveDirectory, randomName,
                                    originalFileName, contentType));
                        } else {
                            LogUtil.warn("[文件上传] 重命名文件失败: " + filesystemName);
                            src.delete();
                        }
                    }
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isSafeFile(UploadFile uploadFile) {
        String fileName = uploadFile.getFileName();
        String originalFileName = uploadFile.getOriginalFileName();
        String ext = extractExtension(StringUtil.isNotEmpty(originalFileName) ? originalFileName : fileName);
        
        // Tarn 1.0 安全加固：增强路径穿越防护
        // 1) 文件名不得包含路径分隔符或上级目录引用
        if (fileName.indexOf("..") != -1 || fileName.indexOf('/') != -1 || fileName.indexOf('\\') != -1) {
            LogUtil.warn("[文件上传] 检测到路径穿越攻击尝试: " + fileName);
            uploadFile.getFile().delete();
            return false;
        }
        
        // 2) 规范化路径检查：确保文件确实在目标目录内
        try {
            File targetDir = new File(uploadFile.getSaveDirectory()).getCanonicalFile();
            File uploadedFile = new File(uploadFile.getSaveDirectory(), fileName).getCanonicalFile();
            if (!uploadedFile.getPath().startsWith(targetDir.getPath())) {
                LogUtil.warn("[文件上传] 路径规范化检查失败: " + fileName);
                uploadFile.getFile().delete();
                return false;
            }
        } catch (IOException e) {
            LogUtil.error("[文件上传] 路径规范化异常: " + e.getMessage(), e);
            uploadFile.getFile().delete();
            return false;
        }
        
        // 3) 文件名长度限制（防止缓冲区溢出攻击）
        if (fileName.length() > 255) {
            LogUtil.warn("[文件上传] 文件名过长: " + fileName.length() + " 字符");
            uploadFile.getFile().delete();
            return false;
        }
        
        // 4) 白名单校验（默认启用内置白名单；显式设置后以设置为准）
        String[] whiteList = (allowedExtensions != null && allowedExtensions.length > 0)
                ? allowedExtensions : DEFAULT_ALLOWED_EXT;
        if (!containsExt(whiteList, ext)) {
            LogUtil.warn("[文件上传] 扩展名不在白名单内: " + ext);
            uploadFile.getFile().delete();
            return false;
        }

        // 5) 黑名单兜底（防白名单配置失误）
        if (containsExt(DEFAULT_DENIED_EXT, ext)) {
            LogUtil.warn("[文件上传] 扩展名在黑名单内: " + ext);
            uploadFile.getFile().delete();
            return false;
        }

        // 6) 文件内容魔数校验（已知类型必须匹配，防止伪装扩展名）
        if (!hasValidMagic(uploadFile.getFile(), ext)) {
            LogUtil.warn("[文件上传] 文件内容与扩展名不匹配: " + ext);
            uploadFile.getFile().delete();
            return false;
        }

        return true;
    }

    /**
     * 随机落盘文件名（保留小写扩展名）。
     */
    private static String randomFileName(String ext) {
        String base = java.util.UUID.randomUUID().toString().replace("-", "");
        return ext == null || ext.length() == 0 ? base : base + "." + ext;
    }

    /**
     * 校验常见文件类型的魔数；未定义签名的类型返回 true。
     */
    private static boolean hasValidMagic(File file, String ext) {
        if (file == null || !file.isFile()) {
            return false;
        }
        byte[] head = new byte[16];
        int n;
        try {
            java.io.InputStream in = new java.io.FileInputStream(file);
            try {
                n = in.read(head);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return false;
        }
        if (n < 4) {
            return false;
        }
        int b0 = head[0] & 0xFF;
        int b1 = head[1] & 0xFF;
        int b2 = head[2] & 0xFF;
        int b3 = head[3] & 0xFF;
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF;
        } else if ("png".equals(ext)) {
            return b0 == 0x89 && b1 == 'P' && b2 == 'N' && b3 == 'G';
        } else if ("gif".equals(ext)) {
            return b0 == 'G' && b1 == 'I' && b2 == 'F' && b3 == '8';
        } else if ("bmp".equals(ext)) {
            return b0 == 'B' && b1 == 'M';
        } else if ("webp".equals(ext)) {
            return n >= 12 && b0 == 'R' && b1 == 'I' && b2 == 'F' && b3 == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        } else if ("pdf".equals(ext)) {
            return b0 == '%' && b1 == 'P' && b2 == 'D' && b3 == 'F';
        } else if ("zip".equals(ext) || "docx".equals(ext) || "xlsx".equals(ext) || "pptx".equals(ext)) {
            return b0 == 'P' && b1 == 'K';
        }
        return true;
    }

    /**
     * 提取小写扩展名，并做尾部点/空格归一化（防 "a.jsp." 、"a.jsp " 等变体绕过）
     */
    private static String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        String name = fileName.trim();
        while (name.endsWith(".") || name.endsWith(" ")) {
            name = name.substring(0, name.length() - 1);
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static boolean containsExt(String[] extList, String ext) {
        for (String e : extList) {
            if (e.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tarn 2.0+：设置上传扩展名白名单（设置后仅允许列表内文件，推荐生产环境使用）
     * <pre>
     *     MultipartRequest.setAllowedExtensions(new String[]{"jpg", "png", "pdf", "doc", "xls"});
     * </pre>
     */
    public static void setAllowedExtensions(String[] extensions) {
        allowedExtensions = extensions;
    }

    public static String[] getAllowedExtensions() {
        return allowedExtensions;
    }

    public List<UploadFile> getFiles() {
        return uploadFiles;
    }

    /**
     * Methods to replace HttpServletRequest methods
     */
    public Enumeration getParameterNames() {
        return multipartRequest.getParameterNames();
    }

    public String getParameter(String name) {
        return multipartRequest.getParameter(name);
    }

    public String[] getParameterValues(String name) {
        return multipartRequest.getParameterValues(name);
    }

    public Map getParameterMap() {
        Map map = new HashMap();
        Enumeration enumm = getParameterNames();
        while (enumm.hasMoreElements()) {
            String name = (String) enumm.nextElement();
            map.put(name, multipartRequest.getParameterValues(name));
        }
        return map;
    }
}






