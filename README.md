# Task Manager · 任务记录管理

基于 Spring Boot、MyBatis 和 MySQL 的任务管理后端项目，用于记录待办事项、查询任务进度、完成任务及保存操作记录。
目前提供 HTTP JSON 接口，可使用 Postman、Apifox 或 PowerShell 调用。项目不包含前端、登录、分页、Redis 或消息队列。

## 技术栈

以下版本以仓库中的 `pom.xml` 为准。

| 技术 | 版本或用途 |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.1 |
| MyBatis Spring Boot Starter | 4.0.0 |
| MySQL | 数据持久化，两张表使用 InnoDB |
| Maven Wrapper | 构建、启动和执行测试 |
| Lombok | 生成构造方法、访问方法等代码 |
| JUnit Jupiter / Spring Boot Test | 测试依赖 |

## 功能

- 创建任务：保存标题、描述，初始状态为 `PENDING`，返回生成的 ID。
- 查询任务：根据 ID 获取任务详情。
- 查询列表：支持按状态筛选，不传状态时查询全部，按 ID 升序排列。
- 完成任务：将 `PENDING` 改为 `DONE`，同时写入一条 `COMPLETE` 操作记录。
- 删除任务：删除任务本身，保留历史操作记录。
- 统一响应：使用 `code`、`message`、`data` 包装结果，集中处理异常。

当前代码与设计目标的差异见文末“待完善事项”。

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
    │   │   ├── controller/                # HTTP 接口
    │   │   ├── service/                   # 业务接口及实现
    │   │   ├── mapper/                    # 数据访问接口、注解 SQL
    │   │   ├── entity/dto/                # Task、TaskOperation
    │   │   ├── entity/vo/                 # CreateTaskRequest
    │   │   ├── exception/                 # 业务异常
    │   │   └── utils/                     # 响应封装及状态枚举
    │   └── resources/
    │       ├── application.yml
    │       └── mapper/TaskMapper.xml      # 列表动态 SQL、结果映射
    └── test/java/com/taskmanager/
        └── TaskManagerApplicationTests.java
```

请求流程：`Controller → Service → Mapper → MySQL`。依赖通过构造方法注入，连接和事务由 Spring 管理。当前 Mapper 同时使用注解 SQL 和 XML 映射。

## 本地运行

### 1. 准备环境

安装 JDK 25，配置 `JAVA_HOME`，并准备可连接的 MySQL 实例。项目自带 Maven Wrapper，无需单独安装 Maven；首次执行需要下载 Maven 和项目依赖。

下面的命令在项目根目录的 PowerShell 中执行。

### 2. 创建数据库和表

在 MySQL 客户端执行下面的初始化 SQL。这里以 `task_manager` 为数据库名；也可以使用其他名称，但需要与 `DB_URL` 一致。

仓库目前没有自动初始化脚本，需手动执行。以下结构根据设计书及现有 Mapper 整理，时间字段统一使用代码中的 `created_time`。

```sql
CREATE DATABASE IF NOT EXISTS task_manager
    DEFAULT CHARACTER SET utf8mb4;

USE task_manager;

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

### 3. 设置数据库环境变量

`application.yml` 通过以下三个环境变量读取连接信息，不在配置文件中写入真实密码：

| 环境变量 | 含义 |
| --- | --- |
| `DB_URL` | MySQL JDBC 连接地址，包含数据库名 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |

在当前 PowerShell 终端设置，替换为自己的连接信息：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/task_manager?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf-8'
$env:DB_USERNAME = 'your_database_user'
$env:DB_PASSWORD = 'your_database_password'
```

这些变量只对当前终端及其启动的进程生效。如果通过 IntelliJ IDEA 的运行按钮启动，请在 `TaskManagerApplication` 的运行配置中设置相同的环境变量。项目未配置自动加载 `.env` 文件。

### 4. 启动服务

```powershell
.\mvnw.cmd spring-boot:run
```

默认接口地址为 `http://localhost:8080/tasks`。项目没有首页，直接访问根路径不能用于判断接口是否正常。

也可以在配置好环境变量后，使用 IDEA 运行 `com.taskmanager.TaskManagerApplication`。

macOS / Linux 使用 `export` 设置同名环境变量，并通过 `./mvnw spring-boot:run` 启动。

## 接口说明

请求体使用 `Content-Type: application/json`。

**当前响应体中的 `code` 是业务码，不是实际 HTTP 状态码。** 当前 Controller 和异常处理器返回 `Result` 对象，正常处理及被处理器捕获的异常默认均返回 HTTP 200。例如，删除成功仍返回 JSON，其 `code` 为 204，并非 HTTP 204 空响应。

| 功能 | 方法和路径 | 参数 | 成功时的 `data` / `code` |
| --- | --- | --- | --- |
| 创建任务 | `POST /tasks` | JSON：`title`、`description` | 新任务 ID / `200` |
| 查询详情 | `GET /tasks/{id}` | 路径参数 `id` | 任务对象 / `200` |
| 查询列表 | `GET /tasks?status=PENDING` | 可选参数 `status` | 任务数组 / `200` |
| 完成任务 | `POST /tasks/{id}/complete` | 路径参数 `id` | `null` / `204` |
| 删除任务 | `DELETE /tasks/{id}` | 路径参数 `id` | `null` / `204` |

列表无匹配记录时返回空数组。当前实现中，不传 `status` 或传空字符串均查询全部；合法的状态值为 `PENDING` 和 `DONE`，但尚未主动拦截非法状态。

### 创建任务

```http
POST /tasks
Content-Type: application/json

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

例如，重复完成任务时返回：

```json
{
  "code": 409,
  "message": "任务不可重复完成",
  "data": null
}
```

当前主要业务码为 `400`（已检测到的参数错误）、`404`（任务不存在）、`409`（重复完成）和 `500`（系统异常）。请求格式或参数类型异常目前可能进入通用异常处理，返回业务码 `500`。

### PowerShell 调用示例

服务启动后，在另一个 PowerShell 终端执行。示例会创建、完成并删除一条练习任务，其完成记录会保留在数据库中。

```powershell
$baseUrl = 'http://localhost:8080'
$body = @{ title = 'Review MyBatis'; description = 'Practice transactions' } | ConvertTo-Json

# 创建任务，并获取实际生成的 ID
$created = Invoke-RestMethod -Method Post -Uri "$baseUrl/tasks" -ContentType 'application/json' -Body $body
$taskId = $created.data

# 查询详情、全部任务和待完成任务
Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId"
Invoke-RestMethod -Uri "$baseUrl/tasks"
Invoke-RestMethod -Uri "$baseUrl/tasks?status=PENDING"

# 完成任务
Invoke-RestMethod -Method Post -Uri "$baseUrl/tasks/$taskId/complete"

# 删除任务
Invoke-RestMethod -Method Delete -Uri "$baseUrl/tasks/$taskId"
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

在配置好数据库环境变量后执行：

```powershell
# 执行现有测试
.\mvnw.cmd test

# 执行测试并打包
.\mvnw.cmd clean package

# 运行打包产物
java -jar .\target\Task-Manager-0.0.1-SNAPSHOT.jar
```

当前测试类使用 `@SpringBootTest`，包含异常信息输出示例，尚未覆盖接口行为、业务断言和事务回滚。这些命令是运行说明，不代表仓库已经完成全部测试验证。

日志方面，业务异常使用 WARN 记录消息，系统异常使用 ERROR 记录堆栈；MyBatis 当前通过 `StdOutImpl` 输出 SQL 调试信息。

## 待完善事项

对照设计书，后续主要完善以下内容：

- [ ] 完整校验标题：不能为空白，且最多 100 个字符。当前使用 `title == ""` 判断空字符串，需改为可靠的内容校验。
- [ ] 校验描述最多 500 个字符。
- [ ] 校验列表状态只接受 `PENDING`、`DONE` 或不筛选，非法值返回参数错误。
- [ ] 对齐设计书的 HTTP 状态码：非法输入 400、任务不存在 404、重复完成 409、删除成功 204；当前仅在 JSON 中返回相应业务码。
- [ ] 补充创建、查询、筛选、删除、重复完成等接口与业务测试。
- [ ] 验证事务成功提交和第二步失败时的整体回滚，并保留验证记录。
- [ ] 补充包含任务 ID 和关键操作的业务日志。
- [ ] 补充登录与注册功能
