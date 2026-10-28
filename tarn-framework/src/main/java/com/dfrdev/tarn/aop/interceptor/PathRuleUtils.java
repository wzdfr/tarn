package com.dfrdev.tarn.aop.interceptor;

import com.dfrdev.tarn.util.text.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dfr on 14-9-28.
 */
public class PathRuleUtils {

    public String mappingRuleValue;
    public String path;


    public PathRuleUtils(String mappingRuleValue, String path) {
        this.mappingRuleValue = mappingRuleValue;
        this.path = path;
    }

    public Map conver(int length) {
        Map map = null;
        if (mappingRuleValue.indexOf("{") != -1) {
            path = path.substring(mappingRuleValue.indexOf("{"), path.length());
            mappingRuleValue = mappingRuleValue.substring(mappingRuleValue.indexOf("{") + 1, mappingRuleValue.length());
            map = new HashMap();
            for (int i = 0; i < length; i++) {
                calculate(map);
            }

        }

        return map;
    }


    private void calculate(Map map) {
        String val = "";
        String name = "";
        int e = mappingRuleValue.indexOf("}");
        if (mappingRuleValue.indexOf("{") == -1) {
            name = mappingRuleValue.substring(0, e);
            if (mappingRuleValue.length() > (name.length() + 1)) {
                String wb = mappingRuleValue.substring(e + 1);
                val = path.replaceAll(wb, "");
            } else {
                val = path;
            }
        } else {
            name = mappingRuleValue.substring(0, e);
            String sp = mappingRuleValue.substring(e + 1, mappingRuleValue.indexOf("{"));
            mappingRuleValue = mappingRuleValue.substring(mappingRuleValue.indexOf("{") + 1, mappingRuleValue.length());
            val = path.substring(0, path.indexOf(sp));
            path = path.substring(path.indexOf(sp) + 1);
        }
        if (StringUtil.isNotEmpty(name)) {
            if(name.indexOf("*")==-1){
                map.put(name, val);
            }else{
                map.put("INDEX_"+(map.size()+1),val);
            }

        }
    }

}
