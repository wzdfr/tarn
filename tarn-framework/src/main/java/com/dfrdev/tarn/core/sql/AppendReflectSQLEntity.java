package com.dfrdev.tarn.core.sql;

import java.util.List;
import java.util.Map;

public class AppendReflectSQLEntity {
    /**
     * 表名
     */
    private String tableName;
    /**
     * 字段集合
     */
    private List fileds;
    /**
     * 名值对
     */
    private Map valueKey;

    /**
     * 主键
     */
    private String pk;

    /**
     * 排序
     * */
    private String orderBy;


    public List getFileds() {
        return fileds;
    }

    public void setFileds(List fileds) {
        this.fileds = fileds;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map getValueKey() {
        return valueKey;
    }

    public void setValueKey(Map valueKey) {
        this.valueKey = valueKey;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public AppendReflectSQLEntity() {
    }

    public AppendReflectSQLEntity(List fileds, String tableName, Map valueKey) {
        this.fileds = fileds;
        this.tableName = tableName;
        this.valueKey = valueKey;
    }

    public AppendReflectSQLEntity(List fileds, String tableName, Map valueKey, String pk) {
        this.fileds = fileds;
        this.pk = pk;
        this.tableName = tableName;
        this.valueKey = valueKey;
    }

    public AppendReflectSQLEntity(Map valueKey, String tableName) {
        this.valueKey = valueKey;
        this.tableName = tableName;
    }

    public AppendReflectSQLEntity(Map valueKey, String tableName,String orderBy) {
        this.valueKey = valueKey;
        this.tableName = tableName;
        this.orderBy = orderBy;
    }

    public AppendReflectSQLEntity(String tableName, List fileds, Map valueKey, String pk, String orderBy) {
        this.tableName = tableName;
        this.fileds = fileds;
        this.valueKey = valueKey;
        this.pk = pk;
        this.orderBy = orderBy;
    }
}
