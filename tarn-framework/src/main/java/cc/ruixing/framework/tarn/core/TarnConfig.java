package cc.ruixing.framework.tarn.core;


import cc.ruixing.framework.tarn.util.StringUtil;

/**
 * 插件配置
 * User: dfr
 * Date: 13-12-13
 * Time: 下午5:06
 * 插件配置抽象接口
 */
public abstract class TarnConfig {

    /**
     * 配置Filters
     */
    public abstract void configFilter(Filters filters);

    /**
     * 配置插件
     */
    public abstract void configPlugin(Plugins me);


    public String getDevProperty(String key) {
        return Tarn.sysDevConf(key);
    }

    public String getProperty(String key, String propertiesFileName) {
        return Tarn.getConf(key, propertiesFileName);
    }


    public String get(String value, String defaultValue) {
        return (StringUtil.isNotEmpty(value))? value : defaultValue;
    }

    public Integer getInt(String value) {
        return (StringUtil.isNotEmpty(value))? Integer.parseInt(value) : null;
    }

    public Integer getInt(String value, Integer defaultValue) {
        return (StringUtil.isNotEmpty(value)) ? Integer.parseInt(value) : defaultValue;
    }

    public Long getLong(String value) {
        return (StringUtil.isNotEmpty(value)) ? Long.parseLong(value) : null;
    }

    public Long getLong(String value, Long defaultValue) {
        return (StringUtil.isNotEmpty(value)) ? Long.parseLong(value) : defaultValue;
    }

    public Boolean getBoolean(String value) {
        return (StringUtil.isNotEmpty(value)) ? Boolean.parseBoolean(value) : null;
    }

    public Boolean getBoolean(String value, Boolean defaultValue) {
        return (StringUtil.isNotEmpty(value)) ? Boolean.parseBoolean(value) : defaultValue;
    }
}
