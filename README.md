# Task Manager · 任务记录管理

基于 Spring Boot 的任务管理后端项目，用于记录待办事项、查询任务进度、完成任务及保存操作记录。
目前提供邮箱验证码注册、JWT 登录鉴权、退出登录和 IP / UID 双维度限流的 JSON 接口，默认通过 HTTPS 访问，可使用 Postman、Apifox 或 PowerShell 调用。项目不包含前端或分页；通过 RabbitMQ 异步发送邮件，使用 Redis 保存验证码、JWT 黑名单和限流计数。限流由两套注解、拦截器和 Redis Lua 脚本实现，脚本分别位于 `src/main/resources/scripts/rate-limit-ip.lua` 和 `src/main/resources/scripts/rate-limit-uid.lua`。

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
| Spring Data Redis | 4.2.0-M1，保存邮箱验证码、JWT 黑名单和限流计数 |
| Redis Lua 脚本 | 通过 `DefaultRedisScript` 分别执行 `scripts/rate-limit-ip.lua` 和 `scripts/rate-limit-uid.lua`，保证每个维度内的计数与封禁判断原子执行 |
| Spring AMQP / RabbitMQ | 异步投递邮件任务 |
| Spring Mail | 通过 SMTP 发送验证码邮件 |
| Maven Wrapper | 构建、启动和执行测试 |
| Lombok | 生成构造方法、访问方法等代码 |
| Hibernate Validator | 通过 `spring-boot-starter-validation` 集成，使用 Jakarta Validation 注解校验参数 |
| JUnit Jupiter / Spring Boot Test | 测试依赖 |

## 功能

- 邮箱注册：发送六位验证码，有效期 5 分钟；验证成功后保存 BCrypt 密码哈希并返回 JWT。
- 用户登录：通过用户名和密码登录，使用 BCrypt 校验数据库中的密码哈希，成功后返回 JWT。
- 退出登录：将当前 Token 的唯一标识 `jti` 写入 Redis（`jwt:logout:{jti}`），过期时间取该 Token 的剩余有效期，不再保存完整 Token 且不会残留永不过期的键；后续携带同一 Token 的受保护请求被拒绝。
- 接口鉴权：任务接口要求携带有效 JWT，校验通过后将用户 ID 和用户名保存到请求属性 `user` 中。
- 接口限流：通过 `@ipRateLimit` 和 `@uidRateLimit` 为接口分别声明 IP、UID 限流规则，支持 `STRICT`、`NORMAL`、`LOOSE` 三档阈值。IP 限流先于鉴权执行，UID 限流在鉴权后按用户 ID 执行；任一维度超限均返回业务码 `429`。
- 创建任务：保存标题、描述，初始状态为 `PENDING`，返回生成的 ID。
- 参数校验：使用 Hibernate Validator 校验创建任务的标题和描述，以及查询列表时 `status` 的取值，校验失败时返回具体提示。
- 查询任务：根据 ID 获取任务详情。
- 查询列表：支持按状态筛选，不传状态时查询全部，按 ID 升序排列。
- 完成任务：将 `PENDING` 改为 `DONE`，同时写入一条 `COMPLETE` 操作记录。
- 删除任务：删除任务本身，保留历史操作记录。
- 统一响应：使用 `code`、`message`、`data` 包装结果，集中处理异常。

当前代码与设计目标的差异见文末“待完善事项”。

当前已实现邮箱注册、登录和退出登录，尚未提供 Token 刷新接口。任务尚未按用户隔离：已登录用户可以访问和操作现有的全部任务。

### 接口限流

两个限流维度分别计数、分别封禁：

- **IP 维度**：使用 `request.getRemoteAddr()` 获取客户端地址，在 JWT 鉴权前限制同一 IP 对同类接口的请求总量，未登录请求也会计数。
- **UID 维度**：从鉴权后写入的请求属性 `user`（`LoginUserInfo`）读取 `uid`，限制同一用户对同类接口的请求总量。同一用户使用不同 Token 或不同 IP 时仍共享 UID 额度。

任务接口同时启用两个维度，必须都通过才能进入 Controller；登录和注册接口只启用 IP 限流。

实现位于 `src/main/java/com/taskmanager/rateLimit/`，下表中的 Java 路径相对于该目录（`adpter` 为当前包名）：

| 组成 | 位置 | 职责 |
| --- | --- | --- |
| `@ipRateLimit` / `@uidRateLimit` | `adpter/annotation/` | 在 Controller 方法上声明请求类型和限流档位 |
| `IpLimitLevel` / `UidLimitLevel` / `RequestType` | `adpter/annotation/enums/` | 分别管理两个维度的阈值，以及共享的接口分类 |
| `ipLimitInterceptor` / `uidLimitInterceptor` | `adpter/interceptor/` | 按拦截器顺序调用对应限流器 |
| `ipLimiter` / `uidLimiter` | `infrastructure/` | 读取注解、获取 IP 或 UID、拼接 Redis 键、执行脚本并判断结果 |
| `RedisScriptsConfig` | `infrastructure/RedisScriptsConfig.java` | 注册名为 `rate-limit-ip`、`rate-limit-uid` 的两个脚本 Bean，由 `@Qualifier` 区分注入 |
| IP / UID Lua 脚本 | `src/main/resources/scripts/rate-limit-ip.lua`、`src/main/resources/scripts/rate-limit-uid.lua` | 在 Redis 中执行对应维度的计数、过期和封禁逻辑 |

两个注解都只作用于方法，参数如下：

| 参数 | 含义 |
| --- | --- |
| `type` | 必填，取值为 `RequestType`，用于区分接口类别，也是 Redis 键中的接口标识 |
| `level` | 限流档位：IP 注解使用 `IpLimitLevel`，UID 注解使用 `UidLimitLevel`，均默认 `NORMAL` |

每个档位包含 `count`（窗口内允许的请求数）、`windowSecond`（窗口秒数）、`banSecond`（超限封禁秒数），不再逐个接口填写数值。当前配置以 `IpLimitLevel.java` 和 `UidLimitLevel.java` 为准：

| 档位 | IP 请求数 / 窗口 | IP 封禁时长 | UID 请求数 / 窗口 | UID 封禁时长 |
| --- | --- | --- | --- | --- |
| `STRICT` | 30 次 / 60 秒 | 300 秒 | 3 次 / 60 秒 | 300 秒 |
| `NORMAL` | 60 次 / 60 秒 | 180 秒 | 10 次 / 60 秒 | 180 秒 |
| `LOOSE` | 120 次 / 60 秒 | 60 秒 | 20 次 / 60 秒 | 60 秒 |

当前接口配置：

| 接口 | `type` | IP 档位 | UID 档位 |
| --- | --- | --- | --- |
| `POST /login` | `AUTHORIZE` | `STRICT` | 未启用 |
| `POST /register/send-code` | `AUTHORIZE` | `STRICT` | 未启用 |
| `POST /register/confirm` | `AUTHORIZE` | `STRICT` | 未启用 |
| `POST /tasks` | `CREATE_TASK` | `NORMAL` | `NORMAL` |
| `GET /tasks/{id}` | `GET_TASK_MESSAGE` | `LOOSE` | `LOOSE` |
| `GET /tasks` | `GET_TASK_LIST` | `LOOSE` | `LOOSE` |
| `POST /tasks/{id}/complete` | `COMPLETE_TASK` | `NORMAL` | `NORMAL` |
| `DELETE /tasks/{id}` | `DELETE_TASK` | `NORMAL` | `NORMAL` |
| `GET /logout` | — | 未启用 | 未启用 |

例如，查询任务详情的方法同时声明：

```java
@ipRateLimit(type = RequestType.GET_TASK_MESSAGE, level = IpLimitLevel.LOOSE)
@uidRateLimit(type = RequestType.GET_TASK_MESSAGE, level = UidLimitLevel.LOOSE)
```

额度按 `type` 分组，而不是按完整 URL 或 Token 分组。登录、发送验证码和确认注册共用 `AUTHORIZE`，因此同一 IP 共享 30 次 / 60 秒的额度；查询不同任务 ID 也共享 `GET_TASK_MESSAGE` 额度。未标注某一维度注解的方法会跳过该维度检查，注解中的默认 `NORMAL` 不会自动应用于未标注的方法；`/logout` 跳过两个限流检查，但仍需鉴权。

Redis 键名如下，IP 和 UID 的计数、封禁键相互独立：

```text
limit:ip:count:{type}:{ip}        # IP 请求计数，TTL = windowSecond
limit:ip:blacklist:{type}:{ip}    # IP 封禁标记，TTL = banSecond
limit:uid:count:{type}:{uid}      # UID 请求计数，TTL = windowSecond
limit:uid:blacklist:{type}:{uid}  # UID 封禁标记，TTL = banSecond
```

`{type}` 使用 `RequestType.getType()` 返回的字符串：`authorize`、`createTask`、`getTaskMessage`、`getTaskList`、`completeTask`、`deleteTask`。例如，用户 42 查询任务详情的计数键为 `limit:uid:count:getTaskMessage:42`。

两个脚本采用相同的固定窗口加封禁逻辑，分别接收对应维度的键和阈值：

1. `EXISTS` 黑名单键，命中则直接返回 `false`（拒绝）；
2. `INCR` 计数键；
3. 计数为 1（新窗口）**或** 键的 TTL 小于 0 时执行 `EXPIRE`，设置 `windowSecond` 秒的有效期；
4. 计数大于 `count` 时 `SET` 黑名单键并附带 `EX banSecond`，返回 `false`；
5. 其余情况返回 `true`（放行）。

窗口从首次计数开始，不按自然分钟对齐；已有有效 TTL 的计数键不会被后续请求续期。TTL 小于 0 的分支用于补齐缺失的过期时间。计数等于 `count` 时仍放行，第 `count + 1` 次触发封禁；封禁期间再次请求会直接返回，不延长封禁时间。

两个脚本分别以 `Boolean` 为返回类型注册为单例 Bean。限流器使用 `!Boolean.TRUE.equals(result)` 判断拒绝，只有结果为 `true` 才放行。Spring Data Redis 默认先按脚本 SHA1 执行 `EVALSHA`，脚本未缓存时回退到 `EVAL`，详见 [Spring Data Redis 脚本执行说明](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html)。

每个 Lua 脚本将检查、计数、设置过期时间和封禁放在一次原子执行中，避免这些步骤之间被其他请求插入。IP 和 UID 检查是先后发生的两次脚本调用：IP 已计数后，即使后续鉴权失败、UID 超限或业务处理失败，也不会回退已消耗的 IP 额度；IP 检查未通过时，不会继续执行 UID 检查。

## 项目结构

```text
Task-Manager/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/taskmanager/
    │   │   ├── TaskManagerApplication.java  # 启动入口及 Mapper 扫描
    │   │   ├── identity/                   # 注册、登录、退出与鉴权
    │   │   │   ├── adapter/                # web 接口、请求 DTO、AuthorizeInterceptor
    │   │   │   ├── application/            # AuthorizeService 及实现
    │   │   │   ├── domain/                 # User、LoginUserInfo
    │   │   │   └── infrastructure/         # UserMapper、JWTUtils、BCryptUtils
    │   │   ├── rateLimit/                  # IP / UID 双维度限流
    │   │   │   ├── adpter/                 # 当前包名为 adpter
    │   │   │   │   ├── annotation/         # ipRateLimit、uidRateLimit
    │   │   │   │   │   └── enums/          # IpLimitLevel、UidLimitLevel、RequestType
    │   │   │   │   └── interceptor/        # ipLimitInterceptor、uidLimitInterceptor
    │   │   │   └── infrastructure/         # ipLimiter、uidLimiter、RedisScriptsConfig
    │   │   ├── task/                       # 任务管理
    │   │   │   ├── adapter/                # TaskController、CreateTaskRequest
    │   │   │   ├── application/            # TaskService、TaskServiceImpl
    │   │   │   ├── domain/                 # Task、TaskOperation 及状态枚举
    │   │   │   └── infrastructure/         # TaskMapper、TaskOperationMapper
    │   │   └── shared/                     # 共用组件
    │   │       ├── exception/              # BusinessException
    │   │       ├── mq/                     # RabbitMQConfig、EmailListener、EmailMessageDTO
    │   │       └── web/                    # WebMvcConfig、GlobalExceptionHandler、Result、ResultCode
    │   └── resources/
    │       ├── application.yml
    │       ├── mapper/TaskMapper.xml        # 列表动态 SQL、结果映射
    │       └── scripts/
    │           ├── rate-limit-ip.lua       # IP 限流 Lua 脚本
    │           └── rate-limit-uid.lua      # UID 限流 Lua 脚本
    └── test/java/com/taskmanager/
        └── TaskManagerApplicationTests.java
```

请求流程：`Controller → Service → Mapper → MySQL`。依赖通过构造方法注入，连接和事务由 Spring 管理。当前 Mapper 同时使用注解 SQL 和 XML 映射。

拦截器顺序由 `shared/web/WebMvcConfig.java` 中的 `order` 决定：

1. `ipLimitInterceptor`（`/**`，`order = 1`）：读取 `@ipRateLimit`，按 IP 执行限流检查，超限抛出业务异常 `429`。
2. `AuthorizeInterceptor`（`/**`，排除 `/login` 和 `/register/**`，`order = 2`）：解析并校验 JWT，检查 Redis 退出登录黑名单，将用户信息放入请求属性 `user`。
3. `uidLimitInterceptor`（`/**`，`order = 3`）：读取 `@uidRateLimit`，从 `user` 获取 UID 并执行限流检查，超限抛出业务异常 `429`；有 UID 限流注解但缺少用户信息时返回业务码 `401`。

两个限流器均跳过非 `HandlerMethod` 的请求，以及未标注对应限流注解的方法。IP 检查放在鉴权前，可以先限制未携带或携带无效 Token 的请求；UID 检查放在鉴权后，使用已校验的用户身份。任一步抛出异常，后续步骤即停止。

```text
任务请求 → IP 限流 → JWT 鉴权 → UID 限流 → Controller → Service → Mapper
```

`/login`、`/register/**` 使用 IP 限流且不参与 JWT 鉴权；`/logout` 仍需鉴权，但当前未启用 IP 或 UID 限流。

邮件任务经 `amq.direct` 交换机和 `email` 路由键进入 `email` 队列，由 `EmailListener` 发送，成功后手动确认，失败时拒绝且不重新入队。

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
new com.taskmanager.identity.infrastructure.BCryptUtils().encode("your_password")
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
| `EMAIL_CODE_SECOND` | 验证码及其发送标记在 Redis 中的有效秒数（`limit.email-code-second`） |

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
$env:EMAIL_CODE_SECOND = '300'
```

以上值均为占位示例，`JWT_KEY` 应替换为随机密钥。验证码存储、退出登录黑名单和限流计数都依赖 Redis。当前 Redis 配置位于 `spring.redis`，运行时需核对配置是否被绑定，确保实际连接地址符合预期。SMTP 端口、认证及 TLS/SSL 选项需按邮箱服务商要求补充到配置中。

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

除以上成功响应外，任一被标注 `@ipRateLimit` 或 `@uidRateLimit` 的接口触发对应维度限流时都会返回业务码 `429`；当前响应内容不区分 IP 和 UID。列表无匹配记录时返回空数组。当前实现中，不传 `status` 或传空字符串均查询全部；`status` 的取值由 `@Pattern` 限制为 `PENDING` 或 `DONE`。

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

验证码与其发送标记共用 Redis 键 `EmailCode:{email}:{ip}`，有效期由 `EMAIL_CODE_SECOND` 决定（当前为 5 分钟）。同一邮箱和 IP 在有效期内重复请求会返回业务码 `429`（`请求验证码频繁，请稍后再试`）。

收到邮件后，在有效期内提交注册信息，使用相同的用户名和邮箱。验证码使用字符串，保留可能出现的前导零：

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

密码必填，长度为 5～30 个字符；验证码必填且长度为 6。校验成功后创建用户，返回业务码 `200`，`data` 为 JWT，可直接访问任务接口。

### 用户登录

```http
POST /login
Content-Type: application/json

{
  "username": "demo",
  "password": "your_password"
}
```

`username` 和 `password` 均不能为空或纯空白，长度分别为 3～15 和 5～30 个字符。登录使用 `LoginRequest`，仅接收用户名和密码。

成功响应示例（Token 为占位值）：

```json
{
  "code": 200,
  "message": "请求成功，响应体包含结果",
  "data": "<JWT字符串>"
}
```

JWT 包含 `uid`、`username`、唯一标识 `jti`、签发时间 `iat` 和过期时间 `exp`，使用 HS256 签名，有效期由 `JWT_EXPIRE_DAY` 决定。过期后需重新登录；退出登录通过 Redis 中的 `jti` 黑名单撤销当前 Token。

### 退出登录

```http
GET /logout
Authorization: Bearer <token>
```

成功返回 `data: null`、业务码 `204`（实际 HTTP 状态仍为 200），客户端应清除本地 Token。服务端把 Token 的 `jti` 写入 `jwt:logout:{jti}`，过期时间取 Token 的剩余有效期，因此登出记录不会长期残留；再次使用同一 Token 访问受保护接口会返回业务码 `401`。

### 创建任务

创建请求通过 Controller 参数上的 `@Valid` 触发 `CreateTaskRequest` 的字段校验：

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

限流命中时返回（例如查询任务详情超过阈值后再次请求）：

```json
{
  "code": 429,
  "message": "请求过多，被限流",
  "data": null
}
```

登录和鉴权错误同样使用 `Result` 包装，以下 `code` 均为业务码，实际 HTTP 状态仍为 200：

| 情况 | `code` | `message` |
| --- | --- | --- |
| 用户不存在 | `400` | 该用户不存在，请重新输入用户名 |
| 密码不匹配 | `400` | 密码错误，登录失败 |
| `status` 取值非法 | `400` | 参数只能是PENDING或DONE |
| 未提供 Authorization、请求头为空或缺少 `Bearer ` 前缀 | `401` | 未提供Token |
| Token 过期 | `401` | Token已过期 |
| Token 格式错误（捕获到 `MalformedJwtException`） | `401` | Token格式错误 |
| Token 签名错误 | `401` | Token签名错误 |
| 其他 JWT 异常（捕获到 `JwtException`） | `401` | 未认证，需要登录 |
| 触发接口限流 | `429` | 请求过多，被限流 |

JWT 相关的异常一律按凭证问题处理，返回 `401`，不会被当作系统异常。当前主要业务码为 `400`（字段校验、参数取值或登录失败）、`401`（未通过鉴权）、`404`（任务不存在）、`409`（重复完成）、`429`（触发限流）和 `500`（系统异常）。JSON 格式错误、参数类型不匹配或其他运行时异常目前可能进入通用异常处理，返回业务码 `500`。

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

## 双维度限流的手工验证

### 分别验证两个 Lua 脚本

脚本可以脱离 Java 单独验证。在项目根目录执行，按实际 Redis 连接补充 `-h`、`-p`、`-n` 等参数。以下使用专门的 `test` 请求类型，不消耗应用接口的额度；测试阈值由命令传入，不受 Java 档位枚举影响。

逗号前是 `KEYS`、逗号后是 `ARGV`。PowerShell 中将逗号写成 `','`，确保它作为独立参数传给 `redis-cli`：

```powershell
# 参数顺序：KEYS[1] 黑名单键、KEYS[2] 计数键、ARGV[1] 阈值、ARGV[2] 窗口秒、ARGV[3] 封禁秒
# IP 脚本：每个窗口允许 5 次，窗口和封禁均为 60 秒
redis-cli --eval src/main/resources/scripts/rate-limit-ip.lua limit:ip:blacklist:test:127.0.0.1 limit:ip:count:test:127.0.0.1 ',' 5 60 60

# UID 脚本：使用独立测试 UID，单独计数和封禁
redis-cli --eval src/main/resources/scripts/rate-limit-uid.lua limit:uid:blacklist:test:1001 limit:uid:count:test:1001 ',' 5 60 60
```

从测试键不存在的状态开始，在 60 秒内分别重复执行对应命令，观察返回值和键的变化：

- 使用默认 RESP2 协议时，前 5 次的 Lua `true` 显示为整数 `1`，第 6 次的 Lua `false` 显示为空回复（通常为 `(nil)`），并写入对应黑名单键。`false` 不会转换为整数 `0`，参见 [Redis Lua 返回值转换规则](https://redis.io/docs/latest/develop/programmability/lua-api/#lua-to-resp2-type-conversion)。
- 计数键 TTL 从窗口长度开始递减，不因后续请求增加。
- 首次封禁时，黑名单键 TTL 接近 60 秒；封禁期间再次调用不会续期，也不会继续增加该维度的计数。
- IP 测试键和 UID 测试键相互独立，一套键被封禁不影响直接调用另一套脚本。

检查 TTL：

```powershell
redis-cli TTL limit:ip:count:test:127.0.0.1
redis-cli TTL limit:ip:blacklist:test:127.0.0.1
redis-cli TTL limit:uid:count:test:1001
redis-cli TTL limit:uid:blacklist:test:1001
```

首次测试前如有旧测试键，或测试结束后，可清理以下四个测试键：

```powershell
redis-cli DEL limit:ip:blacklist:test:127.0.0.1 limit:ip:count:test:127.0.0.1 limit:uid:blacklist:test:1001 limit:uid:count:test:1001
```

### 验证应用中的 IP 与 UID 拦截

以下以 `GET /tasks/{id}` 为例，使用已有任务 ID、有效 Token 和当前 `LOOSE` 档位。每组测试开始前等待对应计数和封禁键过期，并在同一窗口内完成请求：

1. **验证 UID 限流**：同一用户连续查询任务详情，前 20 次通过限流检查，第 21 次返回业务码 `429`。此时 IP 的 120 次额度尚未耗尽；检查 `limit:uid:blacklist:getTaskMessage:{uid}` 确认 UID 封禁。
2. **验证用户间独立计数**：同一 IP 下用户 A 的 UID 已被封禁时，换用户 B 的有效 Token，在 IP 总量未超限且 B 的 UID 额度未耗尽的条件下仍可访问。只更换 A 的 Token 不会获得新的 UID 额度。
3. **验证 IP 限流**：在本地测试中使用多个用户分担请求，确保每个 UID 都不超过 20 次；同一 IP 对任务详情累计第 121 次请求触发 IP 封禁。检查 `limit:ip:blacklist:getTaskMessage:{ip}`；IP 被封禁后，更换用户也无法通过该类型的 IP 检查。
4. **验证封禁恢复**：等待该维度的计数窗口及封禁都过期后再次请求，在另一维度也未超限的条件下应恢复访问。

由于两个维度返回相同的业务码和消息，仅看到 `429` 不能判断是哪一层触发；应结合对应黑名单键确认。以上为验证步骤，不代表已执行自动化测试。

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

限流：

- 未标注某一维度注解的方法会跳过该维度检查（例如 `/logout` 跳过两种限流），尚未实现未标注方法的默认限流策略。
- 已实现 IP / UID 双维度限流；共享出口 IP 的用户仍共享 IP 总额度，需要结合用户规模调整 IP 阈值。
- 阈值已集中到 `IpLimitLevel` 和 `UidLimitLevel` 的 `STRICT`、`NORMAL`、`LOOSE` 三档，但仍写在枚举中，尚未外置为可动态调整的配置。
- 登录和注册接口尚无已认证 UID，当前只启用 IP 限流；发码另有 `EmailCode:{email}:{ip}` 去重检查，独立的 email 维度限流和多级窗口（1 分钟 / 1 小时 / 1 天）尚未实现。
- 白名单路径（静态资源、接口文档、健康检查）尚未统一排除。
- 尚未抽取 `IpUtils.getRealIp(request)`：当前直接使用 `getRemoteAddr()`，未读取可能被伪造的 `X-Forwarded-For`；将来上 Nginx 时需开启 `forward-headers-strategy: native` 并限制应用端口只对内网开放（`application.yml` 中已留注释）。

验证码与注册：

- 验证码使用 `ThreadLocalRandom` 生成，计划改用 `SecureRandom`。
- 注册成功后清理验证码的键是 `EmailCode:{email}`，与写入的 `EmailCode:{email}:{ip}` 不一致，实际未删除，验证码在有效期内可重复使用。
- 验证码的“发送标记”和“验证码值”共用同一个键，两者的生命周期被绑在一起；邮件发送失败时也不会回滚已写入的标记。

JWT 与鉴权：

- `catch (JwtException)` 分支未记录日志，签名错误等安全事件目前是静默的。
- `uid`、`username` 等负载字段名以及 Redis 键名前缀仍是散落的字面量，未抽成常量。
- 请求头的 `Bearer ` 前缀大小写敏感，尚未按 RFC 6750 做成大小写不敏感。

任务与业务：

- 任务尚未按用户隔离，已登录用户可以访问和操作全部任务。
- 补充包含任务 ID 和关键操作的业务日志。

测试：

- 补充注册、验证码失效、退出后 Token 被拒绝，以及 IP / UID 各自触发限流、封禁恢复和双维度拦截顺序的自动化断言。
- 补充同 IP 多用户、同 UID 多 IP / Token 等场景的手工验证记录。

## 更新记录

### 2026-10-18

- 接口限流从单一 IP 维度升级为 IP / UID 双维度，新增 `@uidRateLimit`、`uidLimitInterceptor`、`uidLimiter` 和 `scripts/rate-limit-uid.lua`。
- IP 限流使用 `@ipRateLimit` 和 `scripts/rate-limit-ip.lua`；两个维度分别使用 `limit:ip:*`、`limit:uid:*` 键，独立计数和封禁。
- 拦截器顺序调整为 IP 限流 → JWT 鉴权 → UID 限流；登录和注册仅启用 IP 限流，任务接口同时启用两种限流。
- 新增 `IpLimitLevel`、`UidLimitLevel`，按 `STRICT`、`NORMAL`、`LOOSE` 集中管理阈值，`RequestType` 仅负责接口分类。
- 同步按 `identity`、`rateLimit`、`task`、`shared` 划分的项目结构，并补充双维度限流的手工验证说明。

### 2026-09-27

- 新增接口限流：`@RateLimit` 注解、`ipLimitInterceptor`、`ipLimiter` 与 Redis Lua 脚本 `scripts/rate-limit.lua`。
- 限流与鉴权拆分为两个独立拦截器，并把限流排在鉴权之前，避免未携带 Token 的请求绕过计数。
- `RequestType` 由按请求方法划分改为按接口划分，各接口拥有独立计数额度。
- JWT 增加 `jti`；退出登录改用 `jwt:logout:{jti}`，过期时间取 Token 剩余有效期（此前保存完整 Authorization 值且未设置过期时间）。
- JWT 兜底异常由 `500` 改为 `401`，与凭证问题语义一致。
- 查询列表的 `status` 参数增加取值校验。
