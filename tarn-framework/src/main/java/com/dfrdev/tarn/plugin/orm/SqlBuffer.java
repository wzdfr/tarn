package com.dfrdev.tarn.plugin.orm;

import com.dfrdev.tarn.collection.Mapx;

/**
 * Created by dfr on 15-12-29.
 */
public interface SqlBuffer {


    /**
     *获取SQL
     * */
    String getSql();
    /**
     *获取统计数量SQL
     * */
    String getCountSql();

    /**
     * 获取参数集合
     * */
    Mapx getParams();

    /**
     * 设置更新SQL
     * */
    void setSql(String sourceSql);

    /**
     * 当前SQL类型
     * */
    SqlType sqlType();
}
