package cc.ruixing.framework.tarn.json;

import java.util.List;

/**
 * JSON 引擎抽象接口（Tarn 2.0+，前后端分离支持）。
 * <p>
 * 框架内置默认实现 {@link FastJsonEngine}。业务代码请统一通过 {@link JsonKit} 访问，
 * 不要直接依赖 fastjson API —— 未来升级 fastjson2 / Jackson 时业务零改动。
 *
 * @author dfr
 * @see JsonKit
 * @see FastJsonEngine
 * @since 2.0
 */
public interface JsonEngine {

    /**
     * 对象序列化为 JSON 字符串
     */
    String toJson(Object obj);

    /**
     * JSON 字符串反序列化为指定类型对象
     */
    <T> T fromJson(String json, Class<T> clazz);

    /**
     * JSON 字符串反序列化为 JSONObject / Map（不指定目标类型）
     */
    Object parse(String json);

    /**
     * JSON 字符串反序列化为指定元素类型的 List
     */
    List<Object> parseArray(String json, Class<?> elementClass);
}
