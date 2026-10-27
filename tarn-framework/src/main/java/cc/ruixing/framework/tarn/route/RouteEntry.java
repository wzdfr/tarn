package cc.ruixing.framework.tarn.route;

import cc.ruixing.framework.tarn.aop.ControllerInfo;
import cc.ruixing.framework.tarn.aop.ControllerMethodInfo;
import cc.ruixing.framework.tarn.aop.annotation.RequestMethod;

/**
 * 路由条目（Tarn 2.0+）：一条静态 URL → Controller 方法的映射。
 *
 * @author dfr
 * @since 2.0
 */
public class RouteEntry {

    private final ControllerInfo controllerInfo;
    private final String controllerClass;
    private final String methodName;       // null 表示走默认 execute()
    private final RequestMethod httpMethod; // AUTO 表示不限
    private final String mappingValue;      // 方法上的 @ERequestMapping 值（无则 null）
    private final ControllerMethodInfo methodInfo; // 方法元数据，用于参数注入（无参方法为 null）
    private final String pathTemplate;      // 完整路径模板（如 /api/demo/delete/{id}），路径变量解析用
    private final java.util.regex.Pattern pattern; // 动态路由正则（静态路由为 null）

    public RouteEntry(ControllerInfo controllerInfo, String methodName, RequestMethod httpMethod) {
        this(controllerInfo, methodName, httpMethod, null, null, null);
    }

    public RouteEntry(ControllerInfo controllerInfo, String methodName, RequestMethod httpMethod,
                      String mappingValue, ControllerMethodInfo methodInfo) {
        this(controllerInfo, methodName, httpMethod, mappingValue, methodInfo,
                methodInfo == null ? null : mappingValue);
    }

    public RouteEntry(ControllerInfo controllerInfo, String methodName, RequestMethod httpMethod,
                      String mappingValue, ControllerMethodInfo methodInfo,
                      String pathTemplate) {
        this(controllerInfo, methodName, httpMethod, mappingValue, methodInfo, pathTemplate, null);
    }

    public RouteEntry(ControllerInfo controllerInfo, String methodName, RequestMethod httpMethod,
                      String mappingValue, ControllerMethodInfo methodInfo,
                      String pathTemplate, java.util.regex.Pattern pattern) {
        this.controllerInfo = controllerInfo;
        this.controllerClass = controllerInfo.getValue();
        this.methodName = methodName;
        this.httpMethod = httpMethod;
        this.mappingValue = mappingValue;
        this.methodInfo = methodInfo;
        this.pathTemplate = pathTemplate;
        this.pattern = pattern;
    }

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public String getControllerClass() {
        return controllerClass;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * 方法上的 @ERequestMapping 值（如 "/users/page"），用于构造 ControllerMappingInfo
     */
    public String getMappingValue() {
        return mappingValue;
    }

    /**
     * 方法元数据（含参数类型与参数注解），无参方法为 null
     */
    public ControllerMethodInfo getMethodInfo() {
        return methodInfo;
    }

    /**
     * 完整路径模板（静态路由为精确路径，动态路由含 {id} 等占位符）
     */
    public String getPathTemplate() {
        return pathTemplate;
    }

    /**
     * 动态路由正则（静态路由为 null）
     */
    public java.util.regex.Pattern getPattern() {
        return pattern;
    }

    /**
     * 是否为动态路由
     */
    public boolean isDynamic() {
        return pattern != null;
    }

    /**
     * HTTP 方法约束是否满足（AUTO 恒通过）
     */
    public boolean matchesHttpMethod(String httpMethod) {
        return httpMethod == null
                || this.httpMethod == null
                || this.httpMethod == RequestMethod.AUTO
                || this.httpMethod.name().equalsIgnoreCase(httpMethod);
    }

    @Override
    public String toString() {
        return "RouteEntry{" + controllerClass + "#" + methodName + "@" + httpMethod + '}';
    }
}
