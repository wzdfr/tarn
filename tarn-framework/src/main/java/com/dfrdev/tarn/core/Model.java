package com.dfrdev.tarn.core;


/**
 * 数据查询语句构造对象
 * */
public class Model implements Cloneable {
    /**
     * 指定数据表名.
     */
    public String table = "TARN_DEF";
    /**
     * 指定要查询的字段,默认值为"*".
     */
    public String fields = "*";
    /**
     * 指定查询条件,如果不指定此属性或指定其值为空(null或空字符串"")，将查询所有记录，其默认值为null.
     */
    public String filter = null;
    /**
     * 如果filter 属性含有预编译语句，那么这个属性则是对应的值.请注意选后顺序
     */
    public Object[] prepareVal = null;

    /**
     * 设置每页显示的记录数,默认为0代表不分页.
     */
    public int pageSize = 0;
    /**
     * 设置当前页,默认值为1.
     */
    public int absPage = 1;
    //SELECT * from ceo where id=#id and uid=#uid
    //api.get(sql)
    //
    private String recordNullDefaultValue = null;
    public Model() {

    }

    public Model(String table, String filter) {
        this.table = table;
        this.filter = filter;
    }

    public Model(String table, String filter, String fields) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
    }

    public Model(String table, String filter, String fields, int pageSize) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
        this.pageSize = pageSize;
    }

    public Model(String table, String filter, String fields, int absPage, int pageSize) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
        this.absPage = absPage;
        this.pageSize = pageSize;
    }

    public Model(String table, String filter, String fields, int pageSize, Object[] prepareVal) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
        this.prepareVal = prepareVal;
        this.pageSize = pageSize;
    }

    public Model(String table, String filter, String fields, Object[] prepareVal) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
        this.prepareVal = prepareVal;
    }

    public Model(String table, String filter, String fields, int absPage, int pageSize, Object[] prepareVal) {
        this.table = table;
        this.filter = filter;
        this.fields = fields;
        this.absPage = absPage;
        this.pageSize = pageSize;
        this.prepareVal = prepareVal;
    }



    public String getRecordNullDefaultValue() {
        return recordNullDefaultValue;
    }

    public void setRecordNullDefaultValue(String recordNullDefaultValue) {
        this.recordNullDefaultValue = recordNullDefaultValue;
    }

    public Model(String table) {
        this.table = table;
    }

    /**
     * 转化SQL语句
     * */
    public String toSql() {
        return "SELECT " + fields + " FROM " + table + " WHERE " + filter;
    }

    @Override
    @SuppressWarnings("ALL")
    public Model clone() {
        try {
            return (Model) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Model();
        }
    }
}
