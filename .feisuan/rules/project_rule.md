
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息

- **项目名称**：LingLong AI Platform (玲珑AI平台)
- **工作区路径**：`E:\OmniStack AI`
- **代码作者**：lien
- **第一语言**：中文

## 二、技术栈要求

- **主框架**：Spring Boot 3.2.0
- **语言版本**：Java 21
- **构建工具**：Maven
- **核心依赖**：
  - `spring-boot-starter-web` / `spring-boot-starter-webflux`
  - `spring-boot-starter-data-jpa` / `spring-boot-starter-data-redis`
  - `spring-boot-starter-security` / `spring-boot-starter-oauth2-resource-server`
  - `spring-cloud-starter-gateway`
  - `spring-ai` (0.8.0-SNAPSHOT)
  - `langchain4j` (0.25.0)
  - `mybatis-plus-boot-starter` (3.5.5/3.5.6)
  - `lombok` (1.18.30)
  - `postgresql` (42.7.1) / `mysql-connector-j` (8.3.0)
  - `springdoc-openapi-starter-webmvc-ui` (2.3.0)

## 三、目录结构规范

项目采用多模块 Maven 结构，主模块名为 `linglong-ai-platform`。

```text
E:\OmniStack AI
├── linglong-ai-platform (父POM)
│   ├── linglong-llm-core          # 核心LLM服务、向量存储、RAG
│   ├── linglong-mcp-server        # MCP协议实现、工具注册
│   ├── linglong-agent             # 10大Agent具体实现、业务编排
│   ├── linglong-engine            # Agent编排引擎、工作流、状态机
│   ├── linglong-infra             # 基础设施模块、数据库、Git、Docker
│   ├── linglong-gateway           # API网关、鉴权、路由
│   ├── linglong-user              # 用户管理、认证服务
│   └── linglong-common            # 公共模块、常量、异常、工具类
└── linglong-ui                    # 前端项目目录
```

**子模块标准包结构**：
```text
[Module Name]
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── linglong / lingnong
│   │   │           └── [module]
│   │   │               ├── config          # 配置类
│   │   │               ├── controller      # 控制器层
│   │   │               ├── service         # 服务层
│   │   │               │   └── impl        # 服务实现
│   │   │               ├── mapper          # 数据访问层
│   │   │               ├── entity          # 实体类
│   │   │               ├── model           # 请求/响应DTO
│   │   │               ├── exception       # 自定义异常
│   │   │               └── utils           # 工具类
│   │   └── resources
│   │       ├── application.yml
│   │       ├── mapper              # MyBatis XML映射文件
│   │       └── static / templates  # 静态资源
│   └── test
│       └── java
```

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用                    |
| **Service**    | 实现业务逻辑、事务管理、LLM调用    | 必须通过 Mapper/Repository 层访问数据库；返回 DTO 而非 Entity       |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `MyBatis-Plus` 接口；使用 `@Mapper` 注解                       |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端（需转换为 DTO）；包名统一为 `entity`             |

## 五、安全与性能规范

### 输入校验
- 使用 `@Valid` 与 JSR-303 校验注解。
- **注意**：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`。

### 事务管理
- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。

### 数据库与缓存
- **PostgreSQL**：用于 LLM Core 的向量存储及主数据持久化。
- **MySQL**：用于 Agent、MCP Server、User 模块的用户表及对话历史。
- **Redis**：用于缓存、Session管理、Token校验及高并发场景。

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范
- **语言**：使用 **中文** 编写类、方法、字段的 Javadoc 注释。
- 使用 `@Slf4j` 注解进行日志记录，禁止使用 `System.out.println`。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| DO   | 数据库实体对象               | `UserDO`     |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具
- 使用 Lombok 注解：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

## 七、接口与实现分离
- 所有业务接口需单独定义（如 `UserService`），具体实现放在 `impl` 子包中（如 `UserServiceImpl`）。

## 八、扩展性与日志规范

### 日志记录
- 使用 `@Slf4j` 注解。
- 日志级别：`DEBUG` 用于开发调试，`INFO` 用于关键流程，`ERROR` 用于异常捕获。

## 九、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
