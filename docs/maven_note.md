# Maven 安装速查

> 完整集成说明见 [Tarn-2.0-Maven集成指南.md](./Tarn-2.0-Maven集成指南.md)，
> 合并版总手册见 [Tarn-2.1-项目方案与使用全书.md](./Tarn-2.1-项目方案与使用全书.md) §12。

## 本机开发环境（在框架工程根目录执行）

```bash
# 安装全部模块（parent + tarn-framework + starter + demo）
mvn -DskipTests install

# 只装核心框架（更快）
mvn -pl tarn-framework -am -DskipTests install
```

安装后 `~/.m2/repository` 中应存在：

```text
cc/ruixing/framework/tarn-parent/2.0.0/
cc/ruixing/framework/tarn-framework/2.0.0/
cc/ruixing/framework/tarn-spring-boot-starter/2.0.0/
```

> 注意：`mvn exec:java` 不带 `-am` 时会从 `~/.m2` 解析兄弟模块，
> 改了框架代码必须先 `install`（或用 `-am` 走 reactor）。

## 用本地 jar 安装到 Maven 仓库（无源码时）

```bash
# 安装主框架
mvn install:install-file \
  -Dfile=libs/tarn-framework-2.0.0.jar \
  -DgroupId=com.dfrdev \
  -DartifactId=tarn-framework \
  -Dversion=2.0.0 \
  -Dpackaging=jar

# 安装 Spring Boot starter
mvn install:install-file \
  -Dfile=libs/tarn-spring-boot-starter-2.0.0.jar \
  -DgroupId=com.dfrdev \
  -DartifactId=tarn-spring-boot-starter \
  -Dversion=2.0.0 \
  -Dpackaging=jar
```

> `artifactId` 必须与依赖坐标一致：主框架是 `tarn-framework`，不是 `tarn-parent`。
> 父 POM 仅框架工程内部使用，业务工程**不需要**安装或继承。
