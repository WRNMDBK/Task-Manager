# Task Manager · 任务记录管理

基于 Spring Boot 的任务管理后端项目，用于记录待办事项、查询任务进度、完成任务及保存操作记录。
目前提供邮箱验证码注册、JWT 登录鉴权和退出登录的 JSON 接口，默认通过 HTTPS 访问，可使用 Postman、Apifox 或 PowerShell 调用。项目不包含前端或分页；通过 RabbitMQ 异步发送邮件，使用 Redis 保存验证码和 JWT 黑名单。

## 技术栈

以下版本以仓库中的 `pom.xml` 为准。

| 技术 | 版本或用途 |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.1 |
| MyBatis Spring Boot Starter | 4.0.0 |
| MySQL | 用户、任务和操作记录持久化，使用 InnoDB |
| JJWT | 0.13.0，签发和校验 HS256 JWT |
| Spring Security Crypto | 7.2.0-M1，使用 BCrypt 校验密码 |
| Spring Data Redis | 4.2.0-M1，保存邮箱验证码和 JWT 黑名单 |
| Spring AMQP / RabbitMQ | 异步投递邮件任务 |
| Spring Mail | 通过 SMTP 发送验证码邮件 |
| Maven Wrapper | 构建、启动和执行测试 |
| Lombok | 生成构造方法、访问方法等代码 |
| Hibernate Validator | 通过 `spring-boot-starter-validation` 集成，使用 Jakarta Validation 注解校验参数 |
| JUnit Jupiter / Spring Boot Test | 测试依赖 |

## 功能

- 邮箱注册：发送六位验证码，有效期 5 分钟；验证成功后保存 BCrypt 密码哈希并返回 JWT。
- 用户登录：通过用户名和密码登录，使用 BCrypt 校验数据库中的密码哈希，成功后返回 JWT。
- 退出登录：将当前 Token 加入 Redis 黑名单，后续携带同一 Token 的受保护请求被拒绝。
- 接口鉴权：任务接口要求携带有效 JWT，校验通过后将用户 ID 和用户名保存到请求属性 `user` 中。
- 创建任务：保存标题、描述，初始状态为 `PENDING`，返回生成的 ID。
- 参数校验：使用 Hibernate Validator 校验创建任务的标题和描述，校验失败时返回具体提示。
- 查询任务：根据 ID 获取任务详情。
- 查询列表：支持按状态筛选，不传状态时查询全部，按 ID 升序排列。
- 完成任务：将 `PENDING` 改为 `DONE`，同时写入一条 `COMPLETE` 操作记录。
- 删除任务：删除任务本身，保留历史操作记录。
- 统一响应：使用 `code`、`message`、`data` 包装结果，集中处理异常。

当前代码与设计目标的差异见文末“待完善事项”。

当前已实现邮箱注册、登录和退出登录，尚未提供 Token 刷新接口。任务尚未按用户隔离：已登录用户可以访问和操作现有的全部任务。

## 项目结构

```text
Task-Manager/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/taskmanager/
    │   │   ├── TaskManagerApplication.java  # 启动入口及 Mapper 扫描
    │   │   ├── GlobalExceptionHandler.java # 统一异常处理
    │   │   ├── config/                    # WebMvcConfig 鉴权配置、RabbitMQConfig 队列配置
    │   │   ├── controller/                # HTTP 接口
    │   │   │   └── authorize/             # 登录、邮箱注册、退出登录接口
    │   │   ├── interceptor/               # JWT 请求鉴权
    │   │   ├── service/                   # 业务接口及实现
    │   │   ├── mapper/                    # 数据访问接口、注解 SQL
    │   │   ├── entity/dto/                # Task、TaskOperation、User、EmailMessageDTO
    │   │   ├── entity/vo/                 # CreateTaskVO、LoginVO、FirstRegisterVO、SecondRegisterVO
    │   │   ├── mqListener/                # 消费邮件任务并发送验证码
    │   │   ├── exception/                 # 业务异常
    │   │   └── utils/                     # JWT、BCrypt、响应封装及 enums 状态枚举
    │   └── resources/
    │       ├── application.yml
    │       └── mapper/TaskMapper.xml      # 列表动态 SQL、结果映射
    └── test/java/com/taskmanager/
        └── TaskManagerApplicationTests.java
```

请求流程：`Controller → Service → Mapper → MySQL`。依赖通过构造方法注入，连接和事务由 Spring 管理。当前 Mapper 同时使用注解 SQL 和 XML 映射。

受保护请求先经过 `AuthorizeInterceptor → JWTUtils` 检查 Redis 黑名单并校验 JWT，再进入 Controller。拦截器配置覆盖 `/**`，排除 `/login` 和 `/register/**`；`/logout` 需要鉴权。邮件任务经 `amq.direct` 交换机和 `email` 路由键进入 `email` 队列，由 `EmailListener` 发送，成功后手动确认，失败时拒绝且不重新入队。

## 本地运行

### 1. 准备环境

安装 JDK 25，配置 `JAVA_HOME`，并准备可连接的 MySQL、Redis、RabbitMQ 实例，以及可发送邮件的 SMTP 账号。项目自带 Maven Wrapper，无需单独安装 Maven；首次执行需要下载 Maven 和项目依赖。

下面的命令在项目根目录的 PowerShell 中执行。

### 2. 创建数据库和表

在 MySQL 客户端执行下面的初始化 SQL。这里以 `task_manager` 为数据库名；也可以使用其他名称，但需要与 `DB_URL` 一致。

仓库目前没有自动初始化脚本，需手动执行。以下结构根据设计书及现有 Mapper 整理，时间字段统一使用代码中的 `created_time`。

```sql
CREATE DATABASE IF NOT EXISTS task_manager
    DEFAULT CHARACTER SET utf8mb4;

USE task_manager;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_operation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

`task_record.status` 使用 `PENDING`（待完成）或 `DONE`（已完成）；`task_operation.action` 正常写入 `COMPLETE`。两表不设置外键，以便删除任务后保留操作记录。

推荐通过下文邮箱注册接口创建用户，无需手动插入记录。如需手动准备测试账号，`user.password` 必须保存 BCrypt 哈希，不能直接存明文。可在 IDEA 的 Evaluate Expression 中调用项目已有工具生成哈希（将参数替换为自己的密码，长度为 5～30 个字符）：

```java
new com.taskmanager.utils.BCryptUtils().encode("your_password")
```

将生成的完整哈希替换到下面的 SQL 中，再执行插入；用户名长度为 3～15 个字符：

```sql
INSERT INTO `user` (username, password, email)
VALUES ('demo', '<替换为生成的 BCrypt 哈希>', NULL);
```

### 3. 设置环境变量

`application.yml` 通过以下环境变量读取数据库、Redis、RabbitMQ、SMTP、JWT 和 HTTPS 配置：

| 环境变量 | 含义 |
| --- | --- |
| `DB_URL` | MySQL JDBC 连接地址，包含数据库名 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_KEY` | JWT 签名密钥，代码按 UTF-8 原文读取；HS256 至少需要 32 字节 |
| `JWT_EXPIRE_DAY` | Token 有效天数，设置为正整数，无默认值 |
| `SSL_KEY_STORE_PASSWORD` | 项目根目录 `taskmanager.p12` 的密码 |
| `REDIS_HOST` | Redis 地址，配置默认 `localhost` |
| `REDIS_PORT` | Redis 端口，建议显式设置为 `6379`，当前占位符默认值为空 |
| `REDIS_DB_NUM` | Redis 数据库编号，默认 `0` |
| `MQ_HOST` / `MQ_PORT` | RabbitMQ 地址和端口，默认 `localhost` / `5672` |
| `MQ_USERNAME` / `MQ_PASSWORD` | RabbitMQ 账号和密码，需有 `/` 虚拟主机的访问权限 |
| `MAIL_HOST` | SMTP 服务器地址 |
| `MAIL_USERNAME` | SMTP 登录账号，同时作为发件人地址 |
| `MAIL_PASSWORD` | SMTP 密码或邮箱授权码 |

在当前 PowerShell 终端设置，替换为自己的连接信息：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/task_manager?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf-8'
$env:DB_USERNAME = 'your_database_user'
$env:DB_PASSWORD = 'your_database_password'
$env:JWT_KEY = 'replace_with_your_random_secret_at_least_32_bytes'
$env:JWT_EXPIRE_DAY = '7'
$env:SSL_KEY_STORE_PASSWORD = 'your_keystore_password'
$env:REDIS_HOST = 'localhost'
$env:REDIS_PORT = '6379'
$env:REDIS_DB_NUM = '0'
$env:MQ_HOST = 'localhost'
$env:MQ_PORT = '5672'
$env:MQ_USERNAME = 'your_mq_user'
$env:MQ_PASSWORD = 'your_mq_password'
$env:MAIL_HOST = 'smtp.example.com'
$env:MAIL_USERNAME = 'sender@example.com'
$env:MAIL_PASSWORD = 'your_mail_authorization_code'
```

以上值均为占位示例，`JWT_KEY` 应替换为随机密钥。验证码存储、退出登录和受保护接口的黑名单检查依赖 Redis。当前 Redis 配置位于 `spring.redis`，运行时需核对配置是否被绑定，确保实际连接地址符合预期。SMTP 端口、认证及 TLS/SSL 选项需按邮箱服务商要求补充到配置中。

这些变量只对当前终端及其启动的进程生效。如果通过 IntelliJ IDEA 的运行按钮启动，请在 `TaskManagerApplication` 的运行配置中设置相同的环境变量。项目未配置自动加载 `.env` 文件。本地配置可保存在 `envionment_variables.env`，再由运行环境加载；该文件已被 `.gitignore` 忽略，不随仓库提交。

### 4. 启动服务

默认启用 HTTPS，启动前需在项目根目录准备 PKCS12 文件 `taskmanager.p12`，证书别名为 `taskmanager`，密码与 `SSL_KEY_STORE_PASSWORD` 一致。若尚无证书，可使用 JDK 自带的 `keytool` 生成本地开发证书（已有文件时不要重复生成）：

```powershell
keytool -genkeypair -alias taskmanager -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore taskmanager.p12 -validity 365 -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

按提示输入密钥库密码。客户端需信任此证书后再调用 HTTPS 接口。

```powershell
.\mvnw.cmd spring-boot:run
```

默认服务地址为 `https://localhost:8443`，先调用 `/login` 获取 Token，再访问 `/tasks`。项目没有首页，直接访问根路径不能用于判断接口是否正常。

也可以在配置好环境变量后，使用 IDEA 运行 `com.taskmanager.TaskManagerApplication`。

macOS / Linux 使用 `export` 设置同名环境变量，并通过 `./mvnw spring-boot:run` 启动。

## 接口说明

请求体使用 `Content-Type: application/json`。

登录和注册接口无需 Token；所有任务接口及退出登录接口都需要请求头 `Authorization: Bearer <token>`，其中 `<token>` 为登录或注册成功返回的 `data` 字符串。

**当前响应体中的 `code` 是业务码，不是实际 HTTP 状态码。** 当前 Controller 和异常处理器返回 `Result` 对象，正常处理及被处理器捕获的异常默认均返回 HTTP 200。例如，删除成功仍返回 JSON，其 `code` 为 204，并非 HTTP 204 空响应。

| 功能 | 方法和路径 | 参数 | 成功时的 `data` / `code` |
| --- | --- | --- | --- |
| 发送注册验证码 | `POST /register/send-code` | JSON：`username`、`email` | `null` / `204` |
| 确认注册 | `POST /register/confirm` | JSON：`username`、`password`、`email`、`code` | JWT 字符串 / `200` |
| 用户登录 | `POST /login` | JSON：`username`、`password` | JWT 字符串 / `200` |
| 退出登录 | `GET /logout` | 请求头 `Authorization: Bearer <token>` | `null` / `204` |
| 创建任务 | `POST /tasks` | JSON：`title`、`description` | 新任务 ID / `200` |
| 查询详情 | `GET /tasks/{id}` | 路径参数 `id` | 任务对象 / `200` |
| 查询列表 | `GET /tasks?status=PENDING` | 可选参数 `status` | 任务数组 / `200` |
| 完成任务 | `POST /tasks/{id}/complete` | 路径参数 `id` | `null` / `204` |
| 删除任务 | `DELETE /tasks/{id}` | 路径参数 `id` | `null` / `204` |

列表无匹配记录时返回空数组。当前实现中，不传 `status` 或传空字符串均查询全部；合法的状态值为 `PENDING` 和 `DONE`，但尚未主动拦截非法状态。

### 邮箱注册

先提交用户名和实际可收信的邮箱：

```http
POST /register/send-code
Content-Type: application/json

{
  "username": "demo",
  "email": "demo@example.com"
}
```

用户名必填，长度为 3～15 个字符；邮箱必填且需符合邮箱格式。用户名或邮箱已被使用时返回业务码 `409`。成功返回业务码 `204`，表示已提交邮件任务并保存验证码，邮件由后台异步发送。

收到邮件后，在 5 分钟内提交注册信息，使用相同的用户名和邮箱。验证码使用字符串，保留可能出现的前导零：

```http
POST /register/confirm
Content-Type: application/json

{
  "username": "demo",
  "password": "your_password",
  "email": "demo@example.com",
  "code": "012345"
}
```

密码必填，长度为 5～30 个字符；验证码必填且长度为 6。校验成功后创建用户，返回业务码 `200`，`data` 为 JWT，可直接访问任务接口。验证码保存在 Redis 的 `EmailCode:<email>` 中，重新发送会覆盖旧验证码并重新计时。

### 用户登录

```http
POST /login
Content-Type: application/json

{
  "username": "demo",
  "password": "your_password"
}
```

`username` 和 `password` 均不能为空或纯空白，长度分别为 3～15 和 5～30 个字符。登录使用 `LoginVO`，仅接收用户名和密码。

成功响应示例（Token 为占位值）：

```json
{
  "code": 200,
  "message": "请求成功，响应体包含结果",
  "data": "<JWT字符串>"
}
```

JWT 包含 `uid`、`username`、签发时间 `iat` 和过期时间 `exp`，使用 HS256 签名，有效期由 `JWT_EXPIRE_DAY` 决定。过期后需重新登录；退出登录通过 Redis 黑名单撤销当前 Token。

### 退出登录

```http
GET /logout
Authorization: Bearer <token>
```

成功返回 `data: null`、业务码 `204`（实际 HTTP 状态仍为 200），客户端应清除本地 Token。服务端将完整 Authorization 值保存到 `jwt:blackList:<Authorization>`，再次使用同一 Token 访问受保护接口会返回业务码 `401`。当前黑名单写入尚未设置过期时间，见文末待完善事项。

### 创建任务

创建请求通过 Controller 参数上的 `@Valid` 触发 `CreateTaskVO` 的字段校验：

| 字段 | 校验规则 | 校验失败提示 |
| --- | --- | --- |
| `title` | `@NotBlank`：不能为 `null`、空字符串或纯空白 | 标题不能为空 |
| `title` | `@Size(max = 100)`：最多 100 个字符 | 标题不能超过100个字符 |
| `description` | `@Size(max = 500)`：最多 500 个字符，可省略或为 `null` | 描述不能超过500个字符 |

```http
POST /tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "复习 MyBatis",
  "description": "练习动态 SQL 和事务回滚"
}
```

响应示例（ID 以实际生成值为准）：

```json
{
  "code": 200,
  "message": "请求成功，响应体包含结果",
  "data": 1
}
```

查询详情时，`data` 包含 `id`、`title`、`description`、`status`、`createdTime`；查询列表时，`data` 为这些对象组成的数组。

### 错误响应

创建任务的字段校验失败时，`GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException`，返回第一个字段错误的提示，并以 WARN 级别记录日志。例如，标题仅包含空格时返回：

```json
{
  "code": 400,
  "message": "标题不能为空",
  "data": null
}
```

此处 `400` 仍是业务码，实际 HTTP 状态为 200。多个约束同时校验失败时，仅返回其中第一个字段错误的提示。

例如，重复完成任务时返回：

```json
{
  "code": 409,
  "message": "任务不可重复完成",
  "data": null
}
```

登录和鉴权错误同样使用 `Result` 包装，以下 `code` 均为业务码，实际 HTTP 状态仍为 200：

| 情况 | `code` | `message` |
| --- | --- | --- |
| 用户不存在 | `400` | 该用户不存在，请重新输入用户名 |
| 密码不匹配 | `400` | 密码错误，登录失败 |
| 未提供 Authorization、请求头为空或缺少 `Bearer ` 前缀 | `401` | 未提供Token |
| Token 过期 | `401` | Token已过期 |
| Token 格式错误（捕获到 `MalformedJwtException`） | `401` | Token格式错误 |
| Token 签名错误 | `401` | Token签名错误 |
| 其他 JWT 异常（捕获到 `JwtException`） | `500` | Token解析失败 |

当前主要业务码为 `400`（字段校验或登录失败）、`401`（未通过鉴权）、`404`（任务不存在）、`409`（重复完成）和 `500`（系统异常）。JSON 格式错误、参数类型不匹配或其他运行时异常目前可能进入通用异常处理，返回业务码 `500`；列表 `status` 的取值校验尚未实现。

### PowerShell 调用示例

服务启动并信任本地 HTTPS 证书后，在另一个 PowerShell 终端执行。将登录信息替换为已注册的账号及其密码。示例会登录、创建、完成并删除一条练习任务，最后退出登录；完成记录会保留在数据库中。

```powershell
$baseUrl = 'https://localhost:8443'
$loginBody = @{ username = 'demo'; password = 'your_password' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/login" -ContentType 'application/json' -Body $loginBody
if ($login.code -ne 200) { throw $login.message }
$headers = @{ Authorization = "Bearer $($login.data)" }

$body = @{ title = 'Review MyBatis'; description = 'Practice transactions' } | ConvertTo-Json

# 创建任务，并获取实际生成的 ID
$created = Invoke-RestMethod -Method Post -Uri "$baseUrl/tasks" -Headers $headers -ContentType 'application/json' -Body $body
if ($created.code -ne 200) { throw $created.message }
$taskId = $created.data

# 查询详情、全部任务和待完成任务
Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Headers $headers
Invoke-RestMethod -Uri "$baseUrl/tasks" -Headers $headers
Invoke-RestMethod -Uri "$baseUrl/tasks?status=PENDING" -Headers $headers

# 完成任务
Invoke-RestMethod -Method Post -Uri "$baseUrl/tasks/$taskId/complete" -Headers $headers

# 删除任务
Invoke-RestMethod -Method Delete -Uri "$baseUrl/tasks/$taskId" -Headers $headers

# 退出登录，撤销当前 Token
Invoke-RestMethod -Method Get -Uri "$baseUrl/logout" -Headers $headers
```

## 事务设计与验证

`TaskServiceImpl.completeTask` 使用 `@Transactional`，依次执行：

1. 查询任务，不存在则抛出业务异常。
2. 使用 `WHERE id = ? AND status = 'PENDING'` 更新为 `DONE`，影响行数为 0 则抛出冲突异常。
3. 插入一条 `COMPLETE` 操作记录。
4. 正常结束后提交；运行时异常向外传播时回滚。

`BusinessException` 继承 `RuntimeException`。条件更新用于避免同一任务被重复完成；两张表使用同一数据源和 InnoDB 引擎。

设计书要求验证以下情况，当前仓库尚未提供对应的自动化断言或验证记录：

- 成功：任务变为 `DONE`，操作记录增加一条。
- 失败：在本地练习环境临时将操作记录的 `action` 设为 `null`，触发数据库非空约束异常；通过独立请求或数据库查询确认任务仍为 `PENDING`，且未新增操作记录。验证后恢复 `COMPLETE`。

验证失败回滚时，应在业务调用结束后独立查询数据库，避免仅依靠测试方法外层事务的自动回滚来判断。

## 测试与构建

在配置好数据库、Redis、RabbitMQ、SMTP、JWT 和 HTTPS 环境变量及证书后执行：

```powershell
# 执行现有测试
.\mvnw.cmd test

# 执行测试并打包
.\mvnw.cmd clean package

# 运行打包产物
java -jar .\target\Task-Manager-0.0.1-SNAPSHOT.jar
```

当前测试类使用 `@SpringBootTest`，包含 RabbitMQ 消息生产和消费示例，会连接消息代理；原生客户端示例使用本地 `5672` 端口及 `/test` 虚拟主机，与应用配置的 `/` 不同。当前尚未提供注册、登录、退出、接口行为和事务回滚的自动化断言。这些命令是运行说明，不代表仓库已经完成全部测试验证。

日志方面，业务异常和参数校验失败使用 WARN 记录消息，系统异常使用 ERROR 记录堆栈；MyBatis 当前通过 `StdOutImpl` 输出 SQL 调试信息。

## 待完善事项

对照设计书，后续主要完善以下内容：

校验列表状态只接受 `PENDING`、`DONE` 或不筛选，非法值返回参数错误。

补充包含任务 ID 和关键操作的业务日志。

补充验证码缺失或过期时的空值处理，注册成功后删除验证码，并限制发送及验证频率。

确认注册时重新校验用户名和邮箱唯一性，并完善数据库约束及并发处理。

补充注册、验证码失效和退出后 Token 被拒绝的自动化测试。

按用户关联任务并校验访问权限，实现任务数据隔离。
