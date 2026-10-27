package com.dfrdev.tarn.util;

import java.io.*;

/**
 * 此类提供文件的相关操作.
 * @author DFR
 */
public final class FileKit {

    @SuppressWarnings("unused")
    private FileKit() {
    }

    /**
     * 安全的复制单个文件.
     * 此方法等同于：FileKit.copyFile(oldPath, newPath, false);
     *
     * @param oldPath 原文件路径 如：c:/a.txt
     * @param newPath 复制后路径 如：f:/b.txt
     * @return 如果成功复制文件，将返回true
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static boolean copyFile(String oldPath, String newPath) throws IOException {
        return copyFile(oldPath, newPath, false);
    }

    /**
     * 安全的复制单个文件.
     * 此方法等同于：FileKit.copyFile(oldPath, newPath, false);
     *
     * @param oldPath   原文件路径 如：c:/a.txt
     * @param newPath   复制后路径 如：f:/b.txt
     * @param overWrite 指定当目标位置已经存在文件时，是否覆盖该文件.
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断操作是否成功.
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static boolean copyFile(String oldPath, String newPath, boolean overWrite) throws IOException {
        File f2 = new File(newPath);
        if (!overWrite && f2.exists()) {
            return false;
        }
        int byteread;
        File oldfile = new File(oldPath);
        if (oldfile.exists()) { //文件存在时
            InputStream inStream = new FileInputStream(oldPath); //读入原文件
            FileOutputStream fs = new FileOutputStream(newPath);
            byte[] buffer = new byte[1444];
            while ((byteread = inStream.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
            fs.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 创建新目录.
     *
     * @param path 要创建的新目录路径.
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static void newFolder(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * 返回指定路径下的文件（夹）所占用空间的字节大小.
     *
     * @param path 文件（夹）的路径
     * @return 该路径下的文件(夹)所占用的空间字节大小.
     * @see #sizeStr(String)
     */
    public static long size(String path) {
        //此方法仅用于获取文件大小
        File f = new File(path);
        if (!f.exists()) {
            return 0L;
        }
        if (f.isFile()) {
            return f.length();
        }
        File[] fs = f.listFiles();
        long sizes = 0L;
        for (File f1 : fs) {
            sizes += size(f1.getAbsolutePath());
        }
        return sizes;
    }

    /**
     * 返回指定路径下的文件（夹）所占用空间的大小并以字符串返回. <br />
     * （根据文件大小自动格式化为以Byte,Kb, Mb, Gb等单位的字符串）
     *
     * @param path 文件（夹）的路径
     * @return 该路径下的文件(夹)所占用的空间大小的字符串表示.
     * @see #size(String)
     */
    public static String sizeStr(String path) {
        //此方法仅用于获取文件大小,因此未加JC_Data目录安全限制(另：备份数据库中也用到了此方法,所以如果要限制,需同时更改备份数据库的代码)
        long size1 = size(path);
        return sizeStr(size1);
    }
      /**
     * 根据文件大小自动格式化为以Byte,Kb, Mb, Gb等单位的字符串
     *
     * @param size 文件（夹）的字节大小
     * @return 返回该字节大小格式化后的Byte,Kb, Mb, Gb等单位的字符串
     * @see #size(String)
     */
    public static String sizeStr(long size){
        double size0;
        String s = "Byte";
        if (size < 1024 * 1024 && size > 1024) {
            size0 = size / 1024.0;
            s = "KB";
        } else if (size < 1024 * 1000 * 1024 && size > 1024 * 1024) {
            size0 = size / (1024 * 1024.0);
            s = "MB";
        } else if (size < 1024L * 1024 * 1024 * 1024 && size > 1024 * 1024 * 1024) {
            size0 = size / (1024 * 1024 * 1024.0);
            s = "GB";
        } else {
            size0 = size;
        }
        size0 = Math.round(size0 * 100.0) / 100.0;
        String ss = String.valueOf(size0);
        if (ss.lastIndexOf(".") == ss.length() - 2) {
            ss += "0";
        }
        return ss + " " + s;
    }

    /**
     * 删除文件夹.
     *
     * @param path 要删除的文件夹路径，如果此目录下还有子目录或文件，将连同一起删除。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static void delFolder(String path) throws IOException {
        delAllFile(path);
        delFile(path);
    }

    /**
     * 删除空文件夹.
     *
     * @param path 要删除的空文件夹路径，如果此文件夹不为空，则不执行任何操作。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public synchronized static void delIfEmpty(String path) throws IOException {
        File f = new File(path);
        if (f.listFiles() == null || f.listFiles().length == 0) {
            f.delete();
        }
    }

    /**
     * 清空某个文件夹里面的全部内容 （包括所有的子文件夹及文件）.
     * <br />此方法仅删除目录下的子目录和文件,当前的目录并不会删除.
     *
     * @param path String 要清空内容的文件夹。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static void delAllFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        for (String aTempList : tempList) {
            File temp;
            if (path.endsWith(File.separator)) {
                temp = new File(path + aTempList);
            } else {
                temp = new File(path + File.separator + aTempList);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delFolder(path + "/" + aTempList);
            }
        }
    }

    /**
     * 删除文件.
     *
     * @param path 要删除的文件路径。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static void delFile(String path) throws IOException {
        File myDelFile = new File(path);
        if (myDelFile.exists()) {
            myDelFile.delete();
        }
    }

    /**
     * 安全的移动文件.
     * 将文件从一个位置安全的移动到另一个位置, 此方法等同于：FileKit.moveFile(oldPath, newPath, false);
     *
     * @param oldPath 要移动的文件路径. 如：c:/a.txt
     * @param newPath 要移动的文件路径. 如：d:/a.txt
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断移动操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    @SuppressWarnings("unused")
    public static boolean moveFile(String oldPath, String newPath) throws IOException {
        return moveFile(oldPath, newPath, false);
    }

    /**
     * 移动文件.
     *
     * @param oldPath   要移动的文件路径. 如：c:/a.txt
     * @param newPath   要移动的文件路径. 如：d:/a.txt
     * @param overWrite 指定当目标位置已经存在文件时，是否覆盖该文件。
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断移动操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static boolean moveFile(String oldPath, String newPath, boolean overWrite) throws IOException {
        File f2 = new File(newPath);
        if (!overWrite && f2.exists()) {
            return false;
        }
        copyFile(oldPath, newPath, true);
        delFile(oldPath);
        return true;
    }

    /**
     * 安全的移动文件夹位置.
     * 此方法等同于Fso.moveFolder(oldPath, newPath, false);
     *
     * @param oldPath 如：c:/abc
     * @param newPath 如：d:/bcd
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    @SuppressWarnings("unused")
    public static boolean moveFolder(String oldPath, String newPath) throws IOException {
        return moveFolder(oldPath, newPath, false);
    }

    /**
     * 移动文件夹位置.
     *
     * @param oldPath   如：c:/abc
     * @param newPath   如：d:/bcd
     * @param overWrite 指定当目标位置已经存在文件夹时，是否覆盖。
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static boolean moveFolder(String oldPath, String newPath, boolean overWrite) throws IOException {
        File f2 = new File(newPath);
        if (!overWrite && f2.exists()) {
            return false;
        }
        copyFolder(oldPath, newPath, true);
        delFolder(oldPath);
        return true;
    }

    /**
     * 安全的复制整个文件夹内容.
     *
     * @param oldPath 原文件路径 如：c:/fqf/aa
     * @param newPath 复制后路径 如：f:/fqf/bb
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    @SuppressWarnings("unused")
    public static boolean copyFolder(String oldPath, String newPath) throws IOException {
        return copyFolder(oldPath, newPath, false);
    }

    /**
     * 复制整个文件夹内容.
     *
     * @param oldPath   原文件路径 如：c:/abc
     * @param newPath   复制后路径 如：f:/bcd
     * @param overWrite 指定当目标位置已经存在文件夹时，是否覆盖。
     * @return 操作成功将返回true.应尽可能的检查返回状态，以判断操作是否成功。
     * @throws java.io.IOException 当发生文件I/O读写错误时将抛出此异常.
     */
    public static boolean copyFolder(String oldPath, String newPath, boolean overWrite) throws IOException {
        File f2 = new File(newPath);
        if (!overWrite && f2.exists()) {
            return false;
        }
        newFolder(newPath);
        File a = new File(oldPath);
        String[] files = a.list();
        for (String file : files) {
            File temp;
            if (oldPath.endsWith(File.separator)) {
                temp = new File(oldPath + file);
            } else {
                temp = new File(oldPath + File.separator + file);
            }
            if (temp.isFile()) {
                String oFile = temp.getAbsolutePath();
                String nFile = newPath + File.separator + temp.getName();
                copyFile(oFile, nFile);
            } else if (temp.isDirectory()) {
                copyFolder(oldPath + File.separator + file, newPath + File.separator + file, true);
            }
        }
        return true;
    }

    /**
     * 读取文本文件.
     * @param file 要读取的文本文件
     * @return 读出的文本内容。
     * @throws java.io.IOException ex
     */
    public static String readTxt(File file, String encoding ) throws IOException {
        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
            reader = new BufferedReader(read);
            String text;
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
            }
        }finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return contents.toString();
    }

    /**
     * 将文本内容写入文件
     * @param file 文件
     * @param encoding 文本文件打开的编码方式
     * @param str 文本内容
     */
    public static void saveTxt(File file, String str,String encoding) {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;

        try {
            /*if (!file.exists()) {
                file.createNewFile();
                file = new File(fileName);
            }*/
            fos = new FileOutputStream(file);
            osw = new OutputStreamWriter(fos, encoding);

            osw.write(str);
            osw.flush();
        } catch (IOException iex) {
            iex.printStackTrace();
        } finally {
            try {
                if (null != osw)
                    osw.close();
                if (null != fos)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
