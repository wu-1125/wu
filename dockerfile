# 使用 Alpine + JRE 的轻量基础镜像（以 Java 17 为例）
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制本地已构建好的 JAR 包（假设位于 target 目录下）
COPY /wbs/jenkins-demo-app.jar /app/

# 暴露应用端口（根据实际修改）
EXPOSE 8088

# 启动命令，可添加 JVM 参数
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
