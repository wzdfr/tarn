package com.dfrdev.tarn.util.resource;



import com.dfrdev.tarn.collection.JarClassLoader;
import com.dfrdev.tarn.util.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * URL资源访问类
 * @author Looly
 *
 */
public class UrlResource implements Resource, Serializable{
	private static final long serialVersionUID = 1L;
	
	protected URL url;
	protected String name;
	
	//-------------------------------------------------------------------------------------- Constructor start
	/**
	 * 构造
	 * @param url URL
	 */
	public UrlResource(URL url) {
		this(url, null);
	}
	
	/**
	 * 构造
	 * @param url URL，允许为空
	 * @param name 资源名称
	 */
	public UrlResource(URL url, String name) {
		this.url = url;
		this.name = ObjectUtil.defaultIfNull(name, (null != url) ? FileUtil.getName(url.getPath()) : null);
	}
	
	/**
	 * 构造
	 * @param file 文件路径
	 * @deprecated Please use {@link FileResource}
	 */
	@Deprecated
	public UrlResource(File file) {
		this.url = JarClassLoader.getURL(file);
	}
	//-------------------------------------------------------------------------------------- Constructor end
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public URL getUrl(){
		return this.url;
	}
	
	@Override
	public InputStream getStream() throws IORuntimeException {
		if(null == this.url){
			throw new IORuntimeException("Resource URL is null!");
		}
		return getStream(url);
	}


	/**
	 * 从URL中获取流
	 *
	 * @param url {@link URL}
	 * @return InputStream流
	 * @since 3.2.1
	 */
	public static InputStream getStream(URL url) {
		if (url==null) return null;
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	/**
	 * 获得Reader
	 *
	 * @param url     {@link URL}
	 * @param charset 编码
	 * @return {@link BufferedReader}
	 * @since 3.2.1
	 */
	public static BufferedReader getReader(URL url, Charset charset) {
		return FileUtil.getReader(getStream(url), charset);
	}

	/**
	 * 获得Reader
	 * @param charset 编码
	 * @return {@link BufferedReader}
	 * @since 3.0.1
	 */
	@Override
	public BufferedReader getReader(Charset charset){
		return getReader(this.url, charset);
	}
	
	//------------------------------------------------------------------------------- read
	@Override
	public String readStr(Charset charset) throws IORuntimeException{
		BufferedReader reader = null;
		try {
			reader = getReader(charset);
			return FileUtil.read(reader);
		} finally {
			FileUtil.close(reader);
		}
	}
	
	@Override
	public byte[] readBytes() throws IORuntimeException{
		InputStream in = null;
		try {
			in = getStream();
			return FileUtil.readByte(in);
		} finally {
			FileUtil.close(in);
		}
	}
	
	/**
	 * 获得File
	 * @return {@link File}
	 */
	public File getFile(){
		return FileUtil.file(this.url);
	}
	
	/**
	 * 返回路径
	 * @return 返回URL路径
	 */
	@Override
	public String toString() {
		return (null == this.url) ? "null" : this.url.toString();
	}
}
