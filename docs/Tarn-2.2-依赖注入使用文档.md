# Tarn 2.2 依赖注入使用文档

> 适用版本：Tarn 2.2+
> 核心实现：`com.dfrdev.tarn.container`、`com.dfrdev.tarn.aop.annotation`
> 本文档描述 Tarn 自研 IoC 容器的注解、配置、生命周期、循环依赖处理与使用边界。

---

## 1. 模块定位

Tarn 2.2 提供了一套**对标 Spring IoC 的轻量容器**，目标是：

- 容器管理的 Bean 全自动注入，无需再手写 `ServiceInject.inject(this)`；
- 具备 `ApplicationContext`、`BeanPostProcessor`、`@PostConstruct` 等生命周期能力；
- 通过三级缓存解决单例 Bean 的循环依赖；
- 保持与 Tarn 2.1 的 `@EService` / `@ERepository` / `@EAutowired` / `@EResource` 兼容。

与 Spring 的关系：

| 能力 | Tarn 2.2 | Spring |
|---|---|---|
| 容器 | `TarnApplicationContext` | `ApplicationContext` |
| 通用组件 | `@EComponent` | `@Component` |
| 服务 | `@EService` | `@Service` |
| 数据层 | `@ERepository` | `@Repository` |
| 注入 | `@EAutowired` / `@EResource` | `@Autowired` / `@Resource` |
| 初始化回调 | `@EPostConstruct` / `javax.annotation.PostConstruct` | `@PostConstruct` |
| 销毁回调 | `@EPreDestroy` / `javax.annotation.PreDestroy` | `@PreDestroy` |
| 后置处理器 | `BeanPostProcessor` | `BeanPostProcessor` |
| 循环依赖 | 三级缓存 | 三级缓存 |

> Tarn 容器与 Spring 容器相互独立。在 Spring Boot 项目中，Spring **不会**识别 `@EAutowired`，详见 [第 10 节](#10-与-spring-boot-共存)。

---

## 2. 核心概念

### 2.1 TarnApplicationContext

全局容器入口，单例，位于 [TarnApplicationContext.java](../tarn-framework/src/main/java/com/dfrdev/tarn/container/TarnApplicationContext.java)：

```java
TarnApplicationContext context = TarnApplicationContext.getInstance();
```

主要能力：

| 方法 | 说明 |
|---|---|
| `getBean(String name)` | 按名称获取，未注册抛 `TarnException` |
| `getBean(Class<T> type)` | 按类型获取唯一实例，0 个或多个候选时抛异常 |
| `getBean(String name, Class<T> type)` | 按名称获取并校验类型 |
| `containsBean(String name)` | 是否注册 |
| `isSingleton(String name)` | 是否单例 |
| `getBeanDefinitionNames()` | 全部 Bean 定义名称 |
| `getBeanNamesForType(Class<?>)` | 指定类型的 Bean 名称 |
| `getBeansOfType(Class<T>)` | 指定类型的 Bean 实例（名称 → 实例） |
| `getBeanDefinitionCount()` | Bean 定义数量 |
| `createBean(Class<?>)` | **新建**一个原型实例，完成注入与生命周期，不缓存 |
| `injectBean(Object)` | 对已创建对象执行注入与生命周期（兼容入口） |
| `addBeanPostProcessor(...)` | 注册后置处理器 |
| `close()` | 关闭容器，逆序执行销毁回调 |

### 2.2 Bean 定义与作用域

`BeanDefinition` 持有三个要素：

- **名称**：注入时按名称匹配；
- **实现类**：`Class` 或类名字符串；
- **作用域**：`SINGLETON`（默认）或 `PROTOTYPE`。

默认名称规则：`@EService` / `@ERepository` / `@EComponent` 的 `value()` 为空时，使用**首字母小写的简单类名**。例如 `BookServiceImpl` → `bookServiceImpl`。

### 2.3 注解一览

| 注解 | 目标 | 作用 |
|---|---|---|
| `@EService("name")` | 类 | 注册为单例服务 |
| `@ERepository("name")` | 类 | 注册为单例数据层组件 |
| `@EComponent("name")` | 类 | 注册为单例通用组件 |
| `@EAutowired("name")` | 字段 | 注入服务/组件/数据层 Bean |
| `@EResource("name")` | 字段 | 注入数据层 Bean（受 limitPackage 白名单约束） |
| `@EPostConstruct` | 方法 | 初始化回调（无参） |
| `@EPreDestroy` | 方法 | 销毁回调（无参） |

注解包路径：`com.dfrdev.tarn.aop.annotation`。

### 2.4 生命周期顺序

Bean 创建流程：

```text
实例化（无参构造）
  → 三级缓存暴露早期引用
  → 字段注入（@EAutowired / @EResource）
  → BeanNameAware / ApplicationContextAware
  → BeanPostProcessor.postProcessBeforeInitialization
  → @EPostConstruct / javax.annotation.PostConstruct
  → InitializingBean.afterPropertiesSet()
  → BeanPostProcessor.postProcessAfterInitialization
  → 放入一级缓存
```

容器关闭时按**创建顺序逆序**执行：

```text
@EPreDestroy / javax.annotation.PreDestroy
  → DisposableBean.destroy()
```

### 2.5 三级缓存与循环依赖

| 缓存 | 内容 | 作用 |
|---|---|---|
| 一级 `singletonObjects` | 完全初始化的单例 | 正常获取 |
| 二级 `earlySingletonObjects` | 提前暴露的原始实例 | 避免重复调用工厂 |
| 三级 `singletonFactories` | `ObjectFactory` | 实例化后、注入前注册，供循环依赖取早期引用 |

因为所有注入都是**字段注入**，Bean 在属性赋值前就已实例化并暴露早期引用，所以：

```text
A 创建 → 暴露 A 的早期引用 → 注入 B
  → B 创建 → 暴露 B 的早期引用 → 注入 A
    → 命中 A 的三级缓存，拿到早期 A → B 完成
  → A 完成
```

**无法解析**的循环（例如在构造方法或静态块中调用 `getBean`）会抛出：

```text
TarnException: 检测到构造期循环依赖（构造方法/静态块中获取 Bean）: xxx
```

原型 Bean 的循环会抛出：

```text
TarnException: 检测到原型 Bean 循环依赖: xxx
```

### 2.6 BeanPostProcessor

实现 `com.dfrdev.tarn.container.BeanPostProcessor`，可对每个 Bean 的初始化前后做增强：

```java
public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean, String beanName) { return bean; }
    default Object postProcessAfterInitialization(Object bean, String beanName) { return bean; }
}
```

注册方式：

1. 标注 `@EComponent` 并由容器扫描，`refresh()` 时自动实例化并加入处理器列表；
2. 手动调用 `TarnApplicationContext.getInstance().addBeanPostProcessor(new XxxProcessor())`。

返回 `null` 表示不替换原实例。

---

## 3. 快速开始

### 3.1 配置 `context-mapping.xml`

在 `src/main/resources/context-mapping.xml` 中声明扫描包：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<framework>
    <!-- 控制器包：必须 -->
    <controllers>
        <mapping package="com.example.controller" name="业务控制器"/>
    </controllers>

    <!-- 服务包：必须 -->
    <services>
        <mapping package="com.example.service.impl" name="服务实现"/>
    </services>

    <!-- 数据层包：可选；limitPackage 限制哪些包允许注入 Repository -->
    <repositorys limitPackage="com.example.service">
        <mapping package="com.example.repository.impl" name="数据层实现"/>
    </repositorys>

    <!-- 通用组件包：可选，Tarn 2.2 新增 -->
    <components>
        <mapping package="com.example.component" name="通用组件"/>
    </components>
</framework>
```

> `limitPackage` 为空或未配置时，所有包都允许注入 `@EResource`；配置后只有类名包含指定片段的类才能注入 Repository。

### 3.2 定义服务

```java
package com.example.service.impl;

import com.dfrdev.tarn.aop.annotation.EService;
import com.dfrdev.tarn.aop.annotation.EAutowired;

@EService("bookService")   // 不写 value 时默认名称为 bookServiceImpl
public class BookServiceImpl implements BookService {

    @EAutowired
    private UserService userService;   // 字段名 = Bean 名称

    @Override
    public String detail(Long id) {
        return userService.nameOf(id) + " 的书";
    }
}
```

### 3.3 定义通用组件

```java
package com.example.component;

import com.dfrdev.tarn.aop.annotation.EComponent;
import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EPostConstruct;

@EComponent
public class BookFacade {

    @EAutowired
    private BookService bookService;

    @EPostConstruct
    public void init() {
        // 此时 bookService 已注入完成
    }

    public String detail(Long id) {
        return bookService.detail(id);
    }
}
```

### 3.4 定义控制器

```java
package com.example.controller;

import com.dfrdev.tarn.aop.annotation.EAutowired;
import com.dfrdev.tarn.aop.annotation.EPostConstruct;
import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.Json;

@EController("/book")
public class BookController {

    @EAutowired
    private BookFacade bookFacade;

    @EPostConstruct
    public void init() {
        // 每个请求新建的控制器都会执行一次
    }

    @Json
    @ERequestMapping("/detail")
    public Object detail(Long id) {
        return bookFacade.detail(id);
    }
}
```

> Controller 以**原型作用域**由容器创建：每个请求一个新实例，并完整执行注入与生命周期，与 2.1 的“每请求新建”行为一致。

---

## 4. 注入详解

### 4.1 按名称注入

```java
@EAutowired
private BookService bookService;      // 解析 Bean 名称 bookService

@EAutowired("bookService")
private BookService anyName;          // 显式指定名称
```

### 4.2 名称未命中时按类型回退

当名称找不到定义时，容器会在同类型 Bean 中查找：

- **唯一候选**：自动注入；
- **多个候选**：输出告警并跳过，需用 `@EAutowired("name")` 明确指定；
- **没有候选**：输出告警并注入 `null`（不会启动失败，与 2.1 容错行为一致）。

### 4.3 Service 之间互相注入

```java
@EService("orderService")
public class OrderServiceImpl implements OrderService {

    @EAutowired
    private UserService userService;
}

@EService("userService")
public class UserServiceImpl implements UserService {

    @EAutowired
    private OrderService orderService;   // 与 OrderServiceImpl 形成循环依赖
}
```

上述循环由三级缓存自动解决。

### 4.4 注入数据层

```java
@EService
public class BookServiceImpl implements BookService {

    @EResource
    private BookRepository bookRepository;   // 受 limitPackage 约束
}
```

### 4.5 父类字段注入

容器会沿继承链收集字段（子类优先，同名父类字段被忽略）：

```java
public abstract class BaseComponent {
    @EAutowired
    protected UserService userService;
}

@EComponent
public class OrderFacade extends BaseComponent {
    // userService 同样会被注入
}
```

### 4.6 外部对象注入（兼容入口）

容器以外的对象（例如自己 `new` 出来的对象）不会被自动注入，这与 Spring 语义一致。需要时可显式处理：

```java
BookReportHelper helper = new BookReportHelper();
TarnApplicationContext.getInstance().injectBean(helper);
// 或使用兼容方法：
// ServiceInject.inject(helper);
```

> `injectBean` 每次调用都会重新执行注入和生命周期，请勿对同一个对象重复调用。

---

## 5. 生命周期示例

### 5.1 初始化与销毁

```java
@EComponent
public class ConnectionHolder {

    @EPostConstruct
    public void open() {
        // 依赖注入完成后初始化连接
    }

    @EPreDestroy
    public void close() {
        // 容器 close() 时释放资源
    }
}
```

也可以使用 JDK 注解（Tarn 按注解全名识别，兼容 `javax` 与 `jakarta`）：

```java
@EComponent
public class CacheWarmer {

    @javax.annotation.PostConstruct
    public void warmUp() {
        // ...
    }

    @javax.annotation.PreDestroy
    public void clear() {
        // ...
    }
}
```

### 5.2 InitializingBean / DisposableBean

```java
@EComponent
public class TaskScheduler implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 在 @PostConstruct 之后执行
    }

    @Override
    public void destroy() throws Exception {
        // 容器关闭时执行
    }
}
```

### 5.3 Aware 回调

```java
@EComponent
public class ContextHolder implements BeanNameAware, ApplicationContextAware {

    private String beanName;
    private ApplicationContext context;

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
}
```

### 5.4 手动关闭容器

```java
TarnApplicationContext.getInstance().close();
```

> 关闭会清空所有 Bean 定义与单例缓存，之后不能再获取 Bean。生产环境通常由框架生命周期管理，无需手动调用。

---

## 6. BeanPostProcessor 示例

```java
package com.example.component;

import com.dfrdev.tarn.container.BeanPostProcessor;
import com.dfrdev.tarn.aop.annotation.EComponent;

@EComponent
public class TimingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 初始化前
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 初始化后
        return bean;
    }
}
```

执行顺序：

```text
before(A) → @PostConstruct(A) → after(A) → before(B) → @PostConstruct(B) → after(B)
```

如需替换实例，可返回包装对象：

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof BookService) {
        return new BookServiceProxy((BookService) bean);
    }
    return bean;
}
```

> 注意：循环依赖中其他 Bean 拿到的是**原始早期引用**，不会拿到后置处理器替换后的实例（Tarn 未引入 AOP 代理，属已知简化）。

---

## 7. 编程式 API 示例

### 7.1 注册与获取

```java
TarnApplicationContext context = TarnApplicationContext.getInstance();

// 注册单例（名称为空时按类名推导）
context.registerSingleton("bookService", BookServiceImpl.class);
context.registerSingleton("", BookFacade.class);       // 默认名称 bookFacade

// 注册原型
context.registerPrototype("protoBean", ProtoBean.class);

// 直接按类型获取
BookFacade facade = context.getBean(BookFacade.class);

// 判断与遍历
boolean exists = context.containsBean("bookService");
Map<String, BookService> all = context.getBeansOfType(BookService.class);
String[] names = context.getBeanDefinitionNames();
```

### 7.2 创建一次性原型

```java
Object controller = context.createBean(BookController.class);
```

`createBean` 不注册定义、不缓存，适合“每次都要新实例”的场景。

### 7.3 独立容器（测试场景）

`TarnApplicationContext` 构造函数是公开的，可为测试创建隔离容器：

```java
TarnApplicationContext context = new TarnApplicationContext();
context.registerSingleton("serviceB", ServiceB.class);
context.registerSingleton("serviceA", ServiceA.class);

ServiceA a = context.getBean(ServiceA.class);
```

---

## 8. 配置项参考

### 8.1 `context-mapping.xml`

| 节点 | 必填 | 说明 |
|---|---|---|
| `<controllers>` | 是 | Controller 扫描包 |
| `<services>` | 是 | `@EService` 扫描包 |
| `<repositorys>` | 否 | `@ERepository` 扫描包；`limitPackage` 为注入白名单 |
| `<components>` | 否 | `@EComponent` 扫描包（Tarn 2.2 新增） |

### 8.2 启动流程

```text
Tarn 启动
  → 读取 context-mapping.xml
  → 扫描 @EController
  → 扫描 @EService（并初始化 Repository 扫描）
  → 扫描 @ERepository
  → 扫描 @EComponent
  → TarnApplicationContext.refresh()
       └─ 实例化所有 BeanPostProcessor
  → 开始接收请求
```

> 普通单例保持**惰性创建**（首次 `getBean` 时创建），避免启动阶段因外部依赖不可用而失败。

---

## 9. 边界与限制

### 9.1 只管理容器内的 Bean

- 自动注入仅作用于 `@EService` / `@ERepository` / `@EComponent` / Controller；
- 自己 `new` 出来的对象不会被注入（与 Spring 一致）；
- 需要注入时使用 `getBean` / `createBean` 获取对象，或调用 `injectBean` 兼容入口。

### 9.2 仅支持字段注入

- 不支持构造器注入、方法参数注入；
- Bean 必须有可访问的无参构造方法（`Class.newInstance()`）；
- 字段不能是 `final`（否则无法写入）。

### 9.3 作用域

- 支持 `SINGLETON` / `PROTOTYPE`；
- 没有 `@Scope` 注解，编程式注册时通过 `registerSingleton` / `registerPrototype` 指定；
- Controller 固定为原型作用域。

### 9.4 循环依赖

- 单例字段循环可解决；
- 构造方法、静态块中获取 Bean 形成的循环会抛出异常；
- 原型之间的循环会抛出异常；
- 循环引用拿到的是原始早期引用，不是后置处理器包装后的对象。

### 9.5 容错行为

- 依赖未注册时注入 `null` 并输出告警，不会阻止启动；
- 字段不可写时输出告警并跳过；
- Bean 名称冲突时保留先注册的定义并输出告警。不同 Service/Repository 请避免同名。

### 9.6 不支持的能力

当前版本**不提供**：

- `@Value` 配置注入；
- `@Qualifier` / `@Primary`；
- 构造器注入；
- AOP 代理与声明式事务；
- 父子容器 / 多容器层级；
- 条件装配（`@Conditional`）。

### 9.7 与旧版本兼容性

| 2.1 用法 | 2.2 行为 |
|---|---|
| `ServiceInject.inject(obj)` | 委托容器执行完整注入与生命周期 |
| `RepositoryInject.inject(obj)` | 同上 |
| `ComponentRegistry` / `newInstance` | 保留，仅按名称创建新实例，**不执行注入与生命周期** |
| `ComponentInjector` | 已删除，能力由容器接管 |

---

## 10. 与 Spring Boot 共存

Tarn 容器与 Spring 容器互不认识：

- Spring 不会识别 `@EAutowired`、`@EService`、`@EComponent`；
- Tarn 不会识别 Spring 的 `@Autowired`、`@Component`；
- `javax.annotation.PostConstruct` 两边都会识别，但**只有各自容器管理的 Bean 才会被对应容器回调**。

在 Spring Bean 中使用 Tarn 服务，推荐主动获取：

```java
@Component
public class SpringSideService {

    private BookService bookService;

    @javax.annotation.PostConstruct
    public void init() {
        // 从 Tarn 容器获取，Tarn 会完成该 Bean 的注入与生命周期
        this.bookService = TarnApplicationContext.getInstance().getBean(BookService.class);
    }
}
```

不建议在 Spring Bean 上同时使用 `@EAutowired` 并期望 Spring 注入。

---

## 11. 常见问题排查

### 11.1 注入结果为 null

依次检查：

1. 实现类是否标注 `@EService` / `@ERepository` / `@EComponent`；
2. 所在包是否在 `context-mapping.xml` 对应节点中声明；
3. 字段名是否与 Bean 名称一致（或使用 `@EAutowired("name")`）；
4. 是否存在多个同类型候选（查看告警日志）；
5. 使用 `@EResource` 时，类是否在 `limitPackage` 白名单内。

### 11.2 启动报“构造期循环依赖”

说明在构造方法或静态初始化中调用了 `getBean`。改为字段注入，或把获取 Bean 的逻辑移到 `@EPostConstruct` 中。

### 11.3 启动报“原型 Bean 循环依赖”

原型作用域无法用三级缓存解决，请拆解依赖或改为单例。

### 11.4 Bean 名称冲突

日志出现：

```text
[Tarn] Bean 名称冲突 'xxx'：A 与 B，保留前者
```

为其中一个组件显式指定不同的 `value()` 即可。

### 11.5 `@PostConstruct` 没执行

- 方法必须**无参**且非静态；
- 对象必须由容器创建（Controller / Service / Repository / Component）；
- 自己 `new` 的对象需要调用 `injectBean` / `createBean`。

---

## 12. 相关源码

| 文件 | 说明 |
|---|---|
| [ApplicationContext.java](../tarn-framework/src/main/java/com/dfrdev/tarn/container/ApplicationContext.java) | 容器接口 |
| [TarnApplicationContext.java](../tarn-framework/src/main/java/com/dfrdev/tarn/container/TarnApplicationContext.java) | 容器实现（三级缓存、注入、生命周期） |
| [BeanDefinition.java](../tarn-framework/src/main/java/com/dfrdev/tarn/container/BeanDefinition.java) | Bean 定义与作用域 |
| [BeanPostProcessor.java](../tarn-framework/src/main/java/com/dfrdev/tarn/container/BeanPostProcessor.java) | 后置处理器 |
| [EComponent.java](../tarn-framework/src/main/java/com/dfrdev/tarn/aop/annotation/EComponent.java) | 通用组件注解 |
| [EPostConstruct.java](../tarn-framework/src/main/java/com/dfrdev/tarn/aop/annotation/EPostConstruct.java) | 初始化回调注解 |
| [EPreDestroy.java](../tarn-framework/src/main/java/com/dfrdev/tarn/aop/annotation/EPreDestroy.java) | 销毁回调注解 |
| [ComponentAOP.java](../tarn-framework/src/main/java/com/dfrdev/tarn/aop/service/ComponentAOP.java) | 组件扫描 |
| [Inject.java](../tarn-framework/src/main/java/com/dfrdev/tarn/aop/Inject.java) | 启动扫描入口 |
| [RouteDispatcher.java](../tarn-framework/src/main/java/com/dfrdev/tarn/route/RouteDispatcher.java) | Controller 由容器创建 |
| [TarnApplicationContextTest.java](../tarn-framework/src/test/java/com/dfrdev/tarn/container/TarnApplicationContextTest.java) | 单元测试用例 |

---

## 13. 版本说明

- **Tarn 2.2**：新增 IoC 容器、`@EComponent`、`@EPostConstruct` / `@EPreDestroy`、`BeanPostProcessor`、Aware 接口、三级缓存循环依赖处理；Controller 改由容器创建；删除 `ComponentInjector`。
- **Tarn 2.1**：引入 `BeanProperties` 属性缓存，`@EAutowired` / `@EResource` 字段注入。
