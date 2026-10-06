# Jenkins Demo App

一个用于 Jenkins 流水线测试的简单 Node.js Web 应用。

## 项目结构

```
jenkins-demo-app/
├── src/
│   └── app.js          # Express 应用主文件
├── tests/
│   └── app.test.js     # 单元测试
├── Dockerfile          # Docker 构建文件
├── Jenkinsfile         # Jenkins 流水线配置
├── package.json        # 项目依赖和脚本
└── README.md
```

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/` | 欢迎页面 |
| GET | `/health` | 健康检查 |
| GET | `/api/info` | 应用信息 |
| POST | `/api/echo` | Echo 测试接口 |

## 本地运行

```bash
npm install
npm start
# 访问 http://localhost:3000
```

## 运行测试

```bash
npm test
```

## Jenkins 流水线阶段

1. **Checkout** - 拉取代码
2. **Install Dependencies** - 安装依赖
3. **Lint** - 代码检查
4. **Test** - 运行单元测试
5. **Build Docker Image** - 构建 Docker 镜像
6. **Deploy** - 部署（仅 main 分支）
