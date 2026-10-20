# mini-web-demo

一个**极小的 Java 网页服务**，专门用来做 CI/CD 流水线测试：构建 → 启动 → 访问网页 → 看到成功结果。

- 只依赖 JDK 自带的 `HttpServer`，**零第三方依赖**，不用 Spring
- 全部代码只有 **1 个 Java 文件**，jar 包约 6 KB
- 构建只需要执行 `mvn clean package`

---

## 一、要改的内容在哪个文件？

**只有这一个文件：**

```
src/main/java/com/example/demo/App.java
```

打开它，最上面就有这三行常量（在大约第 22~25 行）：

```java
// ==================== 改这里就能改网页内容 ====================
static final String APP_NAME = "mini-web-demo";
static final String VERSION  = "v1.0.0";
static final String MESSAGE  = "Hello CI/CD! 这是 Java 代码里写好的内容。";
// =============================================================
```

| 常量 | 作用 |
| --- | --- |
| `MESSAGE` | **网页上那个大字显示的内容**（就是你要改的东西） |
| `VERSION` | 版本号，网页上显示，也有单独的 `/version` 接口 |
| `APP_NAME` | 网页标题和表格里的应用名 |

> 改完之后重新打包、重新部署，网页上的内容就会变 —— 这就是你要的“版本控制效果”。
>
> 想验证得明显一点，建议每次改动同时把 `VERSION` 加一（`v1.0.1`、`v1.0.2`……），
> 这样在流水线里一眼就能看出这次部署的是哪一版。

### 想改成从配置文件读内容？（可选）
如果你希望不重新编译就能改内容，可以把 `MESSAGE` 改成从环境变量读，例如：

```java
static final String MESSAGE =
        System.getenv().getOrDefault("APP_MESSAGE", "Hello CI/CD! 这是默认内容。");
```

---

## 二、项目结构

```
mini-web-demo/
├── pom.xml                                  # Maven 打包文件
├── .gitignore
├── README.md
└── src/main/java/com/example/demo/App.java  # ★ 全部代码都在这里（要改的内容也在这里）
```

---

## 三、构建 & 运行

### 1. 打包
```bash
mvn clean package
```
产物：`target/mini-web-demo.jar`

### 2. 启动
```bash
java -jar target/mini-web-demo.jar
```
默认端口 **8080**，可以改：
```bash
java -jar target/mini-web-demo.jar          # 8080
PORT=9090 java -jar target/mini-web-demo.jar # Windows PowerShell: $env:PORT='9090'; java -jar ...
```

### 3. 访问
| 地址 | 返回 |
| --- | --- |
| http://localhost:8080/ | 网页（显示 `MESSAGE` 的内容） |
| http://localhost:8080/health | `OK`，给流水线做健康检查 |
| http://localhost:8080/version | `v1.0.0`，当前版本号 |

启动时控制台会打印（CI 日志里能直接看到）：
```
==================================================
应用名称 : mini-web-demo
版本号   : v1.0.0
输出内容 : Hello CI/CD! 这是 Java 代码里写好的内容。
启动时间 : 2026-10-20 08:42:00
Java 版本: 17.0.12
==================================================
网页地址 : http://localhost:8080/
```

---

## 四、在流水线里怎么串起来

```bash
mvn clean package                       # 1. 构建
java -jar target/mini-web-demo.jar &    # 2. 后台启动
sleep 3                                 # 3. 等它起来

curl -f http://localhost:8080/health    # 4. 健康检查，失败就让流水线红
curl -s http://localhost:8080/          # 5. 抓网页内容打印到流水线日志里

# 6. 校验内容对不对（内容被改坏时流水线直接报错）
curl -s http://localhost:8080/version | grep -q "v1.0.0" || exit 1
```

### 如果用 Docker（可选，需要时再加）
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/mini-web-demo.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## 五、附：本机没装 Maven 时怎么跑

只要有 **JDK 8 及以上**（`javac`、`jar` 命令）就能手工构建：

```bash
javac -encoding UTF-8 -d target/classes src/main/java/com/example/demo/App.java
jar --create --file target/mini-web-demo.jar --main-class com.example.demo.App -C target/classes .
java -jar target/mini-web-demo.jar
```

---

## 六、说明

- 编译目标为 Java 11 字节码，所以流水线机器上是 **JDK 8 / 11 / 17 / 21 都能编译**；
  只想用 17 就把 `pom.xml` 里的 `maven.compiler.release` 改成 `17`。
- 服务用的是 JDK 自带的 `HttpServer`，单线程、无连接池，**只适合做流水线和冒烟测试**，不要拿去做生产。
- 中文日志在 Linux/容器（UTF-8 环境）下正常；如果某个流水线日志显示乱码，
  启动时加上 `-Dstdout.encoding=UTF-8` 即可。
- 发布一版就改一次 `MESSAGE` + `VERSION`，然后 `git commit`，流水线跑完后对比网页内容就能看到版本变化。
