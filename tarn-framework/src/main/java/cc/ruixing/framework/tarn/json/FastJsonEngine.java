package cc.ruixing.framework.tarn.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;

import java.util.List;

/**
 * 基于 fastjson 1.x 的默认 JSON 引擎实现。
 * <p>
 * 安全说明：初始化时会尽力开启 safeMode / 关闭 autoType（依赖 fastjson 版本能力，
 * 旧版本不支持时静默跳过）。建议将 WEB-INF/lib 中的 fastjson 升级到 1.2.83+，
 * 本引擎 API 不变，无需改代码。
 *
 * @author dfr
 * @since 2.0
 */
public class FastJsonEngine implements JsonEngine {

    private static volatile boolean hardened = false;

    /**
     * 尽力加固：禁用 autoType 反序列化（防 fastjson 历史漏洞利用链）
     */
    static void harden() {
        if (hardened) {
            return;
        }
        try {
            // fastjson 1.2.36+ 支持 safeMode；低版本通过反射探测，避免 NoSuchMethodError
            ParserConfig.class.getMethod("setSafeMode", boolean.class)
                    .invoke(ParserConfig.getGlobalInstance(), Boolean.TRUE);
        } catch (Throwable ignore) {
            // 低版本 fastjson 无此能力，升级 jar 后自动生效
        }
        hardened = true;
    }

    public String toJson(Object obj) {
        harden();
        return JSON.toJSONString(obj);
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        harden();
        return JSON.parseObject(json, clazz, Feature.IgnoreNotMatch);
    }

    public Object parse(String json) {
        harden();
        return JSON.parse(json);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> parseArray(String json, Class<?> elementClass) {
        harden();
        JSONArray array = JSON.parseArray(json);
        List<Object> list = JSONArray.parseArray(JSON.toJSONString(array), (Class) elementClass);
        return list;
    }

    /**
     * 快捷方式：解析为 JSONObject
     */
    public JSONObject parseObject(String json) {
        harden();
        return JSON.parseObject(json);
    }
}
