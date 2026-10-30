# mini-web-demo

Spring Boot 示例项目。业务代码不多，主要用来验证一条从代码提交到 Kubernetes 上线的 CI/CD 流程。

## 目录结构

```
mini-web-demo/
├── src/                   # Spring Boot 代码，/ 和 /health 两个接口
├── docker/
│   ├── Dockerfile         # 多阶段构建
│   └── docker-compose.yml # 本地起一套用
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
├── Jenkinsfile            # 流水线定义
├── pom.xml
└── README.md
```

## 流水线

在 Jenkins 上触发构建，可以选分支 / Tag 和环境。

| 步骤        | 做的事                                       |
| ----------- | -------------------------------------------- |
| 1. 拉取代码 | 按参数指定的分支或 Tag                       |
| 2. 构建     | `mvn clean verify`，跑单测并生成 jacoco 报告 |
| 3. 扫描     | SonarQube 静态扫描                           |
| 4. 质量门禁 | `waitForQualityGate`，不通过直接终止         |
| 5. 构建镜像 | 多阶段构建，tag 用 `构建号-commit`           |
| 6. 推送     | 推到 Harbor                                  |
| 7. 部署     | `kubectl set image`，滚动更新                |
| 8. 健康检查 | 集群内请求 `/health`，失败则 `rollout undo`  |

## 几点说明

**镜像 tag**

用的是 `${BUILD_NUMBER}-${GIT_COMMIT:0:7}`，比如 `42-a1b2c3d`。

不用 latest 是因为它每次覆盖，出了事想回滚，根本找不到上一版镜像是哪个。

**部署方式**

第一版是 ssh 登录 master 执行 kubectl，能跑通，但有几个问题：master 成了单点、用的 kubeconfig 是集群 admin、权限太大、也不走 API Server 审计。

后来给流水线单独建了个 ServiceAccount，权限用 Role 限制在 demo 命名空间，Jenkins 拿 kubeconfig 直连 apiserver，不用登录任何机器。

Role 里故意没给 `delete deployments`、`secrets`、`nodes`。这几个操作都试过，返回 Forbidden。

**质量门禁**

扫描不通过时，后面的构建镜像和部署都不会执行。这是特意验证过的：提交一段带硬编码密码的代码，流水线在门禁那一步就停了。

**健康检查**

没有只看 `rollout status`。它只能说明 Pod Ready，不代表接口真的能用。所以是起一个临时 curl Pod，从集群内部请求 `/health`，看是不是返回 200。

**镜像体积**

单阶段构建时大约 1.2G，改成多阶段 + JRE 基础镜像 + `.dockerignore` 后降到 380M 左右。

另外把 `COPY pom.xml` 和 `mvn dependency:go-offline` 放在 `COPY src` 前面，pom 没变就能命中缓存，构建时间从 6 分钟降到 90 秒左右。

## 本地运行

```bash
mvn clean verify
mvn spring-boot:run
curl http://127.0.0.1:8080/health
```

用 Docker：

```bash
# Dockerfile 在 docker/ 下，但构建上下文要用仓库根目录
docker build -f docker/Dockerfile -t mini-web-demo:local .
docker run -d -p 8080:8080 -e TZ=Asia/Shanghai mini-web-demo:local
```

## 部署到 K8s

```bash
kubectl create namespace demo

kubectl create secret docker-registry harbor-secret \
  --docker-server=harbor.local \
  --docker-username=jenkins \
  --docker-password='<Harbor密码>' \
  -n demo

kubectl apply -f k8s/deployment.yaml -n demo
kubectl apply -f k8s/service.yaml -n demo

kubectl get pod -n demo -w
```

`deployment.yaml` 里的镜像地址写的是占位符。第一次部署用 sed 替换后 apply，之后发版统一用 `kubectl set image`，避免 apply 把镜像改回去。

## Jenkins 配置

需要装的插件：SonarQube Scanner、Sonar Quality Gates、Docker Pipeline、Git Parameter、Credentials Binding。

需要的凭据：

| ID               | 类型        | 内容                             |
| ---------------- | ----------- | -------------------------------- |
| `harbor-cred`    | 用户名密码  | Harbor 推送账号                  |
| `k8s-kubeconfig` | Secret file | ServiceAccount 生成的 kubeconfig |
| `sonar-token`    | Secret text | SonarQube 的 User Token          |

都通过 `withCredentials` 注入，不写在脚本里。

## 踩过的坑

| 现象                                                | 原因                                                        | 解决                                          |
| --------------------------------------------------- | ----------------------------------------------------------- | --------------------------------------------- |
| 容器日志时间差 8 小时                               | 容器默认 UTC                                                | Dockerfile 里设 `TZ`，挂 `/etc/localtime`     |
| `COPY failed: file not found`                       | Dockerfile 在 docker/，构建上下文写成了 docker/             | `docker build -f docker/Dockerfile .`         |
| Jenkins 找不到流水线                                | 文件名写成了 `jenkinsfile`                                  | 改成 `Jenkinsfile`                            |
| 每次构建都要重下依赖                                | `COPY . .` 在 `RUN mvn` 之前                                | 先 COPY pom.xml 并 `dependency:go-offline`    |
| push 报 `server gave HTTP response to HTTPS client` | Harbor 是 HTTP                                              | 配 `insecure-registries` 后重启 docker        |
| Pod 一直 ImagePullBackOff                           | 镜像名错 / 没配 imagePullSecrets / 节点解析不了 harbor 域名 | 三个都查                                      |
| 覆盖率一直是 0%                                     | 用了 `mvn package`，没跑测试；或没配 jacoco                 | 改用 `mvn verify`，加 jacoco 插件             |
| `waitForQualityGate` 等到超时                       | SonarQube 没配 webhook                                      | 加 `http://<jenkins>:8080/sonarqube-webhook/` |
| 改了 YAML 发版没生效                                | image 写死，apply 又改回去                                  | 首次 apply，之后用 `set image`                |
| 回滚报 Forbidden                                    | Role 里没有 `patch deployments`                             | 补权限                                        |
| Pod OOMKilled                                       | JVM 没感知容器内存限制                                      | 加 `-XX:MaxRAMPercentage=75.0`                |

## 备注

凭据都放在 Jenkins Credentials 里，仓库里没有任何密码。

后续想做的是把裸 YAML 换成 Helm，这样回滚可以用 `helm rollback --atomic`。
