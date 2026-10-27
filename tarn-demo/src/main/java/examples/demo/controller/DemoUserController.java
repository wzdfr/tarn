package examples.demo.controller;

import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.ERequestBody;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.ERequestParam;
import com.dfrdev.tarn.aop.annotation.Json;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.core.DaoException;
import com.dfrdev.tarn.json.JsonResult;
import examples.demo.entity.DemoUser;
import examples.demo.service.DemoUserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tarn 2.0 全链路示例控制器：新路由 + {@code @Json} + ORM + SqlBuilder + MyBatis。
 * <p>
 * <b>路由注册方式（Tarn 2.0+）</b>
 * <ul>
 *     <li>{@code @EController("/api/demo")} 声明控制器根路径，路径为<b>静态</b>（不含
 *     {@code ${mapping}} / {@code {id}}），因此会进入 {@code Router} 静态路由表，
 *     请求命中时 O(1) 直达，不再遍历所有控制器</li>
 *     <li>{@code @ERequestMapping("/users/page")} 拼接为完整路由 {@code /api/demo/users/page}</li>
 *     <li>方法映射含 {@code {id}} 时进入 {@code Router} <b>动态路由表</b>（正则匹配），
 *     控制器根路径含 {@code ${mapping}} 的老写法仍走老拦截器链，老项目行为不变；
 *     详见 {@code docs/Tarn-2.0-路由与独立运行使用指南.md}</li>
 * </ul>
 * <b>JSON 输出</b>：方法标注 {@code @Json} 或返回类型为 {@link JsonResult} 时，
 * 框架自动以 {@code application/json;charset=UTF-8} 输出，无需视图。</br>
 * <b>参数注入</b>：方法参数需标注 {@code @ERequestParam} / {@code @ERequestBody} /
 * {@code @ERequestHeader} 之一（未标注的参数框架无法解析）。</br>
 * <b>启用步骤</b>
 * <ol>
 *     <li>在 {@code context-mapping.xml} 的 {@code <controllers>} 中加入
 *     {@code <mapping package="examples.demo.controller" name="demo"/>}</li>
 *     <li>如需 MyBatis 分支：{@code application.yml} 中 {@code tarn.mybatis.enabled: true}</li>
 *     <li>建表 DDL 见类注释下方 / 白皮书「示例数据表」章节</li>
 * </ol>
 * <pre>
 * CREATE TABLE demo_dept (
 *     ID   BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     NAME VARCHAR(64) NOT NULL
 * );
 * CREATE TABLE demo_user (
 *     ID      BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     NAME    VARCHAR(64) NOT NULL,
 *     DEPTID  BIGINT,
 *     STATUS  INT DEFAULT 1,
 *     REMARK  VARCHAR(255)
 * );
 * </pre>
 *
 * @author dfr
 * @since 2.0
 */
@EController("/api/demo")
public class DemoUserController extends Controller {

    private final DemoUserService userService = new DemoUserService();

    // ==================== ORM：保存 / 更新 / 删除 / 查询 ====================

    /**
     * 新增用户（ORM 实体保存，JSON 请求体自动反序列化为实体）
     * <pre>
     * POST /api/demo/user/save
     * Content-Type: application/json
     * {"name":"张三","deptid":1,"status":1,"remark":"演示数据"}
     * </pre>
     */
    @Json
    @ERequestMapping(value = "/user/save", method = RequestMethod.POST)
    public JsonResult save(@ERequestBody DemoUser user) {
        if (user == null || user.getName() == null) {
            return JsonResult.fail(400, "name 不能为空");
        }
        List<Object> keys = userService.saveAndReturnKey(user);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("keys", keys);
        data.put("user", user);
        return JsonResult.ok(data);
    }

    /**
     * 更新用户（ORM 按主键更新，仅更新非 null 字段）
     * <pre>
     * POST /api/demo/user/update
     * {"id":1,"name":"张三-改","status":0}
     * </pre>
     */
    @Json
    @ERequestMapping(value = "/user/update", method = RequestMethod.POST)
    public JsonResult update(@ERequestBody DemoUser user) {
        if (user == null || user.getId() == null) {
            return JsonResult.fail(400, "id 不能为空");
        }
        userService.update(user);
        return JsonResult.ok();
    }

    /**
     * 删除用户（ORM 按主键删除）
     * <pre>POST /api/demo/user/delete?id=1</pre>
     */
    @Json
    @ERequestMapping(value = "/user/delete", method = RequestMethod.POST)
    public JsonResult delete(@ERequestParam("id") Long id) {
        if (id == null) {
            return JsonResult.fail(400, "id 不能为空");
        }
        DemoUser condition = new DemoUser();
        condition.setId(id);
        userService.delete(condition);
        return JsonResult.ok();
    }

    /**
     * 主键查询（ORM findById）
     * <pre>GET /api/demo/user/detail?id=1</pre>
     */
    @Json
    @ERequestMapping(value = "/user/detail", method = RequestMethod.GET)
    public JsonResult detail(@ERequestParam("id") Long id) {
        try {
            DemoUser user = userService.findById(id);
            return user == null ? JsonResult.fail(404, "用户不存在") : JsonResult.ok(user);
        } catch (DaoException e) {
            return JsonResult.fail("查询失败: " + e.getMessage());
        }
    }

    // ==================== SqlBuilder：参数化分页查询 ====================

    /**
     * 按状态分页（SqlBuilder 参数化 + 方言分页）
     * <pre>GET /api/demo/users/page?status=1&page=1&size=10</pre>
     */
    @ERequestMapping(value = "/users/page", method = RequestMethod.GET)
    public JsonResult page(@ERequestParam("status") Integer status,
                           @ERequestParam("page") Integer page,
                           @ERequestParam("size") Integer size) {
        try {
            int p = page == null || page < 1 ? 1 : page;
            int s = size == null || size < 1 ? 10 : size;
            List<DemoUser> list = userService.pageByStatus(status, p, s);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("page", p);
            data.put("size", s);
            data.put("total", userService.countByStatus(status));
            data.put("rows", list);
            return JsonResult.ok(data);
        } catch (DaoException e) {
            return JsonResult.fail("分页查询失败: " + e.getMessage());
        }
    }

    /**
     * 参数化更新状态（SqlBuilder + PreparedStatement 绑定）
     * <pre>POST /api/demo/user/status?id=1&status=0</pre>
     */
    @ERequestMapping(value = "/user/status", method = RequestMethod.POST)
    public JsonResult updateStatus(@ERequestParam("id") Long id,
                                   @ERequestParam("status") Integer status) {
        try {
            int rows = userService.updateStatus(id, status);
            return JsonResult.ok(rows);
        } catch (DaoException e) {
            return JsonResult.fail("更新失败: " + e.getMessage());
        }
    }

    // ==================== QueryBuilder：多表 JOIN ====================

    /**
     * 用户 + 部门联表分页（QueryBuilder 多表 JOIN，方言分页自动适配）
     * <pre>GET /api/demo/users/join?status=1&page=1&size=10</pre>
     */
    @ERequestMapping(value = "/users/join", method = RequestMethod.GET)
    public JsonResult joinPage(@ERequestParam("status") Integer status,
                               @ERequestParam("page") Integer page,
                               @ERequestParam("size") Integer size) {
        try {
            int p = page == null || page < 1 ? 1 : page;
            int s = size == null || size < 1 ? 10 : size;
            List<Map<String, Object>> rows = userService.pageWithDept(status, p, s);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("page", p);
            data.put("size", s);
            data.put("rows", rows);
            return JsonResult.ok(data);
        } catch (DaoException e) {
            return JsonResult.fail("联表查询失败: " + e.getMessage());
        }
    }

    // ==================== MyBatis 插件 ====================

    /**
     * MyBatis Mapper 查询（需 tarn.mybatis.enabled=true）
     * <pre>GET /api/demo/users/mybatis?status=1</pre>
     */
    @Json
    @ERequestMapping(value = "/users/mybatis", method = RequestMethod.GET)
    public JsonResult mybatisPage(@ERequestParam("status") Integer status) {
        try {
            List<DemoUser> list = userService.mybatisListByStatus(status == null ? 1 : status);
            return JsonResult.ok(list);
        } catch (IllegalStateException e) {
            // 未启用 MyBatis 插件时给出明确提示，而不是 500
            return JsonResult.fail(503, e.getMessage());
        }
    }

    /**
     * 演示 {@code @Json} 输出任意 POJO / Map（不限定 JsonResult）
     * <pre>GET /api/demo/ping</pre>
     */
    @Json
    @ERequestMapping(value = "/ping", method = RequestMethod.GET)
    public Map<String, Object> ping() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("framework", "Tarn 2.0");
        data.put("routing", "静态路由表 O(1)");
        data.put("time", System.currentTimeMillis());
        return data;
    }
}
