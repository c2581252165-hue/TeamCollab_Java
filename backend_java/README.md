# TeamCollab Java Backend

本后端项目是 team-collab 的 Java 升级版后端，基于 Spring Boot 2.7.18 构建，提供 RESTful API、JWT 认证鉴权、全局异常处理和基于 JPA 的 ORM 模型。

## 环境要求

- JDK 8
- Maven 3.6+
- MySQL 8.0

## 启动步骤

1. 在 MySQL 中创建 `teamcollab` 数据库。
2. 如果首次运行没有表，手动导入 `src/main/resources/schema.sql`。
3. 设置本地环境变量，真实值不要提交到 GitHub：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-database-password"
$env:JWT_SECRET="generate-a-long-random-secret"
$env:DEEPSEEK_API_KEY=""
```

4. 编译并启动：

```bash
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

后端服务默认运行在 `8000` 端口。

## 账号说明

公开仓库不内置默认账号和默认密码。请通过注册接口创建本地账号。