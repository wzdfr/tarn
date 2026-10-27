package cc.ruixing.framework.tarn.upload;

/**
 * OreillyCos.
 */
public class OreillyCos {
	
	private static Boolean isMultipartSupported = null;
	
	public static boolean isMultipartSupported() {
		if (isMultipartSupported == null) {
			detectOreillyCos();
		}
		return isMultipartSupported;
	}
	
	public static void init(String saveDirectory, int maxPostSize, String encoding) {
		if (isMultipartSupported()) {
			MultipartRequest.init(saveDirectory, maxPostSize, encoding);
		}
	}
	
	private static void detectOreillyCos() {
		try {
			Class.forName("com.oreilly.servlet.MultipartRequest");
			isMultipartSupported = true;
		} catch (ClassNotFoundException e) {
			isMultipartSupported = false;
		}
	}
}
