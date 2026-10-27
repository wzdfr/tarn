package cc.ruixing.framework.tarn.json;

import java.util.List;

/**
 * JSON 统一门面（Tarn 2.0+）。
 * <p>
 * 框架内所有 JSON 序列化/反序列化统一收口到本类，业务代码示例：
 * <pre>
 *     String json = JsonKit.toJson(user);
 *     User user = JsonKit.fromJson(json, User.class);
 * </pre>
 * 更换底层引擎（如 fastjson2 / Jackson）只需调用一次 {@link #use(JsonEngine)}，业务零改动。
 *
 * @author dfr
 * @since 2.0
 */
public final class JsonKit {

    private static volatile JsonEngine engine = new FastJsonEngine();

    private JsonKit() {
    }

    /**
     * 更换 JSON 引擎（全局生效）
     */
    public static void use(JsonEngine newEngine) {
        if (newEngine != null) {
            engine = newEngine;
        }
    }

    public static JsonEngine engine() {
        return engine;
    }

    public static String toJson(Object obj) {
        return engine.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return engine.fromJson(json, clazz);
    }

    /**
     * 解析为 Map / JSONObject（不指定目标类型）
     */
    public static Object parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return engine.parse(json);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> parseArray(String json, Class<T> elementClass) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return (List<T>) engine.parseArray(json, elementClass);
    }
}
