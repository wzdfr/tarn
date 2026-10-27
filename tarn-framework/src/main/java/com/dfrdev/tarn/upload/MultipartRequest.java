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
import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.core.Tarn;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.StringUtil;
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
            "class", "jar", "war", "exe", "sh", "bat", "cmd", "vbs", "php", "asp", "aspx", "asax"
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
     * 添加对相对路径的支持
     * 1: 以 "/" 开头或者以 "x:开头的目录被认为是绝对路径
     * 2: 其它路径被认为是相对路径, 需要 JFinalConfig.uploadedFileSaveDirectory 结合
     */
    private String handleSaveDirectory(String saveDirectory) {
        if (saveDirectory.startsWith("/") || saveDirectory.indexOf(":") == 1)
            return saveDirectory;
        else
            return MultipartRequest.saveDirectory + saveDirectory;
    }

    private void wrapMultipartRequest(HttpServletRequest request, String saveDirectory, int maxPostSize, String encoding) {
        if (!isMultipartSupported)
            throw new RuntimeException("Oreilly cos.jar is not found, Multipart post can not be supported.");

        saveDirectory = handleSaveDirectory(saveDirectory);

        File dir = new File(saveDirectory);
        if (!dir.exists()) {
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
                        AttrInject.invokeRecursive(controller, fieldName, parameterValue,AOP_FIELD_);
                    } else {
                        if (StringUtil.isNotEmpty(parameterValue))
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
                    if (isSafeFile(uploadFile))
                        uploadFiles.add(uploadFile);
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSafeFile(UploadFile uploadFile) {
        String fileName = uploadFile.getFileName();
        String ext = extractExtension(fileName);
        
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
        
        // 4) 白名单优先
        if (allowedExtensions != null && allowedExtensions.length > 0) {
            if (!containsExt(allowedExtensions, ext)) {
                LogUtil.warn("[文件上传] 扩展名不在白名单内: " + ext);
                uploadFile.getFile().delete();
                return false;
            }
            return true;
        }
        
        // 5) 黑名单兜底（默认启用）
        if (containsExt(DEFAULT_DENIED_EXT, ext)) {
            LogUtil.warn("[文件上传] 扩展名在黑名单内: " + ext);
            uploadFile.getFile().delete();
            return false;
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






