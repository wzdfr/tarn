package com.dfrdev.tarn.core.db;


import com.dfrdev.tarn.util.date.DateUtil;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.text.TextKit;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.io.Serializable;
import com.dfrdev.tarn.core.TarnException;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.web.PageContext;
import java.util.*;

/**
 * 数据模型超类,它可以表示和容纳任何数据格式的容器
 */
public class DAO implements Serializable {
    /**
     * 对应数据库表名
     */
    String tableName = null;
    /**
     * 数据存放容器
     */
    private Map<String, Object> cm = new CaseInsensitiveMap<String, Object>();
    private List<String> useFields = new ArrayList<String>();
    private String primKey = null;
    private boolean isNull = true;
    private String recordNullDefaultValue = null;


    /**
     * 构造一个空的Dao
     */
    public DAO() {

    }

    /**
     * 通过表名构造一个Dao,随后就可以把这对象看成这张表的整体映射<br/>
     * 即可完成查询,删除，更新等操作
     *
     * @param tableName 数据库表名
     */
    public DAO(String tableName) {
        this.tableName = tableName;
    }

    /**
     * 获取字段值
     *
     * @param field 字段名称
     */
    public String get(String field) {
        //todo CaseInsensitiveMap 本身就是不区分大小写,数据放到freemarker数据集中区分大小写
        String fv = (String) cm.get(field);
        if (fv == null || fv.equals("") || fv.equals("null")) {
            if (field.equalsIgnoreCase("id")) {
                fv = newId();
                set("id", fv);
            } else {
                if (Tarn.isDebug) LogUtil.debug("[警告]：" + field + " 字段无效");
                fv = "";
            }
        }
        return fv;
    }

    public String getRecordNullDefaultValue() {
        return recordNullDefaultValue;
    }

    public void setRecordNullDefaultValue(String recordNullDefaultValue) {
        this.recordNullDefaultValue = recordNullDefaultValue;
    }

    /**
     * 获取Int类型字段值
     *
     * @param field 字段名称
     */
    public int getInt(String field) {
        return Integer.parseInt(get(field));
    }

    /**
     * 获取数据库字段值并格式化为千分位的数字字符串格式 （将保留两位小数，不足两位补0）. <br />
     * 示例：12345将返回：12,345.00
     *
     * @param field 要获取的字段的名称. 该字段必须是可以转化为double型数据的字段.
     * @return 格式化之后生成的字符串
     */
    @SuppressWarnings("unused")
    public String getNum(String field) {
        return TextKit.fmtNum(getDouble(field));
    }

    /**
     * 获取数据库字段的内容, 并截取前面的size个字符,如果字节长度超过size,则后面用省略号代替.
     * <br /><strong>特别说明：</strong><br />如果该字段含有HTML，此方法会先将内容转化为纯文本格式，然后再进行截取.
     *
     * @param field 要获取的字段的名称。
     * @param size  要截取的字符数（一个汉字当两个字符处理）
     * @return String 截取后的字符串。
     */
    public String get(String field, int size) {
        return TextKit.cut(getText(field), size);
    }

    /**
     * 获取数据库字段的内容.并以纯文本的形式返回. 即删除里面的html标签。
     *
     * @param field 要获取的数据库字段的名称。
     * @return 该字段的纯文本形式。
     */
    public String getText(String field) {
        return new TextKit(get(field)).replace("<(.|\n|\r)+?>", "").replace("\\s{2,}", " ").replace("&nbsp;", "").toString();
    }

    /**
     * 获取double类型字段值
     *
     * @param field 字段名称
     */
    public double getDouble(String field) {
        return Double.parseDouble(get(field));
    }

    public String getDate(String field, String style) {
        return TextKit.parseDate(get(field), style);
    }

    public DAO setDate(String field, String value) {
//        set(field, TextKit.parseDate(value, null));
        set(field, DateUtil.parseFull(value));
        return this;
    }

    /**
     * 设置指定的数据库字段的String值. 并返回此Dao对象. <br><br />
     *
     * @param field 要设置的字段名
     * @param value 要设置的字段值
     * @return 当前的Dat对象
     */
    public DAO set(String field, int value) {
        set(field, String.valueOf(value));
        return this;
    }

    /**
     * 数据设置
     *
     * @param field 字段名称
     * @param value 值
     * @return Dao
     */
    public DAO set(String field, long value) {
        return set(field, String.valueOf(value));
    }

    /**
     * 数据设置
     *
     * @param field 字段名称
     * @param value 值
     * @return Dao
     */
    public DAO set(String field, Object value) {
        cm.put(field.toUpperCase(), value);
        String us[] = new String[useFields.size()];
        useFields.toArray(us);
        if (!TextKit.containsIgnoreStr(us, field)) {
            useFields.add(field.toUpperCase());
        }
        return this;
    }

    /**
     * 数据设置
     *
     * @param field 字段名称
     * @param value 值
     * @return Dao
     */
    public DAO set(String field, double value) {
        return set(field, String.valueOf(value));
    }

    /**
     * 数据设置
     *
     * @param field 字段名称
     * @param value 值
     * @return Dao
     */
    public DAO set(String field, String value) throws TarnException {
        //todo CaseInsensitiveMap 本身就是不区分大小写,数据放到freemarker数据集中区分大小写
        if (recordNullDefaultValue!=null) {
            cm.put(field, StringUtil.isEmpty(value) ? recordNullDefaultValue : value);
        } else {
            cm.put(field, StringUtil.isEmpty(value) ? recordNullDefaultValue : value);
        }
        String us[] = new String[useFields.size()];
        useFields.toArray(us);
        if (!TextKit.containsIgnoreStr(us, field)) {
            useFields.add(field.toUpperCase());
        }
        return this;
    }

    /**
     * 删除一个已经设置的字段
     *
     * @param field 字段
     */
    public DAO deleteField(String field) {
        cm.remove(field);
        useFields.remove(field);
        return this;
    }

    /**
     * 将POST或GET过来的所有参数直接映射到当前的Dao中. <br />
     * 程序会自动将request.getParameterNames()
     * 中的参数名和当前的Dao对象的字段名进行匹配,并将符合匹配的的参数值映射到Dao对象中,未匹配的参数将忽略。<br />
     *
     * @return 当前的Dat对象
     * @throws
     */
    public DAO setAll() throws TarnException {
        PageContext pc = Tarn.getPageContext();
        Enumeration<String> reqs = pc.getMultipartRequest() == null
                ? pc.getRequest().getParameterNames()
                : pc.getMultipartRequest().getParameterNames();
        while (reqs.hasMoreElements()) {
            String n = reqs.nextElement();
            // if (containsIgnoreCase(this.cols, n)) {
            set(n, pc.get(n));
            //}
        }
        return this;
    }

    /**
     * 将POST或GET过来的，并且在fieldNames中出现的参数映射到当前的Dat中. <br />
     * 程序会自动将request.getParameterNames()
     * 中的参数名和当前的Dat对象的字段名fieldnames进行匹配,并将符合匹配的的参数值映射到Dat对象中,未匹配的参数将忽略。<br />
     * <strong>例如：</strong><br />//将POST或GET过来的 [ id, tid, uid, tel, fax, note ] 这些字段值映射到新的Dat对象中：<br />
     * new Dao(tableName).setAll("id, tid, uid, tel, fax, note, dt0");
     *
     * @param fieldNames 要匹配映射的字段列表，多个字段之间用逗号或空格分隔。
     * @return 当前的Dat对象
     * @see #setAll()
     */
    @SuppressWarnings("unchecked")
    public DAO setAll(String fieldNames) throws TarnException {
        String fn[] = fieldNames.replaceAll(" ", ",").split(",");
        PageContext pc = Tarn.getPageContext();
        Enumeration<String> reqs = pc.getMultipartRequest() == null
                ? pc.getRequest().getParameterNames()
                : pc.getMultipartRequest().getParameterNames();
        while (reqs.hasMoreElements()) {
            String n = reqs.nextElement();
            if (TextKit.containsIgnoreCase(useFields, n) && TextKit.containsIgnoreCase(fn, n)) {
                set(n, pc.get(n));
            }
        }
        return this;
    }


    /**
     * 获取整个Dao数据集合容器
     */
    public Map<String, Object> getCm() {
        return cm;
    }

    public void setCm(Map<String, Object> cm) {
        if (cm == null) return;
        Set<String> key = cm.keySet();
        Map map = new HashMap();
        for (Iterator it = key.iterator(); it.hasNext(); ) {
            String k = (String) it.next();
            map.put(k.toUpperCase(), cm.get(k));
            useFields.add(k.toUpperCase());
        }
        this.cm = map;
    }

//    public String[] getUseFields() {
//        return useFields;
//    }
//
//    public void setUseFields(String[] useFields) {
//        this.useFields = useFields;
//    }

    public List<String> getUseFields() {
        return useFields;
    }

    public void setUseFields(List<String> useFields) {
        this.useFields = useFields;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPrimKey() {
        return primKey;
    }

    public DAO setPrimKey(String primKey) {
        this.primKey = primKey.toUpperCase();
        return this;
    }

    /**
     * 判断当前的Dat对象在数据库中是否为空.
     *
     * @return true 或 false
     */
    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    /**
     * 一个简易的生成主键的方式，返回一个十位数的字符串 <br/>
     * 此种主键方式不推荐
     */
    public synchronized static String newId() throws TarnException {
        long t1 = System.currentTimeMillis();
        while (t1 == System.currentTimeMillis()) {
            t1 = System.currentTimeMillis();
        }
        String ident = Encoding("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRETYVWYZ".toCharArray(), t1);
        switch (ident.length()) {
            case 9:
                return "d" + ident;
            case 8:
                return "df" + ident;
            case 7:
                return "dfr" + ident;
            case 6:
                return "dfr0" + ident;
            default:
                throw new TarnException("对不起,服务器上的时间设置无效,请校正服务器的时间后重试.");
        }
    }

    private static String Encoding(char[] jz, long n) {
        long s = n / 61,
                y = n % 61;
        int sInt = (int) s,


                yInt = (int) y;
        if (s < 61) {
            return (sInt == 0 ? "" : String.valueOf(jz[sInt])) + jz[yInt];
        } else {
            return Encoding(jz, s) + jz[yInt];
        }
    }
}
