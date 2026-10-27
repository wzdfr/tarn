package cc.ruixing.framework.tarn.upload;

import java.io.File;

/**
 * UploadFile.
 */
public class UploadFile {
	
	private String parameterName;
	
	private String saveDirectory;
	private String fileName;
	private String originalFileName;
	private String contentType;
	
	public UploadFile(String parameterName, String saveDirectory, String filesystemName, String originalFileName, String contentType) {
		this.parameterName = parameterName;
		this.saveDirectory = saveDirectory;
		this.fileName = filesystemName;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
	}
	
	public String getParameterName() {
		return parameterName;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getOriginalFileName() {
		return originalFileName;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public String getSaveDirectory() {
		return saveDirectory;
	}
	
	public File getFile() {
		if (saveDirectory == null || fileName == null) {
			return null;
		} else {
			return new File(saveDirectory + File.separator + fileName);
		}
	}
}






