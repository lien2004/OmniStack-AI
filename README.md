# LingLong AI Platform - AI驱动的全栈智能开发平台

## 项目简介

灵龙AI平台是一个基于Spring Cloud微服务架构的AI驱动全栈软件开发平台，集成多种大语言模型（CodeFlow、智谱AI、DeepSeek、通义千问），提供11个专业AI Agent覆盖软件开发生命全周期，内置MCP工具系统和RAG知识库，实现从需求分析到代码生成、测试部署的全流程AI智能化。

**适用场景**: AI智能编程、全栈开发、简历项目、面试展示

---

## 系统架构

```
LingLong AI Platform/
├── linglong-gateway          # API网关服务 (8080)
├── linglong-user             # 用户服务 (8081)
├── linglong-mcp-server       # MCP工具服务 (8083)
├── linglong-llm-core         # LLM核心服务 (8084)
├── linglong-agent            # AI Agent服务 (8085)
├── linglong-engine           # Agent编排引擎（库）
├── linglong-common           # 公共工具库
└── linglong-ui/              # 前端展示页面 (React)
```

### 架构图

```
                         ┌──────────────────┐
                         │   Nginx (80)     │
                         │   反向代理        │
                         └────────┬─────────┘
                                  │
                  ┌───────────────┼───────────────┐
                  │                               │
          ┌───────▼────────┐             ┌───────▼──────┐
          │ Gateway (8080) │             │  React UI    │
          │  Spring Cloud  │             │  Vite (3000) │
          │    Gateway     │             └──────────────┘
          └───────┬────────┘
                  │
    ┌─────────────┼─────────────┬──────────────┐
    │             │             │              │
┌───▼────┐  ┌────▼────┐  ┌────▼─────┐  ┌─────▼──────┐
│ User   │  │  MCP    │  │ LLM Core │  │   Agent    │
│Service │  │ Server  │  │ (8084)   │  │  (8085)    │
│(8081)  │  │ (8083)  │  │          │  │            │
└───┬────┘  └────┬────┘  └────┬─────┘  └─────┬──────┘
    │            │            │              │
    └────────────┼────────────┼──────────────┘
                 │            │
         ┌───────▼──┐  ┌─────▼──────┐
         │  MySQL   │  │ PostgreSQL │
         │ (8.0)    │  │ + pgvector │
         └──────────┘  └────────────┘
                 │
         ┌───────▼──┐
         │   Redis  │
         └──────────┘
```

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 基础框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |
| Spring Cloud Gateway | 最新 | API网关 |
| Spring AI | 0.8.0 | LLM抽象与向量存储 |
| LangChain4j | 0.25.0 | LLM集成框架 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| Spring Data JPA | 最新 | JPA持久化 |
| MySQL | 8.0 | 业务数据库 |
| PostgreSQL | 14+ (pgvector) | 向量数据库 |
| Redis | 6+ | 缓存中间件 |
| MinIO | 最新 | 对象存储 |
| Apache Tika | 2.9.1 | 文档解析 |
| Apache POI | 5.2.5 | 文档生成 |
| JGit | 6.8.0 | Git操作 |
| Docker Java | 3.3.4 | Docker SDK |
| React | 18 | 前端框架 |
| TypeScript | 5 | 类型安全 |
| Ant Design | 5.12 | UI组件库 |
| Zustand | 4 | 状态管理 |
| Vite | 5 | 构建工具 |
| JDK | 21 | Java开发工具包 |
| Maven | 3.6+ | 项目管理工具 |

### 大模型集成

| 模型提供商 | 模型 | 用途 |


---

## 核心功能

### 11个AI Agent

| Agent | 功能描述 |
|-------|---------|
| RequirementAnalysisAgent | 需求分析与拆解 |
| DomainModelingAgent | DDD领域建模 |
| ArchitectureDesignAgent | 系统架构设计 |
| CodeGenerationAgent | 代码自动生成 |
| CodeReviewAgent | 代码审查 |
| TestGenerationAgent | 测试用例生成 |
| DebuggingAgent | Bug诊断与修复 |
| OptimizationAgent | 性能优化 |
| DeploymentAgent | 部署自动化 |
| DocumentationAgent | 文档生成 |
| ResumeAgent | 简历生成与优化 |

### MCP工具系统（7大工具）

| 工具 | 功能描述 |
|------|---------|
| DatabaseTool | SQL查询执行（MySQL + PostgreSQL） |
| ReadFileTool | 文件读取与内容提取 |
| GitTool | Git操作（clone/commit/push等） |
| DiagramTool | 图表/思维导图生成 |
| DockerTool | Docker容器管理 |
| WriteCodeTool | 代码文件生成与写入 |
| WeatherTool | 天气信息查询 |

### 其他功能

- **RAG知识库**: 基于pgvector的向量检索，支持文档上传与智能问答
- **多模型路由**: 自动根据模型名称路由到对应的LLM提供商
- **SSE流式对话**: 支持流式输出，实时展示AI回复
- **文档对话**: 上传PDF/Word/Excel等文件，AI智能分析
- **AI图像生成**: 集成智谱AI图像生成能力
- **CodeFlow工作流**: 6步AI驱动开发流程
- **对话导出**: 支持导出为DOCX文档

---

## 快速开始

### 1. 环境准备

确保本地已安装:
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- PostgreSQL 14+ (含pgvector扩展)
- Redis 6+
- MinIO（对象存储）
- Node.js 18+ (前端)

### 2. 数据库初始化

```sql
-- 创建MySQL数据库
CREATE DATABASE linglong DEFAULT CHARACTER SET utf8mb4;

-- 创建PostgreSQL数据库（需先安装pgvector扩展）
CREATE DATABASE linglongpg;
CREATE EXTENSION vector;
```

### 3. 配置LLM


### 4. 启动服务

按以下顺序启动各服务：

```bash
# 1. 启动Gateway网关
cd linglong-gateway
mvn spring-boot:run

# 2. 启动User服务
cd linglong-user
mvn spring-boot:run

# 3. 启动MCP Server
cd linglong-mcp-server
mvn spring-boot:run

# 4. 启动LLM Core
cd linglong-llm-core
mvn spring-boot:run

# 5. 启动Agent服务
cd linglong-agent
mvn spring-boot:run

# 6. 启动前端UI
cd linglong-ui
npm install
npm run dev
```

### 5. 访问系统

| 服务 | 访问地址 |
|------|----------|
| API网关 | http://localhost:8080 |
| 前端页面 | http://localhost:3000 |
| 默认账号 | admin / admin123 |

---

## 项目模块

| 模块 | 端口 | 说明 |
|------|------|------|
| linglong-gateway | 8080 | API网关，JWT鉴权，CORS，限流，路由转发 |
| linglong-user | 8081 | 用户认证、注册、个人信息管理 |
| linglong-mcp-server | 8083 | MCP工具注册与执行 |
| linglong-llm-core | 8084 | LLM多模型路由，RAG检索，文档解析，SSE流式 |
| linglong-agent | 8085 | 11个AI Agent管理与编排 |
| linglong-engine | - | Agent编排引擎（工作流引擎） |
| linglong-common | - | 公共工具库（Result包装、异常定义、常量） |

---

## 文档

- [使用手册](linglong-ai-platform/使用手册.md)
- [后端接口文档](linglong-ai-platform/后端接口文档.md)
- [数据库设计文档](linglong-ai-platform/数据库设计文档.md)
- [部署手册](linglong-ai-platform/部署手册.md)
