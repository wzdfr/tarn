package com.dfrdev.tarn.util.io;


import com.dfrdev.tarn.collection.CharUtil;
import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.core.boot.Config;
import com.dfrdev.tarn.util.commons.ArrayUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import com.dfrdev.tarn.util.LogUtil;

import com.dfrdev.tarn.util.ResourceUtil;
import com.dfrdev.tarn.util.classloader.ClassUtil;
import com.dfrdev.tarn.util.classloader.NumberUtil;
import com.dfrdev.tarn.util.net.URLEncoder;
import com.dfrdev.tarn.util.text.CharsetUtil;
import com.dfrdev.tarn.util.text.StrFormatter;
import com.dfrdev.tarn.util.text.StringUtil;
public class FileUtil {
    /**
     * URL 前缀表示文件: "file:"
     */
    public static final String FILE_URL_PREFIX = "file:";

    public static String normalizePath(String path) {
        path = path.replace('\\', '/');
        path = StringUtil.replaceEx(path, "../", "/");
        path = StringUtil.replaceEx(path, "./", "/");
        if (path.endsWith("..")) {
            path = path.substring(0, path.length() - 2);
        }
        path = path.replaceAll("/+", "/");
        return path;
    }

    /**
     * 默认缓存大小 8192
     */
    public static final int DEFAULT_BUFFER_SIZE = 2 << 12;
    /**
     * 默认中等缓存大小 16384
     */
    public static final int DEFAULT_MIDDLE_BUFFER_SIZE = 2 << 13;
    /**
     * 默认大缓存大小 32768
     */
    public static final int DEFAULT_LARGE_BUFFER_SIZE = 2 << 14;
    /**
     * 数据流末尾
     */
    public static final int EOF = -1;

    public static File normalizeFile(File f) {
        String path = f.getAbsolutePath();
        path = normalizePath(path);
        return new File(path);
    }

    public static String getExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index < 0) {
            return null;
        }
        String ext = fileName.substring(index + 1);
        return ext.toLowerCase();
    }

    public static boolean writeText(String fileName, String content) {
        return writeText(fileName, content, "UTF-8");
    }

    public static boolean writeText(String fileName, String content, String encoding) {
        return writeText(fileName, content, encoding, false);
    }

    public static boolean writeText(String fileName, String content, String encoding, boolean bomFlag) {
        fileName = normalizePath(fileName);
        try {
            byte[] bs = content.getBytes(encoding);
            if ((encoding.equalsIgnoreCase("UTF-8")) && (bomFlag)) {
                bs = ArrayUtils.addAll(StringUtil.BOM, bs);
            }
            writeByte(fileName, bs);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static byte[] readByte(String fileName) {
        fileName = normalizePath(fileName);
        try {
            FileInputStream fis = new FileInputStream(fileName);
            byte[] r = new byte[fis.available()];
            fis.read(r);
            fis.close();
            return r;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static byte[] readByte(File f) {
        f = normalizeFile(f);
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] r = readByte(fis);
            fis.close();
            return r;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static byte[] readByte(InputStream is) {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while (true) {
            int bytesRead = -1;
            try {
                bytesRead = is.read(buffer);
            } catch (IOException e) {
                throw new RuntimeException("File.readByte() failed");
            }
            if (bytesRead != -1) {
                try {
                    os.write(buffer, 0, bytesRead);
                } catch (Exception e) {
                    throw new RuntimeException("File.readByte() failed");
                }
            } else {
                break;
            }
        }
        return os.toByteArray();
    }

    public static boolean writeByte(String fileName, byte[] b) {
        fileName = normalizePath(fileName);
        try {
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(fileName));
            fos.write(b);
            fos.close();
            return true;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean writeByte(File f, byte[] b) {
        f = normalizeFile(f);
        try {
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
            fos.write(b);
            fos.close();
            return true;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return false;
    }

    public static String readText(File f) {
        return readText(f, Config.getGlobalCharset());
    }

    public static String readText(File f, String encoding) {
        f = normalizeFile(f);
        try {
            InputStream is = new FileInputStream(f);
            String str = readText(is, encoding);
            is.close();
            return str;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static String readText(InputStream is, String encoding) {
        try {
            byte[] bs = readByte(is);
            if ((encoding.equalsIgnoreCase("utf-8")) &&
                    (StringUtil.hexEncode(ArrayUtils.subarray(bs, 0, 3)).equals("efbbbf"))) {
                bs = ArrayUtils.subarray(bs, 3, bs.length);
            }

            return new String(bs, encoding);
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 从Reader中读取String，读取完毕后并不关闭Reader
     *
     * @param reader Reader
     * @return String
     * @throws IORuntimeException IO异常
     */
    public static String read(Reader reader) throws IORuntimeException {
        final StringBuilder builder = new StringBuilder();
        final CharBuffer buffer = CharBuffer.allocate(DEFAULT_BUFFER_SIZE);
        try {
            while (-1 != reader.read(buffer)) {
                builder.append(buffer.flip().toString());
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return builder.toString();
    }

    /**
     * 从FileChannel中读取UTF-8编码内容
     *
     * @param fileChannel 文件管道
     * @return 内容
     * @throws IORuntimeException IO异常
     */
    public static String readUtf8(FileChannel fileChannel) throws IORuntimeException {
        return read(fileChannel, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 从FileChannel中读取内容
     *
     * @param fileChannel 文件管道
     * @param charset     字符集
     * @return 内容
     * @throws IORuntimeException IO异常
     */
    public static String read(FileChannel fileChannel, Charset charset) throws IORuntimeException {
        MappedByteBuffer buffer;
        try {
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).load();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return StrFormatter.str(buffer, charset);
    }

    public static String readText(String fileName) {
        return readText(fileName, Config.getGlobalCharset());
    }

    public static String readText(String fileName, String encoding) {
        fileName = normalizePath(fileName);
        try {
            InputStream is = new FileInputStream(fileName);
            String str = readText(is, encoding);
            is.close();
            return str;
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static String readURLText(String urlPath) {
        return readURLText(urlPath, Config.getGlobalCharset());
    }

    public static String readURLText(String urlPath, String encoding) {
        try {
            URL url = new URL(urlPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), encoding));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();
            return sb.toString();
        } catch (Exception e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return null;
    }

    public static boolean delete(String path) {
        File file = new File(path);
        return delete(file);
    }

    public static boolean delete(File f) {
        f = normalizeFile(f);
        if (!f.exists()) {
            LogUtil.warn("File or directory not found" + f);
            return false;
        }
        if (f.isFile()) {
            return f.delete();
        }
        return deleteDir(f);
    }

    private static boolean deleteDir(File dir) {
        dir = normalizeFile(dir);
        try {
            return (deleteFromDir(dir)) && (dir.delete());
        } catch (Exception e) {
            LogUtil.warn("Delete directory failed");
        }
        return false;
    }

    public static boolean mkdir(String path) {
        path = normalizePath(path);
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return true;
    }

    public static boolean exists(String path) {
        path = normalizePath(path);
        File dir = new File(path);
        return dir.exists();
    }

    public static boolean deleteFromDir(String dirPath) {
        dirPath = normalizePath(dirPath);
        File file = new File(dirPath);
        return deleteFromDir(file);
    }

    public static boolean deleteFromDir(File dir) {
        dir = normalizeFile(dir);
        if (!dir.exists()) {
            LogUtil.warn("Directory not found:" + dir);
            return false;
        }
        if (!dir.isDirectory()) {
            LogUtil.warn(dir + " is not directory");
            return false;
        }
        File[] tempList = dir.listFiles();
        for (int i = 0; i < tempList.length; i++) {
            if (!delete(tempList[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean copy(String oldPath, String newPath, FileFilter filter) {
        oldPath = normalizePath(oldPath);
        newPath = normalizePath(newPath);
        File oldFile = new File(oldPath);
        File[] oldFiles = oldFile.listFiles(filter);
        boolean flag = true;
        if (oldFiles != null) {
            for (int i = 0; i < oldFiles.length; i++) {
                if (!copy(oldFiles[i], newPath + "/" + oldFiles[i].getName())) {
                    flag = false;
                }
            }
        }
        return flag;
    }

    public static boolean copy(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        return copy(oldFile, newPath);
    }

    public static boolean copy(File oldFile, String newPath) {
        oldFile = normalizeFile(oldFile);
        if (!oldFile.exists()) {
            LogUtil.warn("File not found:" + oldFile);
            return false;
        }
        if (StringUtil.isEmpty(newPath)) {
            LogUtil.info("Destintion path:" + newPath + " cannot be empty!");
            return false;
        }
        if (oldFile.isFile()) {
            return copyFile(oldFile, newPath);
        }

        if (normalizePath(newPath).startsWith(normalizePath(oldFile.getAbsolutePath()))) {
            return renameToSubDir(normalizePath(oldFile.getAbsolutePath()), newPath, false);
        }
        return copyDir(oldFile, newPath);
    }

    private static boolean copyFile(File oldFile, String newPath) {
        oldFile = normalizeFile(oldFile);
        newPath = normalizePath(newPath);

        if (!oldFile.exists()) {
            LogUtil.warn("File not found:" + oldFile);
            return false;
        }
        if (!oldFile.isFile()) {
            LogUtil.warn(oldFile + " is not file");
            return false;
        }
        if (oldFile.getName().equalsIgnoreCase("Thumbs.db")) {
            LogUtil.warn(oldFile + " is ignored");
            return true;
        }
        mkdir(newPath.substring(0, newPath.lastIndexOf("/")));
        try {
            int byteread = 0;
            InputStream inStream = new FileInputStream(oldFile);
            File newFile = new File(newPath);

            if (newFile.isDirectory()) {
                newFile = new File(newPath, oldFile.getName());
            }
            if (newFile.getAbsoluteFile().equals(oldFile.getAbsoluteFile())) {
                return true;
            }
            FileOutputStream fs = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            while ((byteread = inStream.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
            fs.close();
            newFile.setLastModified(oldFile.lastModified());
            inStream.close();
        } catch (Exception e) {
            LogUtil.warn("Copy file " + oldFile.getPath() + " failed,cause:" + e.getMessage());
            LogUtil.error("操作失败: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    private static boolean copyDir(File oldDir, String newPath) {
        oldDir = normalizeFile(oldDir);
        newPath = normalizePath(newPath);
        if (!oldDir.exists()) {
            LogUtil.info("File not found:" + oldDir);
            return false;
        }
        if (!oldDir.isDirectory()) {
            LogUtil.info(oldDir + " is not directory");
            return false;
        }
        try {
            new File(newPath).mkdirs();
            File[] files = oldDir.listFiles();
            File temp = null;
            for (File file : files) {
                temp = file;
                if (temp.isFile()) {
                    if (!copyFile(temp, newPath + "/" + temp.getName()))
                        return false;
                } else if ((temp.isDirectory()) &&
                        (!copyDir(temp, newPath + "/" + temp.getName()))) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            LogUtil.info("Copy directory failed,cause:" + e.getMessage());
        }
        return false;
    }

    public static boolean move(String oldPath, String newPath) {
        if (StringUtil.isEmpty(newPath)) {
            LogUtil.info("Destintion path:" + newPath + " cannot be empty!");
            return false;
        }
        if (StringUtil.isEmpty(oldPath)) {
            LogUtil.info("Source path:" + oldPath + " cannot be empty!");
            return false;
        }
        oldPath = normalizePath(oldPath);
        newPath = normalizePath(newPath);

        if (newPath.startsWith(oldPath)) {
            return renameToSubDir(oldPath, newPath, true);
        }
        return (copy(oldPath, newPath)) && (delete(oldPath));
    }

    private static boolean renameToSubDir(String oldPath, String newPath, boolean deleteOldDir) {
        File oldFile = new File(oldPath);
        File oldParent = oldFile.getParentFile();

        if (oldParent == null) {
            String action = deleteOldDir ? "move" : "copy";
            LogUtil.info("Cannot " + action + " directory:" + oldPath + " to a subdirectory of itself " + newPath);
            return false;
        }

        String oldParentPath = normalizePath(oldParent.getAbsolutePath()) + "/";
        String tempPath = oldParentPath + System.currentTimeMillis();

        File tempFile = new File(tempPath);
        File newFile = new File(newPath);
        newFile.mkdirs();

        newFile.renameTo(tempFile);
        if (!copy(oldPath, tempPath)) {
            return false;
        }

        if ((deleteOldDir) && (!delete(oldPath))) {
            return false;
        }
        newFile.getParentFile().mkdirs();

        if (!tempFile.renameTo(newFile)) {
            oldFile.mkdirs();
            return (copy(tempPath, oldPath)) && (delete(tempPath));
        }
        return true;
    }

    public static void serialize(Serializable obj, String fileName) {
        fileName = normalizePath(fileName);
        try {
            FileOutputStream f = new FileOutputStream(fileName);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(obj);
            s.flush();
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // -------------------------------------------------------------------------------------- Copy start

    /**
     * 将Reader中的内容复制到Writer中 使用默认缓存大小，拷贝后不关闭Reader
     *
     * @param reader Reader
     * @param writer Writer
     * @return 拷贝的字节数
     * @throws Exception IO异常
     */
    public static long copy(Reader reader, Writer writer) throws Exception {
        return copy(reader, writer, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 将Reader中的内容复制到Writer中，拷贝后不关闭Reader
     *
     * @param reader     Reader
     * @param writer     Writer
     * @param bufferSize 缓存大小
     * @return 传输的byte数
     * @throws Exception IO异常
     */
    public static long copy(Reader reader, Writer writer, int bufferSize) throws Exception {
        return copy(reader, writer, bufferSize, null);
    }

    /**
     * 将Reader中的内容复制到Writer中，拷贝后不关闭Reader
     *
     * @param reader         Reader
     * @param writer         Writer
     * @param bufferSize     缓存大小
     * @param streamProgress 进度处理器
     * @return 传输的byte数
     * @throws Exception IO异常
     */
    public static long copy(Reader reader, Writer writer, int bufferSize, StreamProgress streamProgress) throws Exception {
        char[] buffer = new char[bufferSize];
        long size = 0;
        int readSize;
        if (null != streamProgress) {
            streamProgress.start();
        }
        try {
            while ((readSize = reader.read(buffer, 0, bufferSize)) != EOF) {
                writer.write(buffer, 0, readSize);
                size += readSize;
                writer.flush();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }
        return size;
    }

    /**
     * 拷贝流，使用默认Buffer大小，拷贝后不关闭流
     *
     * @param in  输入流
     * @param out 输出流
     * @return 传输的byte数
     * @throws IORuntimeException IO异常
     */
    public static long copy(InputStream in, OutputStream out) throws Exception {
        return copy(in, out, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 拷贝流，拷贝后不关闭流
     *
     * @param in         输入流
     * @param out        输出流
     * @param bufferSize 缓存大小
     * @return 传输的byte数
     * @throws Exception IO异常
     */
    public static long copy(InputStream in, OutputStream out, int bufferSize) throws Exception {
        return copy(in, out, bufferSize, null);
    }

    /**
     * 拷贝流，拷贝后不关闭流
     *
     * @param in             输入流
     * @param out            输出流
     * @param bufferSize     缓存大小
     * @param streamProgress 进度条
     * @return 传输的byte数
     * @throws Exception IO异常
     */
    public static long copy(InputStream in, OutputStream out, int bufferSize, StreamProgress streamProgress) throws Exception {

        if (bufferSize <= 0) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }

        byte[] buffer = new byte[bufferSize];
        if (null != streamProgress) {
            streamProgress.start();
        }
        long size = 0;
        try {
            for (int readSize; (readSize = in.read(buffer)) != EOF; ) {
                out.write(buffer, 0, readSize);
                size += readSize;
                out.flush();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (IOException e) {
            throw new Exception(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }
        return size;
    }

    /**
     * 拷贝流 thanks to: https://github.com/venusdrogon/feilong-io/blob/master/src/main/java/com/feilong/io/IOWriteUtil.java<br>
     * 本方法不会关闭流
     *
     * @param in             输入流
     * @param out            输出流
     * @param bufferSize     缓存大小
     * @param streamProgress 进度条
     * @return 传输的byte数
     * @throws Exception IO异常
     */
    public static long copyByNIO(InputStream in, OutputStream out, int bufferSize, StreamProgress streamProgress) throws Exception {
        return copy(Channels.newChannel(in), Channels.newChannel(out), bufferSize, streamProgress);
    }

    /**
     * 拷贝文件流，使用NIO
     *
     * @param in  输入
     * @param out 输出
     * @return 拷贝的字节数
     * @throws Exception IO异常
     */
    public static long copy(FileInputStream in, FileOutputStream out) throws Exception {


        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = in.getChannel();
            outChannel = out.getChannel();
            return inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw new Exception(e);
        } finally {
            close(outChannel);
            close(inChannel);
        }
    }
    /**
     * 关闭<br>
     * 关闭失败不会抛出异常
     *
     * @param closeable 被关闭的对象
     */
    public static void close(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默关闭
            }
        }
    }

    /**
     * 拷贝流，使用NIO，不会关闭流
     *
     * @param in  {@link ReadableByteChannel}
     * @param out {@link WritableByteChannel}
     * @return 拷贝的字节数
     * @throws Exception IO异常
     * @since 4.5.0
     */
    public static long copy(ReadableByteChannel in, WritableByteChannel out) throws Exception {
        return copy(in, out, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 拷贝流，使用NIO，不会关闭流
     *
     * @param in         {@link ReadableByteChannel}
     * @param out        {@link WritableByteChannel}
     * @param bufferSize 缓冲大小，如果小于等于0，使用默认
     * @return 拷贝的字节数
     * @throws Exception IO异常
     * @since 4.5.0
     */
    public static long copy(ReadableByteChannel in, WritableByteChannel out, int bufferSize) throws Exception {
        return copy(in, out, bufferSize, null);
    }

    /**
     * 拷贝流，使用NIO，不会关闭流
     *
     * @param in             {@link ReadableByteChannel}
     * @param out            {@link WritableByteChannel}
     * @param bufferSize     缓冲大小，如果小于等于0，使用默认
     * @param streamProgress {@link StreamProgress}进度处理器
     * @return 拷贝的字节数
     * @throws Exception IO异常
     */
    public static long copy(ReadableByteChannel in, WritableByteChannel out, int bufferSize, StreamProgress streamProgress) throws Exception {
        if(in==null || out==null) return 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize <= 0 ? DEFAULT_BUFFER_SIZE : bufferSize);
        long size = 0;
        if (null != streamProgress) {
            streamProgress.start();
        }
        try {
            while (in.read(byteBuffer) != EOF) {
                byteBuffer.flip();// 写转读
                size += out.write(byteBuffer);
                byteBuffer.clear();
                if (null != streamProgress) {
                    streamProgress.progress(size);
                }
            }
        } catch (IOException e) {
            throw new Exception(e);
        }
        if (null != streamProgress) {
            streamProgress.finish();
        }

        return size;
    }
    // -------------------------------------------------------------------------------------- Copy end

    public static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream s = new ObjectOutputStream(b);
            s.writeObject(obj);
            s.flush();
            s.close();
            return b.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object unserialize(String fileName) {
        fileName = normalizePath(fileName);
        try {
            FileInputStream in = new FileInputStream(fileName);
            ObjectInputStream s = new ObjectInputStream(in);
            Object o = s.readObject();
            s.close();
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object unserialize(byte[] bs) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(bs);
            ObjectInputStream s = new ObjectInputStream(in);
            Object o = s.readObject();
            s.close();
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] mapToBytes(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            for (Entry entry : map.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                if (k != null) {
                    if (v == null)
                        bos.write(new byte[1]);
                    else if ((v instanceof String))
                        bos.write(new byte[]{1});
                    else if ((v instanceof Long))
                        bos.write(new byte[]{2});
                    else if ((v instanceof Integer))
                        bos.write(new byte[]{3});
                    else if ((v instanceof Boolean))
                        bos.write(new byte[]{4});
                    else if ((v instanceof Date))
                        bos.write(new byte[]{5});
                    else if ((v instanceof Mapx))
                        bos.write(new byte[]{6});
                    else if ((v instanceof Serializable))
                        bos.write(new byte[]{7});
                    else {
                        throw new RuntimeException("Unknown datatype:" + v.getClass().getName());
                    }
                    byte[] bs = k.toString().getBytes();
                    bos.write(NumberUtil.toBytes(bs.length));
                    bos.write(bs);
                    if (v != null) {
                        if ((v instanceof String)) {
                            bs = v.toString().getBytes();
                            bos.write(NumberUtil.toBytes(bs.length));
                            bos.write(bs);
                        } else if ((v instanceof Long)) {
                            bos.write(NumberUtil.toBytes(((Long) v).longValue()));
                        } else if ((v instanceof Integer)) {
                            bos.write(NumberUtil.toBytes(((Integer) v).intValue()));
                        } else if ((v instanceof Boolean)) {
                            bos.write(((Boolean) v).booleanValue() ? 1 : 0);
                        } else if ((v instanceof Date)) {
                            bos.write(NumberUtil.toBytes(((Date) v).getTime()));
                        } else if ((v instanceof Mapx)) {
                            byte[] arr = mapToBytes((Mapx) v);
                            bos.write(NumberUtil.toBytes(arr.length));
                            bos.write(arr);
                        } else if ((v instanceof Serializable)) {
                            byte[] arr = serialize((Serializable) v);
                            bos.write(NumberUtil.toBytes(arr.length));
                            bos.write(arr);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }

        return bos.toByteArray();
    }

    public static Mapx<String, Object> bytesToMap(byte[] arr) {
        if (arr == null) {
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(arr);
        int b = -1;
        Mapx map = new Mapx();
        byte[] kbs = new byte[4];
        byte[] vbs = (byte[]) null;
        try {
            while ((b = bis.read()) != -1) {
                bis.read(kbs);
                int len = NumberUtil.toInt(kbs);
                vbs = new byte[len];
                bis.read(vbs);
                String k = new String(vbs);
                Object v = null;
                if (b == 1) {
                    bis.read(kbs);
                    len = NumberUtil.toInt(kbs);
                    vbs = new byte[len];
                    bis.read(vbs);
                    v = new String(vbs);
                } else if (b == 2) {
                    vbs = new byte[8];
                    bis.read(vbs);
                    v = new Long(NumberUtil.toLong(vbs));
                } else if (b == 3) {
                    vbs = new byte[4];
                    bis.read(vbs);
                    v = new Integer(NumberUtil.toInt(vbs));
                } else if (b == 4) {
                    int i = bis.read();
                    v = new Boolean(i == 1);
                } else if (b == 5) {
                    vbs = new byte[8];
                    bis.read(vbs);
                    v = new Date(NumberUtil.toLong(vbs));
                } else if (b == 6) {
                    bis.read(kbs);
                    len = NumberUtil.toInt(kbs);
                    vbs = new byte[len];
                    bis.read(vbs);
                    v = bytesToMap(vbs);
                } else if (b == 7) {
                    bis.read(kbs);
                    len = NumberUtil.toInt(kbs);
                    vbs = new byte[len];
                    bis.read(vbs);
                    v = unserialize(vbs);
                }
                map.put(k, v);
            }
        } catch (IOException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
        return map;
    }
    /**
     * 递归遍历目录以及子目录中的所有文件<br>
     * 如果提供file为文件，直接返回过滤结果
     *
     * @param file       当前遍历文件或目录
     * @param fileFilter 文件过滤规则对象，选择要保留的文件，只对文件有效，不过滤目录
     * @return 文件列表
     */
    public static List<File> loopFiles(File file, FileFilter fileFilter) {
        final List<File> fileList = new ArrayList<>();
        if (null == file || false == file.exists()) {
            return fileList;
        }

        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            if (ArrayUtils.isNotEmpty(subFiles)) {
                for (File tmp : subFiles) {
                    fileList.addAll(loopFiles(tmp, fileFilter));
                }
            }
        } else {
            if (null == fileFilter || fileFilter.accept(file)) {
                fileList.add(file);
            }
        }

        return fileList;
    }

    /**
     * 判断是否为文件，如果file为null，则返回false
     *
     * @param file 文件
     * @return 如果为文件true
     */
    public static boolean isFile(File file) {
        return (null != file) && file.isFile();
    }

    /**
     * 给定路径已经是绝对路径<br>
     * 此方法并没有针对路径做标准化，建议先执行{@link #(String)}方法标准化路径后判断
     *
     * @param path 需要检查的Path
     * @return 是否已经是绝对路径
     */
    public static boolean isAbsolutePath(String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }

        // 给定的路径已经是绝对路径了
        return CharUtil.SLASH == path.charAt(0) || path.matches("^[a-zA-Z]:([/\\\\].*)?");
    }

    /**
     * 返回文件名
     *
     * @param file 文件
     * @return 文件名
     * @since 4.1.13
     */
    public static String getName(File file) {
        return (null != file) ? file.getName() : null;
    }

    /**
     * 返回文件名
     *
     * @param filePath 文件
     * @return 文件名
     * @since 4.1.13
     */
    public static String getName(String filePath) {
        if (null == filePath) {
            return null;
        }
        int len = filePath.length();
        if (0 == len) {
            return filePath;
        }
        if (CharUtil.isFileSeparator(filePath.charAt(len - 1))) {
            // 以分隔符结尾的去掉结尾分隔符
            len--;
        }

        int begin = 0;
        char c;
        for (int i = len - 1; i > -1; i--) {
            c = filePath.charAt(i);
            if (CharUtil.isFileSeparator(c)) {
                // 查找最后一个路径分隔符（/或者\）
                begin = i + 1;
                break;
            }
        }

        return filePath.substring(begin, len);
    }
    /**
     * 获得一个Reader
     *
     * @param in      输入流
     * @param charset 字符集
     * @return BufferedReader对象
     */
    public static BufferedReader getReader(InputStream in, Charset charset) {
        if (null == in) {
            return null;
        }

        InputStreamReader reader;
        if (null == charset) {
            reader = new InputStreamReader(in);
        } else {
            reader = new InputStreamReader(in, charset);
        }

        return new BufferedReader(reader);
    }

    /**
     * 转URL为URI
     *
     * @param url URL
     * @return URI
     * @throws IORuntimeException 包装URISyntaxException
     */
    public static URI toURI(URL url) throws IORuntimeException {
        return toURI(url, false);
    }

    /**
     * 转URL为URI
     *
     * @param url      URL
     * @param isEncode 是否编码参数中的特殊字符（默认UTF-8编码）
     * @return URI
     * @throws IORuntimeException 包装URISyntaxException
     * @since 4.6.9
     */
    public static URI toURI(URL url, boolean isEncode) throws IORuntimeException {
        if (null == url) {
            return null;
        }

        return toURI(url.toString(), isEncode);
    }

    /**
     * 转字符串为URI
     *
     * @param location 字符串路径
     * @return URI
     * @throws IORuntimeException 包装URISyntaxException
     */
    public static URI toURI(String location) throws IORuntimeException {
        return toURI(location, false);
    }

    /**
     * 转字符串为URI
     *
     * @param location 字符串路径
     * @param isEncode 是否编码参数中的特殊字符（默认UTF-8编码）
     * @return URI
     * @throws IORuntimeException 包装URISyntaxException
     * @since 4.6.9
     */
    public static URI toURI(String location, boolean isEncode) throws IORuntimeException {
        if (isEncode) {
            location = encode(location);
        }
        try {
            return new URI(StringUtil.trim(location));
        } catch (URISyntaxException e) {
            throw new IORuntimeException(e);
        }
    }
    /**
     * 编码字符为 application/x-www-form-urlencoded<br>
     * 将需要转换的内容（ASCII码形式之外的内容），用十六进制表示法转换出来，并在之前加上%开头。<br>
     * 此方法用于URL自动编码，类似于浏览器中键入地址自动编码，对于像类似于“/”的字符不再编码
     *
     * @param url     被编码内容
     * @param charset 编码
     * @return 编码后的字符
     * @since 4.4.1
     */
    public static String encode(String url, Charset charset) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        if (null == charset) {
            charset = CharsetUtil.defaultCharset();
        }
        return URLEncoder.DEFAULT.encode(url, charset);
    }
    public static String encode(String url) throws IORuntimeException {
        return encode(url, CharsetUtil.CHARSET_UTF_8);
    }
    /**
     * 创建File对象
     *
     * @param url 文件URL
     * @return File
     */
    public static File file(URL url) {
        return new File(toURI(url));
    }
    public static String getDecodedPath(URL url) {
        if (null == url) {
            return null;
        }

        String path = null;
        try {
            // URL对象的getPath方法对于包含中文或空格的问题
            path = toURI(url).getPath();
        } catch (IORuntimeException e) {
            // ignore
        }
        return (null != path) ? path : url.getPath();
    }

    /**
     * 获取绝对路径，相对于ClassPath的目录<br>
     * 如果给定就是绝对路径，则返回原路径，原路径把所有\替换为/<br>
     * 兼容Spring风格的路径表示，例如：classpath:config/example.setting也会被识别后转换
     *
     * @param path 相对路径
     * @return 绝对路径
     */
    public static String getAbsolutePath(String path) {
        return getAbsolutePath(path, null);
    }

    /**
     * 获取标准的绝对路径
     *
     * @param file 文件
     * @return 绝对路径
     */
    public static String getAbsolutePath(File file) {
        if (file == null) {
            return null;
        }

        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * 获取绝对路径<br>
     * 此方法不会判定给定路径是否有效（文件或目录存在）
     *
     * @param path      相对路径
     * @param baseClass 相对路径所相对的类
     * @return 绝对路径
     */
    public static String getAbsolutePath(String path, Class<?> baseClass) {
        String normalPath;
        if (path == null) {
            normalPath = StringUtil.EMPTY;
        } else {
            normalPath = normalizePath(path);
            if (isAbsolutePath(normalPath)) {
                // 给定的路径已经是绝对路径了
                return normalPath;
            }
        }

        // 相对于ClassPath路径
        final URL url = ResourceUtil.getResource(normalPath, baseClass);
        if (null != url) {
            // 对于jar中文件包含file:前缀，需要去掉此类前缀，在此做标准化，since 3.0.8 解决中文或空格路径被编码的问题
            return FileUtil.normalizePath(getDecodedPath(url));
        }

        // 如果资源不存在，则返回一个拼接的资源绝对路径
        final String classPath = ClassUtil.getClassPath();
        if (null == classPath) {
            // throw new NullPointerException("ClassPath is null !");
            // 在jar运行模式中，ClassPath有可能获取不到，此时返回原始相对路径（此时获取的文件为相对工作目录）
            return path;
        }

        // 资源不存在的情况下使用标准化路径有问题，使用原始路径拼接后标准化路径
        return normalizePath(classPath.concat(Objects.requireNonNull(path)));
    }
    /**
     * 创建File对象，自动识别相对或绝对路径，相对路径将自动从ClassPath下寻找
     *
     * @param path 文件路径
     * @return File
     */
    public static File file(String path) {
        if (null == path) {
            return null;
        }
        return new File(getAbsolutePath(path));
    }
}