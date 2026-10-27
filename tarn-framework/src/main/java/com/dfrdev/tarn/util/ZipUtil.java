package com.dfrdev.tarn.util;


import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ZipUtil {
    public static byte[] zip(byte[] bs) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater def = new Deflater();
        DeflaterOutputStream dos = new DeflaterOutputStream(bos, def);
        try {
            dos.write(bs);
            dos.finish();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] r = bos.toByteArray();
        return r;
    }

    public static void zipBatch(String base, String[] srcFiles, OutputStream destStream)
            throws Exception {
        File[] files = new File[srcFiles.length];
        for (int i = 0; i < srcFiles.length; i++) {
            files[i] = new File(srcFiles[i]);
        }
        zipBatch(base, files, destStream);
    }

    public static void zipBatch(String base, File[] srcFiles, OutputStream destStream)
            throws Exception {
        zipBatch(base, srcFiles, destStream, true);
    }

    public static void zipBatch(String base, File[] srcFiles, OutputStream destStream, boolean filterSVN) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(destStream);
        base = FileUtil.normalizePath(base);
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        for (int k = 0; k < srcFiles.length; k++)
            if (srcFiles[k].exists()) {
                List fileList = getSubFiles(srcFiles[k], filterSVN);
                ZipEntry ze = null;
                byte[] buf = new byte[1024];
                int readLen = 0;
                for (int i = 0; i < fileList.size(); i++) {
                    File f = (File) fileList.get(i);

                    if ((!filterSVN) || (
                            (!f.getName().equals(".svn")) && (!f.getName().equals(".temp")))) {
                        if (!f.isDirectory()) {
                            String name = f.getAbsolutePath();
                            name = FileUtil.normalizePath(name);
                            if (!name.startsWith(base)) {
                                return;
                            }
                            name = name.substring(base.length());
                            ze = new ZipEntry(name);
                            ze.setSize(f.length());
                            ze.setTime(f.lastModified());
                            LogUtil.info("Compressing:" + f.getPath());

                            if (f.isFile()) {
                                ze.setCompressedSize(640);
                                zos.putNextEntry(ze);
                                InputStream is = new BufferedInputStream(new FileInputStream(f));
                                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                                    zos.write(buf, 0, readLen);
                                }
                                is.close();
                            } else if (f.isDirectory()) {
                                ze.setCompressedSize(600);
                                zos.putNextEntry(ze);
                            }
                        }
                    }
                }
            }
        zos.close();
    }

    public static void zipBatch(String[] srcFiles, OutputStream destStream)
            throws Exception {
        File[] files = new File[srcFiles.length];
        for (int i = 0; i < srcFiles.length; i++) {
            files[i] = new File(srcFiles[i]);
        }
        zipBatch(files, destStream);
    }

    public static void zip(String srcFile, String destFile, LongTimeTask lt)
            throws Exception {
        OutputStream os = new FileOutputStream(destFile);
        zip(new File(srcFile), os, lt);
        os.flush();
        os.close();
    }

    public static void zip(String srcFile, String destFile)
            throws Exception {
        OutputStream os = new FileOutputStream(destFile);
        zip(new File(srcFile), os);
        os.flush();
        os.close();
    }

    public static void zip(File srcFile, OutputStream destStream)
            throws Exception {
        zip(srcFile, destStream, null);
    }

    public static void zip(File srcFile, OutputStream destStream, LongTimeTask ltt)
            throws Exception {
        zipBatch(srcFile.listFiles(), destStream, true);
    }

    public static void zipBatch(File[] srcFiles, OutputStream destStream)
            throws Exception {
        zipBatch(srcFiles, destStream, true);
    }

    public static void zipBatch(File[] srcFiles, OutputStream destStream, boolean filterSVN) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(destStream);
        for (int k = 0; k < srcFiles.length; k++)
            if (srcFiles[k].exists()) {
                List fileList = getSubFiles(srcFiles[k], filterSVN);
                ZipEntry ze = null;
                byte[] buf = new byte[1024];
                int readLen = 0;
                for (int i = 0; i < fileList.size(); i++) {
                    File f = (File) fileList.get(i);

                    ze = new ZipEntry(getAbsFileName(srcFiles[k], f));
                    ze.setSize(f.length());
                    ze.setTime(f.lastModified());
                    LogUtil.info("Compressing:" + f.getPath());

                    if (f.isFile()) {
                        ze.setCompressedSize(644);
                        zos.putNextEntry(ze);
                        InputStream is = new BufferedInputStream(new FileInputStream(f));
                        while ((readLen = is.read(buf, 0, 1024)) != -1) {
                            zos.write(buf, 0, readLen);
                        }
                        is.close();
                    } else if (f.isDirectory()) {
                        ze.setCompressedSize(755);
                        zos.putNextEntry(ze);
                    }
                }
            }
        zos.close();
    }

    public static byte[] unzip(byte[] bs) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(bs);
        bos = new ByteArrayOutputStream();
        Inflater inf = new Inflater();
        InflaterInputStream dis = new InflaterInputStream(bis, inf);
        byte[] buf = new byte[1024];
        try {
            int c;
            while ((c = dis.read(buf)) != -1) {
                bos.write(buf, 0, c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] r = bos.toByteArray();
        return r;
    }

    public static byte[] ungzip(byte[] bs) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(bs);
        bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            GZIPInputStream gis = new GZIPInputStream(bis);
            int c;
            while ((c = gis.read(buf)) != -1) {
                bos.write(buf, 0, c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] r = bos.toByteArray();
        return r;
    }

    public static void unzip(String srcFileName, String destPath)
            throws Exception {
        ZipFile zipFile = new ZipFile(srcFileName);
        Enumeration e = zipFile.entries();
        ZipEntry zipEntry = null;
        new File(destPath).mkdirs();
        while (e.hasMoreElements()) {
            zipEntry = (ZipEntry) e.nextElement();
            LogUtil.info("Uncompressing:" + zipEntry.getName());
            if (zipEntry.isDirectory()) {
                new File(destPath + File.separator + zipEntry.getName()).mkdirs();
            } else {
                File f = new File(destPath + File.separator + zipEntry.getName());
                f.getParentFile().mkdirs();
                InputStream in = zipFile.getInputStream(zipEntry);
                OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
                byte[] buf = new byte[1024];
                int c;
                while ((c = in.read(buf)) != -1) {
                    out.write(buf, 0, c);
                }
                out.close();
                in.close();
            }
        }
        zipFile.close();
    }

    //    /**
//     * 解压rar格式压缩包。
//     * 对应的是java-unrar-0.3.jar，但是java-unrar-0.3.jar又会用到commons-logging-1.1.1.jar
//     */
//    private static void unrar(String sourceRar,String destDir) throws Exception{
//        Archive a = null;
//        FileOutputStream fos = null;
//        try{
//            a = new Archive(new File(sourceRar));
//            FileHeader fh = a.nextFileHeader();
//            while(fh!=null){
//                if(!fh.isDirectory()){
//                    //1 根据不同的操作系统拿到相应的 destDirName 和 destFileName
//                    String compressFileName = fh.getFileNameString().trim();
//                    String destFileName = "";
//                    String destDirName = "";
//                    //非windows系统
//                    if(File.separator.equals("/")){
//                        destFileName = destDir + compressFileName.replaceAll("\\\\", "/");
//                        destDirName = destFileName.substring(0, destFileName.lastIndexOf("/"));
//                        //windows系统
//                    }else{
//                        destFileName = destDir + compressFileName.replaceAll("/", "\\\\");
//                        destDirName = destFileName.substring(0, destFileName.lastIndexOf("\\"));
//                    }
//                    //2创建文件夹
//                    File dir = new File(destDirName);
//                    if(!dir.exists()||!dir.isDirectory()){
//                        dir.mkdirs();
//                    }
//                    //3解压缩文件
//                    fos = new FileOutputStream(new File(destFileName));
//                    a.extractFile(fh, fos);
//                    fos.close();
//                    fos = null;
//                }
//                fh = a.nextFileHeader();
//            }
//            a.close();
//            a = null;
//        }catch(Exception e){
//            throw e;
//        }finally{
//            if(fos!=null){
//                try{fos.close();fos=null;}catch(Exception e){e.printStackTrace();}
//            }
//            if(a!=null){
//                try{a.close();a=null;}catch(Exception e){e.printStackTrace();}
//            }
//        }
//    }
    public static Map<String, Long> getFileListInZip(String zipFileName)
            throws Exception {
        ZipFile zipFile = new ZipFile(new File(zipFileName));
        Enumeration e = zipFile.entries();
        Map map = new HashMap();
        while (e.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) e.nextElement();
            if (!zipEntry.isDirectory()) {
                map.put(zipEntry.getName(), Long.valueOf(zipEntry.getSize()));
            }
        }
        zipFile.close();
        return map;
    }

    public static byte[] readFileInZip(String zipFileName, String fileName) {
        ZipFile zipFile = null;
        byte[] arrayOfByte1 = null;
        try {
            zipFile = new ZipFile(new File(zipFileName));
            Enumeration e = zipFile.entries();
            ZipEntry zipEntry = null;


            while (e.hasMoreElements()) {
                zipEntry = (ZipEntry) e.nextElement();
                if (!zipEntry.isDirectory()) {
                    InputStream in = zipFile.getInputStream(zipEntry);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int c;
                    while ((c = in.read(buf)) != -1) {
                        out.write(buf, 0, c);
                    }
                    out.close();
                    in.close();
                    arrayOfByte1 = out.toByteArray();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

            if (zipFile != null)
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return arrayOfByte1;
    }

    static List<File> getSubFiles(File baseDir, boolean filterSVN) {
        List arr = new ArrayList();
        arr.add(baseDir);
        if (baseDir.isDirectory()) {
            File[] tmp = baseDir.listFiles();
            for (File element : tmp)
                if ((!element.getName().equals(".svn")) && (!element.getName().equals(".temp"))) {
                    arr.addAll(getSubFiles(element, filterSVN));
                }
        }
        return arr;
    }

    static String getAbsFileName(File baseDir, File realFileName) {
        File real = realFileName;
        File base = baseDir;
        String ret = real.getName();
        if (real.isDirectory()) {
            ret = ret + "/";
        }

        while (real != base) {
            real = real.getParentFile();
            if (real == null) {
                break;
            }
            if (real.equals(base)) {
                ret = real.getName() + "/" + ret;
                break;
            }
            ret = real.getName() + "/" + ret;
        }

        return ret;
    }
}