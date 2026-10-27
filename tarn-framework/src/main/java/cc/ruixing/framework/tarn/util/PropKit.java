package cc.ruixing.framework.tarn.util;


import cc.ruixing.framework.tarn.collection.Mapx;
import cc.ruixing.framework.tarn.aop.ControllerInfo;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public final class PropKit {
    private static Logger logger = Logger.getLogger(PropKit.class);

    /**
     * PropertiesHelper类的私有构造方法
     */
    private PropKit() {
    }

    /**
     * 读取一个inputStream 返回Mapx数据
     *
     * @param is
     */
    public static Mapx<String, String> read(InputStream is) {
        String text = FileUtil.readText(is, "UTF-8");
        return read(text);
    }

    public static Mapx<String, String> read(String text) {
        Mapx map = new Mapx();
        for (String str : StringUtil.splitEx(text, "\n")) {
            str = str.trim();
            if ((!ObjectUtil.empty(str)) && (!str.startsWith("#"))) {
                int index = str.indexOf("=");
                if (index >= 0) {
                    String k = str.substring(0, index).trim();
                    String v = StringUtil.rightTrim(str.substring(index + 1));
                    v = StringUtil.javaDecode(v);
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    public static Mapx<String, String> read(File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        try {
            return read(fis);
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void write(File f, Map<String, String> map) {
        FileOutputStream fis = null;
        try {
            fis = new FileOutputStream(f);
            write(fis, map);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write(OutputStream os, Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : map.entrySet()) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append((String) entry.getKey());
            sb.append("=");
            sb.append(StringUtil.javaEncode((String) entry.getValue()));
        }
        try {
            os.write(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 根据给定的资源文件的路径值，加载单个资源文件，返回Properties类对象的引用。若有异常产生，则Properties类对象的引用为null。
     *
     * @param filePath 资源文件的路径值
     * @return 给定资源文件所对应的Properties类对象的引用。
     */
    public static Properties loadPropertiesFile(String filePath) {
        Properties properties = new Properties();
        InputStream is = null;

        try {
            try {
                is = new FileInputStream(filePath);
                properties.load(is);
            } finally {
                if (is != null) {
                    is.close();
                    is = null;
                }
            }
        } catch (Exception e) {
            properties = null;
        }

        return properties;
    }


    /**
     * 向Properties 写入数据
     *
     * @param filePath 文件地址
     * @param list     名值对List
     *                 若文件不存在会创建此文件
     */
    public static void writeProperties(String filePath, List<ControllerInfo> list) throws IOException {
        String str = "程序自动生成的文件,该文件只是只读的。\r\n";
        str += "更改此文件不会对应用程序造成任何影响，它存在的意义只是为了让你能够直观的了解到你的应用部署EController（控制器）明细\r\n";
        str += "Time:" + TextKit.parseDate(null, null, true) + "\r\n";
        str += "Author By:ruixing DFR";
        writeProperties(filePath, list, str);
    }

    public static void writeProperties(String filePath, List<ControllerInfo> list, String comments) throws IOException {
        Properties prop = new Properties();
        File f = new File(filePath);
        if (!f.exists()) {
            f.createNewFile();
        }
        try {
            // f.mkdirs();
            InputStream fis = new FileInputStream(filePath);
            prop.load(fis);
            OutputStream fos = new FileOutputStream(filePath);
            for (ControllerInfo pin : list) {
                prop.setProperty(pin.getKey(), pin.getValue());
            }
            prop.store(fos, new String(comments.getBytes("UTF-8"), "ISO-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据多个资源文件的路径值，加载多个资源文件。
     *
     * @param filePaths 多个资源文件的路径值
     * @return 存放Properties类型引用的散列表，该散列表以资源文件的路径值为键。
     * @see cc.ruixing.framework.tarn.util.PropKit#loadPropertiesFile(String)
     */
    public static Map<String, Properties> loadPropertiesFiles(List<String> filePaths) {
        Map<String, Properties> propertiesMap = new HashMap<String, Properties>();

        for (String filePath : filePaths) {
            propertiesMap.put(filePath, loadPropertiesFile(filePath));
        }

        return propertiesMap;
    }

    /**
     * 根据给定的文件路径值，存放properties引用相关的资源文件到磁盘上。
     *
     * @param filePath   目标资源文件的路径值
     * @param properties Properties类型的引用
     * @return 操作成功，返回true；否则，返回false。
     * @see cc.ruixing.framework.tarn.util.PropKit#loadPropertiesFile(String)
     */
    public static boolean storePropertiesFile(String filePath, Properties properties) {
        return storePropertiesFile(filePath, getProperty(properties));
    }

    /**
     * 根据给定的文件路径值和欲存放的键值对，将目标资源文件存放到磁盘上。
     *
     * @param filePath    目标资源文件的路径值
     * @param propertyMap 所有键值对
     * @return 操作成功，返回true；否则，返回false。
     */
    public static boolean storePropertiesFile(String filePath, Map<String, String> propertyMap) {
        Properties properties = new Properties();
        FileWriter writer = null;

        try {
            try {
                writer = new FileWriter(filePath);

                for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                properties.store(writer, null);
            } finally {
                if (writer != null) {
                    writer.close();
                    writer = null;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 处理多个资源文件存放到磁盘上。
     *
     * @param filePaths    多个资源文件的路径值集合
     * @param propertyMaps 多个资源文件中的键值对集合
     * @return 操作成功，返回true；否则，返回false。
     * @see cc.ruixing.framework.tarn.util.PropKit#storePropertiesFile(String, java.util.Map)
     */
    public static boolean storePropertiesFiles(List<String> filePaths, List<Map<String, String>> propertyMaps) {
        int filePathSize = filePaths.size();

        if (filePathSize != propertyMaps.size()) {
            return false;
        }

        for (int i = 0; i < filePathSize; i++) {
            if (!storePropertiesFile(filePaths.get(i), propertyMaps.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获得给定键所关联的值的字符串形式。若键不存在，则返回给定的默认值。若properties为null，则返回null。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param properties   目标资源文件所对应的Properties类对象的引用
     * @return 给定键所关联的值的字符串形式
     */
    public static String getString(String key, String defaultValue, Properties properties) {
        return (properties == null) ? null : properties.getProperty(key, defaultValue);
    }

    /**
     * 获得给定键所关联的值的字符串形式。若键不存在，则返回给定的默认值。若properties为null，则返回null。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param properties   目标资源文件所对应的Properties类对象的引用
     * @return 给定键所关联的值的字符串形式
     */
    public static Integer getInteger(String key, Integer defaultValue, Properties properties) {
        String stringValue = (properties == null) ? null : properties.getProperty(key, defaultValue.toString());
        Integer value = null;

        if (stringValue == null) {
            return null;
        }

        try {
            value = Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * 获得给定键所关联的值的布尔形式。若键不存在，则返回给定的默认值。若键所对应的值不为true，则返回的是false。若properties为null，则返回null。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param properties   目标资源文件所对应的Properties类对象的引用
     * @return 给定键所关联的值的布尔形式
     */
    public static Boolean getBoolean(String key, Boolean defaultValue, Properties properties) {
        String stringValue = (properties == null) ? null : properties.getProperty(key, defaultValue.toString());
        return (stringValue == null) ? null : Boolean.valueOf(stringValue);
    }

    /**
     * 获得给定键所关联的值的浮点型形式。若键不存在，则返回给定的默认值。若properties为null，则返回null。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param properties   目标资源文件所对应的Properties类对象的引用
     * @return 给定键所关联的值的浮点型形式
     */
    public static Double getDouble(String key, Double defaultValue, Properties properties) {
        String stringValue = (properties == null) ? null : properties.getProperty(key, defaultValue.toString());
        Double value = null;

        if (stringValue == null) {
            return null;
        }

        try {
            value = Double.valueOf(stringValue);
        } catch (NumberFormatException e) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * 获得给定键所关联的值的日期形式。若键不存在，则返回给定的默认值。若properties为null，则返回null。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param properties   目标资源文件所对应的Properties类对象的引用
     * @return 给定键所关联的值的日期形式
     */
    public static Date getDate(String key, Date defaultValue, Properties properties) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String stringValue = (properties == null) ? null : properties.getProperty(key, sdf.format(defaultValue));
        Date value = null;

        if (stringValue == null) {
            return null;
        }

        try {
            value = sdf.parse(stringValue);
        } catch (ParseException e) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * 根据给定的若干个键和Properties类型的引用，获得资源文件中若干个指定的键值对。
     *
     * @param keys       若干个键
     * @param properties Properties类型引用
     * @return 若干个指定的键值对
     */
    public static Map<String, String> getProperty(List<String> keys, Properties properties) {
        Map<String, String> propertyMap = new HashMap<String, String>();

        for (String key : keys) {
            propertyMap.put(key, properties.getProperty(key));
        }

        return propertyMap;
    }

    /**
     * 根据给定的若干个键和资源文件的路径值，获得资源文件中若干个指定的键值对。该方法会调用getProperty(List, Properties)方法。
     *
     * @param keys     若干个键
     * @param filePath 资源文件的路径值
     * @return 若干个指定的键值对
     * @see cc.ruixing.framework.tarn.util.PropKit#getProperty(java.util.List, java.util.Properties)
     */
    public static Map<String, String> getProperty(List<String> keys, String filePath) {
        return getProperty(keys, loadPropertiesFile(filePath));
    }

    /**
     * 获得给定Properties类型引用所对应的资源文件中的所有键值对。
     *
     * @param properties Properties类型的引用
     * @return 资源文件中的所有键值对
     */
    public static Map<String, String> getProperty(Properties properties) {
        Map<String, String> propertyMap = new HashMap<String, String>();

        for (Iterator<Object> iter = properties.keySet().iterator(); iter.hasNext(); ) {
            String key = iter.next().toString();
            propertyMap.put(key, properties.getProperty(key));
        }

        return propertyMap;
    }

    /**
     * 根据给定的资源文件的路径，获得资源文件中的所有键值对。该方法会调用getProperty(Properties)方法。
     *
     * @param filePath 资源文件的路径值
     * @return 资源文件中的所有键值对
     * @see cc.ruixing.framework.tarn.util.PropKit#getProperty(java.util.Properties)
     */
    public static Map<String, String> getProperty(String filePath) {
        return getProperty(loadPropertiesFile(filePath));
    }

    /**
     * 增加属性文件值
     *
     * @param key
     * @param value
     */
    public static void addProperties(String key[], String value[], String file) {
        Properties iniFile = getProperties(file);
        FileOutputStream oFile = null;
        try {
            iniFile.put(key, value);
            oFile = new FileOutputStream(file, true);
            iniFile.store(oFile, "modify properties file");
        } catch (FileNotFoundException e) {
            logger.warn("do " + file + " FileNotFoundException:", e);
        } catch (IOException e) {
            logger.warn("do " + file + " IOException:", e);
        } finally {
            try {
                if (oFile != null) {
                    oFile.close();
                }
            } catch (IOException e) {
                logger.warn("do " + file + " IOException:", e);
            }
        }
    }

    //写入properties信息
    public static void writeProperties(String filePath, String parameterName, String parameterValue) throws IOException {
        Properties prop = new Properties();
        File f = new File(filePath);
        if (f.exists()) {
            f.createNewFile();
        }
        try {
            InputStream fis = new FileInputStream(filePath);
            //从输入流中读取属性列表（键和元素对）
            prop.load(fis);
            //调用 Hashtable 的方法 put。使用 getProperty 方法提供并行性。
            //强制要求为属性的键和值使用字符串。返回值是 Hashtable 调用 put 的结果。
            OutputStream fos = new FileOutputStream(filePath);
            prop.setProperty(parameterName, parameterValue);
            //以适合使用 load 方法加载到 Properties 表中的格式，
            //将此 Properties 表中的属性列表（键和元素对）写入输出流
            prop.store(fos, "Update '" + parameterName + "' value");
        } catch (IOException e) {
            System.err.println("Visit " + filePath + " for updating " + parameterName + " value error");
        }
    }

    //读取properties的全部信息
    public static void readProperties(String filePath) {
        Properties props = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            props.load(in);
            Enumeration en = props.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String Property = props.getProperty(key);
                LogUtil.debug(key + Property);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //根据key读取value
    public static String readValue(String filePath, String key) {
        Properties props = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            props.load(in);
            String value = props.getProperty(key);
            LogUtil.debug(key + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取配置文件
     *
     * @return
     */
    public static Properties getProperties(String file) {
        Properties pro = null;
        // 从文件mdxbu.properties中读取网元ID和模块ID信息
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            pro = new Properties();
            pro.load(in);

        } catch (Exception e) {
            logger.warn("Read " + file + " IOException:", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.warn("Read " + file + " IOException:", e);
            }
        }
        return pro;
    }

    /**
     * 保存属性到文件中
     *
     * @param pro
     * @param file
     */
    public static void saveProperties(Properties pro, String file) {
        if (pro == null) {
            return;
        }
        FileOutputStream oFile = null;
        try {
            oFile = new FileOutputStream(file, false);
            pro.store(oFile, "modify properties file");
        } catch (FileNotFoundException e) {
            logger.warn("do " + file + " FileNotFoundException:", e);
        } catch (IOException e) {
            logger.warn("do " + file + " IOException:", e);
        } finally {
            try {
                if (oFile != null) {
                    oFile.close();
                }
            } catch (IOException e) {
                logger.warn("do " + file + " IOException:", e);
            }
        }
    }

    /**
     * 修改属性文件
     *
     * @param key
     * @param value
     */
    public static void updateProperties(String key, String value, String file) {
        //key为空则返回
        if (key == null || "".equalsIgnoreCase(key)) {
            return;
        }
        Properties pro = getProperties(file);
        if (pro == null) {
            pro = new Properties();
        }
        pro.put(key, value);

        //保存属性到文件中
        saveProperties(pro, file);
    }
}
