# 灵龙AI智能平台 — 后端接口文档

> 版本：1.0.0-SNAPSHOT  
> 更新时间：2026-04-16  
> 统一响应格式：`{ "code": 200, "message": "success", "data": ... }`

---

## 服务端口总览

| 服务名               | 端口   | 说明               |
|----------------------|--------|--------------------|
| linglong-gateway     | 8080   | API 网关（统一入口）|
| linglong-project     | 8081   | 认证 / 项目 / 设置 |
| linglong-mcp-server  | 8083   | MCP 工具服务       |
| linglong-llm-core    | 8084   | LLM 对话 / 知识库  |
| linglong-agent       | 8085   | 智能 Agent 服务    |

---

## 目录

1. [认证管理 `/api/auth`](#一认证管理)
2. [项目管理 `/api/projects`](#二项目管理)
3. [平台设置 `/settings`](#三平台设置)
4. [AI 对话 `/ai`](#四ai-对话)
5. [智能对话（多轮）`/ai/conversation`](#五智能对话多轮rag)
6. [文档对话 `/ai/document`](#六文档对话)
7. [RAG 问答 `/rag`](#七rag-问答)
8. [向量知识库 `/vector`](#八向量知识库)
9. [MCP 工具 `/api/mcp`](#九mcp-工具)
10. [Agent 服务 `/api/agent`](#十agent-服务)

---

## 一、认证管理

> **服务地址**：`http://localhost:8081`  
> **Base Path**：`/api/auth`

---

### 1.1 用户登录

- **方法**：`POST`
- **路径**：`/api/auth/login`
- **请求体（JSON）**：

```json
{
  "username": "admin",
  "password": "123456"
}
```

- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "success": true,
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "message": "登录成功"
  }
}
```

---

### 1.2 获取当前用户信息

- **方法**：`GET`
- **路径**：`/api/auth/me`
- **请求头**：`Authorization: Bearer <token>`
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "createdAt": "2026-01-01T00:00:00"
  }
}
```

---

### 1.3 退出登录

- **方法**：`POST`
- **路径**：`/api/auth/logout`
- **响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 二、项目管理

> **服务地址**：`http://localhost:8081`  
> **Base Path**：`/api/projects`

---

### 2.1 创建项目

- **方法**：`POST`
- **路径**：`/api/projects`
- **请求体（JSON）**：

```json
{
  "name": "电商系统",
  "description": "一个完整的电商系统",
  "techStack": "Spring Boot + Vue3",
  "architectureType": "microservice"
}
```

| 字段             | 类型   | 必填 | 说明         |
|------------------|--------|------|--------------|
| name             | String | 是   | 项目名称     |
| description      | String | 否   | 项目描述     |
| techStack        | String | 否   | 技术栈       |
| architectureType | String | 否   | 架构类型     |

- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "id": "uuid-xxx",
    "name": "电商系统",
    "description": "一个完整的电商系统",
    "techStack": "Spring Boot + Vue3",
    "architectureType": "microservice",
    "status": "init",
    "createTime": "2026-04-16T10:00:00",
    "updateTime": "2026-04-16T10:00:00"
  }
}
```

---

### 2.2 获取项目列表

- **方法**：`GET`
- **路径**：`/api/projects`
- **查询参数**：

| 参数   | 类型   | 必填 | 默认值 | 说明           |
|--------|--------|------|--------|----------------|
| status | String | 否   | -      | 项目状态过滤   |
| page   | int    | 否   | 1      | 页码           |
| size   | int    | 否   | 10     | 每页数量       |

- **响应示例**：

```json
{
  "code": 200,
  "data": [
    {
      "id": "uuid-xxx",
      "name": "电商系统",
      "status": "init",
      "createTime": "2026-04-16T10:00:00"
    }
  ]
}
```

---

### 2.3 获取项目详情

- **方法**：`GET`
- **路径**：`/api/projects/{projectId}`
- **路径参数**：`projectId` — 项目 ID

---

### 2.4 更新项目

- **方法**：`PUT`
- **路径**：`/api/projects/{projectId}`
- **请求体（JSON）**：

```json
{
  "name": "新名称",
  "description": "新描述",
  "status": "active"
}
```

---

### 2.5 删除项目

- **方法**：`DELETE`
- **路径**：`/api/projects/{projectId}`
- **响应示例**：

```json
{
  "code": 200,
  "data": null
}
```

---

## 三、平台设置

> **服务地址**：`http://localhost:8081`  
> **Base Path**：`/settings`

---

### 3.1 获取平台基本设置

- **方法**：`GET`
- **路径**：`/settings/platform`
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "general": {
      "platformName": "灵龙AI智能平台",
      "notification": true,
      "autoSave": true
    }
  }
}
```

---

### 3.2 保存平台基本设置

- **方法**：`POST`
- **路径**：`/settings/platform`
- **请求体（JSON）**：

```json
{
  "general": {
    "platformName": "灵龙AI智能平台",
    "notification": true,
    "autoSave": true
  }
}
```

---

### 3.3 获取 LLM 厂商列表

- **方法**：`GET`
- **路径**：`/settings/llm/providers`
- **说明**：返回所有已启用的 LLM 厂商配置

---

### 3.4 保存 LLM 厂商配置

- **方法**：`POST`
- **路径**：`/settings/llm/providers`
- **请求体（JSON）**：

```json
{
  "providerName": "智谱AI",
  "apiKey": "xxx",
  "baseUrl": "https://open.bigmodel.cn/api/paas",
  "enabled": true,
  "isDefault": true
}
```

---

### 3.5 删除 LLM 厂商配置

- **方法**：`DELETE`
- **路径**：`/settings/llm/providers/{id}`

---

### 3.6 获取数据库配置列表

- **方法**：`GET`
- **路径**：`/settings/database`

---

### 3.7 保存数据库配置

- **方法**：`POST`
- **路径**：`/settings/database`
- **请求体（JSON）**：

```json
{
  "dbType": "mysql",
  "host": "localhost",
  "port": 3306,
  "database": "mydb",
  "username": "root",
  "password": "123456",
  "enabled": true
}
```

---

### 3.8 删除数据库配置

- **方法**：`DELETE`
- **路径**：`/settings/database/{id}`

---

### 3.9 测试数据库连接

- **方法**：`POST`
- **路径**：`/settings/database/test`
- **请求体（JSON）**：与 3.7 相同结构
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "success": true,
    "message": "连接成功",
    "version": "8.0.28"
  }
}
```

---

### 3.10 获取数据库详细信息

- **方法**：`GET`
- **路径**：`/settings/database/{id}/info`

---

### 3.11 获取安全设置

- **方法**：`GET`
- **路径**：`/settings/security`
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "jwtEnabled": false,
    "tokenExpireHours": 24,
    "operationLogEnabled": false
  }
}
```

---

### 3.12 保存安全设置

- **方法**：`POST`
- **路径**：`/settings/security`
- **请求体**：与 3.11 响应结构相同

---

## 四、AI 对话

> **服务地址**：`http://localhost:8084`  
> **Base Path**：`/ai`

---

### 4.1 生成 AI 回复（简单版）

- **方法**：`GET`
- **路径**：`/ai/generate`
- **查询参数**：

| 参数    | 类型   | 必填 | 默认值 | 说明     |
|---------|--------|------|--------|----------|
| message | String | 否   | hello  | 用户消息 |

- **响应**：纯文本字符串，AI 生成的回复

---

### 4.2 指定模型生成 AI 回复

- **方法**：`GET`
- **路径**：`/ai/generate/advanced`
- **查询参数**：

| 参数    | 类型   | 必填 | 默认值  | 说明     |
|---------|--------|------|---------|----------|
| message | String | 是   | -       | 用户消息 |
| model   | String | 否   | glm-4.6 | 模型名称 |

- **响应**：纯文本字符串（优先走向量缓存）

---

## 五、智能对话（多轮 + RAG）

> **服务地址**：`http://localhost:8084`  
> **Base Path**：`/ai/conversation`  
> **注意**：需配置 MySQL 方可启用（`chat.datasource.url`）

---

### 5.1 同步多轮对话

- **方法**：`POST`
- **路径**：`/ai/conversation/chat`
- **请求体（JSON）**：

```json
{
  "chatId": "session-abc123",
  "message": "你好，介绍一下Spring Boot",
  "model": "glm-4.6",
  "useRag": true,
  "temperature": 0.7,
  "maxTokens": 4000
}
```

| 字段        | 类型    | 必填 | 默认值  | 说明                            |
|-------------|---------|------|---------|---------------------------------|
| chatId      | String  | 否   | -       | 会话 ID（为空则不保存历史）     |
| message     | String  | 是   | -       | 用户消息                        |
| model       | String  | 否   | glm-4.6 | 模型名称                        |
| useRag      | boolean | 否   | false   | 是否启用 RAG 知识库检索         |
| temperature | double  | 否   | 0.7     | 温度参数（0-1）                 |
| maxTokens   | int     | 否   | 4000    | 最大输出 Token 数               |

- **响应示例**：

```json
{
  "chatId": "session-abc123",
  "answer": "Spring Boot 是...",
  "model": "glm-4.6",
  "fromCache": false
}
```

---

### 5.2 SSE 流式对话

- **方法**：`POST`
- **路径**：`/ai/conversation/stream`
- **Content-Type**：`application/json`
- **Accept**：`text/event-stream`
- **请求体**：与 5.1 相同
- **响应**：SSE 事件流，每帧推送部分文字，最后推送 `[DONE]`

```
data: Spring Boot
data:  是一个
data: ...
data: [DONE]
```

---

### 5.3 获取会话历史记录

- **方法**：`GET`
- **路径**：`/ai/conversation/history/{chatId}`
- **响应示例**：

```json
[
  { "role": "user", "content": "你好", "model": "glm-4.6", "createdAt": "..." },
  { "role": "assistant", "content": "你好！有什么可以帮你的？", "model": "glm-4.6", "createdAt": "..." }
]
```

---

### 5.4 获取所有会话列表

- **方法**：`GET`
- **路径**：`/ai/conversation/sessions`
- **响应示例**：

```json
[
  { "chatId": "session-abc123", "title": "关于Spring Boot的对话", "lastMessageAt": "..." }
]
```

---

### 5.5 删除指定会话

- **方法**：`DELETE`
- **路径**：`/ai/conversation/sessions/{chatId}`

---

### 5.6 清空所有会话

- **方法**：`DELETE`
- **路径**：`/ai/conversation/sessions`

---

## 六、文档对话

> **服务地址**：`http://localhost:8084`  
> **Base Path**：`/ai/document`  
> **支持格式**：PDF、Word（DOCX/DOC）、Excel（XLSX/XLS）、PPT（PPTX/PPT）、RTF、Markdown、TXT、HTML、JSON、CSV、代码文件等  
> **文件大小限制**：单文件最大 100MB

---

### 6.1 文件上传对话（同步）

- **方法**：`POST`
- **路径**：`/ai/document/chat`
- **Content-Type**：`multipart/form-data`
- **表单参数**：

| 参数    | 类型          | 必填 | 默认值  | 说明                    |
|---------|---------------|------|---------|-------------------------|
| file    | MultipartFile | 是   | -       | 上传的文档文件          |
| message | String        | 是   | -       | 针对文档的问题          |
| model   | String        | 否   | glm-4.6 | 模型名称                |
| chatId  | String        | 否   | ""      | 会话 ID（用于多轮记忆） |
| useRag  | boolean       | 否   | false   | 是否叠加 RAG 知识库     |

- **响应**：纯文本（Markdown 格式），AI 对文档内容的回答

---

### 6.2 文件上传对话（SSE 流式）

- **方法**：`POST`
- **路径**：`/ai/document/stream`
- **Content-Type**：`multipart/form-data`
- **Accept**：`text/event-stream`
- **表单参数**：与 6.1 相同
- **响应**：SSE 事件流

---

### 6.3 本地路径文件对话（同步）

- **方法**：`POST`
- **路径**：`/ai/document/chat/path`
- **请求体（JSON）**：

```json
{
  "filePath": "/tmp/linglong_upload_xxx.docx",
  "message": "请总结这份文档的主要内容",
  "model": "glm-4.6",
  "chatId": "session-abc123",
  "useRag": false
}
```

| 字段     | 类型    | 必填 | 默认值  | 说明                |
|----------|---------|------|---------|---------------------|
| filePath | String  | 是   | -       | 服务器本地文件路径  |
| message  | String  | 是   | -       | 用户问题            |
| model    | String  | 否   | glm-4.6 | 模型名称            |
| chatId   | String  | 否   | -       | 会话 ID             |
| useRag   | boolean | 否   | false   | 是否叠加知识库      |

- **响应**：纯文本，AI 回答

---

### 6.4 本地路径文件对话（SSE 流式）

- **方法**：`POST`
- **路径**：`/ai/document/stream/path`
- **Accept**：`text/event-stream`
- **请求体（JSON）**：与 6.3 相同
- **响应**：SSE 事件流

---

### 6.5 上传临时文件（供 MCP 工具使用）

- **方法**：`POST`
- **路径**：`/ai/document/upload/temp`
- **Content-Type**：`multipart/form-data`
- **表单参数**：

| 参数 | 类型          | 必填 | 说明         |
|------|---------------|------|--------------|
| file | MultipartFile | 是   | 上传的文件   |

- **响应示例**：

```json
{
  "path": "/tmp/linglong_upload_abc_report.pdf",
  "filename": "report.pdf"
}
```

---

### 6.6 导出对话为 DOCX

- **方法**：`POST`
- **路径**：`/ai/document/export`
- **请求体（JSON）**：

```json
{
  "title": "我的对话记录",
  "messages": [
    {
      "role": "user",
      "content": "你好",
      "timestamp": "2026-04-16T10:00:00Z",
      "model": null,
      "fileAttachment": null
    },
    {
      "role": "assistant",
      "content": "你好！有什么可以帮你的？",
      "timestamp": "2026-04-16T10:00:05Z",
      "model": "glm-4.6",
      "fileAttachment": null
    }
  ]
}
```

| 字段                    | 类型    | 必填 | 说明                        |
|-------------------------|---------|------|-----------------------------|
| title                   | String  | 否   | 文档标题                    |
| messages                | Array   | 是   | 消息列表                    |
| messages[].role         | String  | 是   | `user` 或 `assistant`       |
| messages[].content      | String  | 是   | 消息内容（支持 Markdown）   |
| messages[].timestamp    | String  | 否   | ISO 8601 时间戳             |
| messages[].model        | String  | 否   | 模型名称（assistant 可选）  |
| messages[].fileAttachment | String | 否 | 文件附件名（展示用）        |

- **响应**：二进制 `.docx` 文件下载流
- **响应头**：`Content-Disposition: attachment; filename*=UTF-8''对话记录.docx`

---

## 七、RAG 问答

> **服务地址**：`http://localhost:8084`  
> **Base Path**：`/rag`

---

### 7.1 RAG 问答（基础）

- **方法**：`GET`
- **路径**：`/rag/chat`
- **查询参数**：

| 参数     | 类型   | 必填 | 默认值 | 说明             |
|----------|--------|------|--------|------------------|
| question | String | 是   | -      | 用户问题         |
| topK     | int    | 否   | 3      | 检索知识库条数   |

- **响应示例**：

```json
{
  "question": "如何登录系统",
  "answer": "您可以通过访问登录页面，输入用户名和密码..."
}
```

---

### 7.2 按项目 RAG 问答

- **方法**：`GET`
- **路径**：`/rag/chat/project`
- **查询参数**：

| 参数      | 类型   | 必填 | 默认值 | 说明               |
|-----------|--------|------|--------|--------------------|
| question  | String | 是   | -      | 用户问题           |
| projectId | String | 是   | -      | 项目 ID            |
| topK      | int    | 否   | 3      | 检索知识库条数     |

- **响应示例**：

```json
{
  "question": "如何登录系统",
  "projectId": "proj-001",
  "answer": "..."
}
```

---

### 7.3 RAG 问答（含引用来源）

- **方法**：`GET`
- **路径**：`/rag/chat/sources`
- **查询参数**：与 7.1 相同
- **响应示例**：

```json
{
  "question": "如何登录系统",
  "answer": "您可以通过...",
  "sources": [
    {
      "id": "uuid-xxx",
      "content": "登录流程说明...",
      "type": "knowledge",
      "distance": 0.12
    }
  ]
}
```

---

## 八、向量知识库

> **服务地址**：`http://localhost:8084`  
> **Base Path**：`/vector`

---

### 8.1 添加单个文档

- **方法**：`POST`
- **路径**：`/vector/doc`
- **请求体（JSON）**：

```json
{
  "content": "Spring Boot 是一个用于快速开发微服务的框架...",
  "metadata": {
    "type": "knowledge",
    "projectId": "proj-001",
    "category": "document"
  }
}
```

- **响应示例**：

```json
{ "success": true, "message": "文档已添加" }
```

---

### 8.2 批量添加文档

- **方法**：`POST`
- **路径**：`/vector/docs`
- **请求体（JSON 数组）**：

```json
[
  { "content": "内容一", "metadata": { "type": "knowledge" } },
  { "content": "内容二", "metadata": { "type": "knowledge" } }
]
```

- **响应示例**：

```json
{ "success": true, "message": "已添加 2 个文档" }
```

---

### 8.3 添加代码片段

- **方法**：`POST`
- **路径**：`/vector/code`
- **请求体（JSON）**：

```json
{
  "content": "public class UserService { ... }",
  "fileName": "UserService.java",
  "projectId": "proj-001"
}
```

- **响应示例**：

```json
{ "success": true, "message": "代码片段已添加", "fileName": "UserService.java" }
```

---

### 8.4 添加需求文档

- **方法**：`POST`
- **路径**：`/vector/requirement`
- **请求体（JSON）**：

```json
{
  "content": "用户需要能够通过手机号注册账号...",
  "title": "用户注册需求",
  "projectId": "proj-001"
}
```

- **响应示例**：

```json
{ "success": true, "message": "需求文档已添加", "title": "用户注册需求" }
```

---

### 8.5 上传文件到知识库

- **方法**：`POST`
- **路径**：`/vector/upload`
- **Content-Type**：`multipart/form-data`
- **表单参数**：

| 参数     | 类型          | 必填 | 默认值   | 说明         |
|----------|---------------|------|----------|--------------|
| file     | MultipartFile | 是   | -        | 上传文件     |
| title    | String        | 否   | 文件名   | 知识库标题   |
| category | String        | 否   | document | 文档分类     |

- **响应示例**：

```json
{
  "success": true,
  "message": "文档已成功建索",
  "filename": "用户手册.pdf",
  "chunks": 15
}
```

---

### 8.6 语义搜索

- **方法**：`GET`
- **路径**：`/vector/search`
- **查询参数**：

| 参数  | 类型   | 必填 | 默认值 | 说明             |
|-------|--------|------|--------|------------------|
| query | String | 是   | -      | 搜索关键词/语义  |
| topK  | int    | 否   | 5      | 返回结果数       |

- **响应示例**：

```json
[
  {
    "id": "uuid-xxx",
    "content": "相关内容片段...",
    "metadata": { "type": "knowledge", "filename": "手册.pdf" }
  }
]
```

---

### 8.7 带相似度阈值的搜索

- **方法**：`GET`
- **路径**：`/vector/search/threshold`
- **查询参数**：

| 参数      | 类型   | 必填 | 默认值 | 说明           |
|-----------|--------|------|--------|----------------|
| query     | String | 是   | -      | 搜索语义       |
| topK      | int    | 否   | 5      | 最多返回数     |
| threshold | double | 否   | 0.7    | 相似度阈值     |

---

### 8.8 按项目搜索代码

- **方法**：`GET`
- **路径**：`/vector/search/code`
- **查询参数**：

| 参数      | 类型   | 必填 | 默认值 | 说明       |
|-----------|--------|------|--------|------------|
| query     | String | 是   | -      | 搜索内容   |
| projectId | String | 是   | -      | 项目 ID    |
| topK      | int    | 否   | 5      | 返回数量   |

---

### 8.9 按项目搜索需求

- **方法**：`GET`
- **路径**：`/vector/search/requirement`
- **查询参数**：与 8.8 相同

---

### 8.10 获取知识库文档列表

- **方法**：`GET`
- **路径**：`/vector/list`
- **查询参数**：

| 参数     | 类型   | 必填 | 默认值   | 说明         |
|----------|--------|------|----------|--------------|
| page     | int    | 否   | 0        | 页码（从0起）|
| size     | int    | 否   | 20       | 每页数量     |
| category | String | 否   | ""       | 文档分类过滤 |

- **响应示例**：

```json
{
  "total": 100,
  "page": 0,
  "size": 20,
  "items": [
    {
      "id": "uuid-xxx",
      "contentPreview": "前120字内容预览...",
      "contentLength": 850,
      "metadata": { "type": "knowledge", "filename": "手册.pdf", "title": "用户手册" }
    }
  ]
}
```

---

### 8.11 获取单条文档完整内容

- **方法**：`GET`
- **路径**：`/vector/doc/{id}`
- **路径参数**：`id` — 文档 UUID
- **响应示例**：

```json
{
  "id": "uuid-xxx",
  "content": "完整的文档内容...",
  "contentLength": 850,
  "metadata": { "type": "knowledge", "filename": "手册.pdf" }
}
```

---

### 8.12 删除文档

- **方法**：`DELETE`
- **路径**：`/vector/docs`
- **请求体（JSON 数组）**：

```json
["uuid-1", "uuid-2", "uuid-3"]
```

- **响应示例**：

```json
{ "success": true, "message": "已删除 3 个文档" }
```

---

## 九、MCP 工具

> **服务地址**：`http://localhost:8083`  
> **Base Path**：`/api/mcp`

---

### 9.1 获取所有工具列表

- **方法**：`GET`
- **路径**：`/api/mcp/tools`
- **响应示例**：

```json
{
  "code": 200,
  "data": [
    {
      "name": "read_file",
      "description": "读取文件内容",
      "category": "filesystem",
      "parameters": {}
    }
  ]
}
```

---

### 9.2 按分类获取工具

- **方法**：`GET`
- **路径**：`/api/mcp/tools/category/{category}`
- **路径参数**：`category` — 工具分类（如 `filesystem`、`database` 等）

---

### 9.3 获取单个工具定义

- **方法**：`GET`
- **路径**：`/api/mcp/tools/{toolName}`
- **路径参数**：`toolName` — 工具名称

---

### 9.4 执行工具

- **方法**：`POST`
- **路径**：`/api/mcp/tools/{toolName}/execute`
- **路径参数**：`toolName` — 工具名称
- **请求体（JSON）**：工具所需参数（因工具而异）

```json
{
  "path": "/tmp/linglong_upload_xxx.pdf"
}
```

- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "success": true,
    "result": "文件内容...",
    "error": null
  }
}
```

---

### 9.5 检查工具是否存在

- **方法**：`GET`
- **路径**：`/api/mcp/tools/{toolName}/exists`
- **响应示例**：

```json
{ "code": 200, "data": true }
```

---

## 十、Agent 服务

> **服务地址**：`http://localhost:8085`  
> **Base Path**：`/api/agent`

---

### 10.1 获取 Agent 列表

- **方法**：`GET`
- **路径**：`/api/agent/list`
- **响应示例**：

```json
{
  "code": 200,
  "data": [
    { "name": "requirements-analyst", "description": "需求分析 Agent", "enabled": true }
  ]
}
```

---

### 10.2 获取 Agent 详细列表（含统计和配置）

- **方法**：`GET`
- **路径**：`/api/agent/list/detail`

---

### 10.3 获取单个 Agent 详情

- **方法**：`GET`
- **路径**：`/api/agent/{agentName}`
- **路径参数**：`agentName` — Agent 名称

---

### 10.4 同步执行 Agent

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/execute`
- **路径参数**：`agentName` — Agent 名称
- **请求体（JSON）**：

```json
{
  "taskId": "task-uuid",
  "projectId": "proj-001",
  "userId": "user-001",
  "input": "分析以下需求：用户需要能够登录系统...",
  "outputPath": "./outputs",
  "autoSave": false
}
```

| 字段       | 类型    | 必填 | 默认值      | 说明                        |
|------------|---------|------|-------------|-----------------------------|
| taskId     | String  | 否   | 随机 UUID   | 任务 ID                     |
| projectId  | String  | 否   | -           | 所属项目 ID                 |
| userId     | String  | 否   | -           | 用户 ID                     |
| input      | Object  | 是   | -           | 输入内容（文本或对象）      |
| outputPath | String  | 否   | -           | 输出文件保存路径            |
| autoSave   | boolean | 否   | false       | 是否自动保存执行结果        |

- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "success": true,
    "agentName": "requirements-analyst",
    "taskId": "task-uuid",
    "result": "需求分析结果...",
    "data": { "savedPath": "./outputs/task-uuid.json" }
  }
}
```

---

### 10.5 带文件上传执行 Agent

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/execute-with-files`
- **Content-Type**：`multipart/form-data`
- **表单参数**：

| 参数       | 类型            | 必填 | 默认值    | 说明                   |
|------------|-----------------|------|-----------|------------------------|
| input      | String          | 是   | -         | 输入文本               |
| taskId     | String          | 否   | 随机 UUID | 任务 ID                |
| projectId  | String          | 否   | -         | 项目 ID                |
| userId     | String          | 否   | -         | 用户 ID                |
| outputPath | String          | 否   | -         | 输出路径               |
| autoSave   | boolean         | 否   | false     | 是否自动保存           |
| files      | MultipartFile[] | 否   | -         | 上传的文件（可多个）   |

- **说明**：文件内容会自动追加到 `input` 中提交给 Agent

---

### 10.6 流式执行 Agent（SSE）

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/stream`
- **Accept**：`text/event-stream`
- **请求体（JSON）**：

```json
{
  "taskId": "task-uuid",
  "projectId": "proj-001",
  "userId": "user-001",
  "input": "请生成用户登录模块的接口代码"
}
```

- **响应**：SSE 流，每帧为文字片段，最后推送 `[DONE]`

```
data: 正在分析需求...
data: 生成接口代码...
data: [DONE]
```

---

### 10.7 启动标准工作流

- **方法**：`POST`
- **路径**：`/api/agent/workflow/start`
- **请求体（JSON）**：

```json
{
  "projectId": "proj-001",
  "userId": "user-001",
  "input": "开发一个用户管理系统"
}
```

- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "workflowId": "wf-uuid",
    "status": "running",
    "currentStep": "requirements-analysis",
    "startTime": "2026-04-16T10:00:00"
  }
}
```

---

### 10.8 获取工作流状态

- **方法**：`GET`
- **路径**：`/api/agent/workflow/{workflowId}`
- **路径参数**：`workflowId` — 工作流 ID

---

### 10.9 获取 Agent 统计信息

- **方法**：`GET`
- **路径**：`/api/agent/{agentName}/stats`
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "totalExecutions": 42,
    "successCount": 40,
    "failCount": 2,
    "averageDuration": 3500,
    "enabled": true
  }
}
```

---

### 10.10 启用 Agent

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/enable`

---

### 10.11 禁用 Agent

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/disable`

---

### 10.12 获取 Agent 配置

- **方法**：`GET`
- **路径**：`/api/agent/{agentName}/config`
- **响应示例**：

```json
{
  "code": 200,
  "data": {
    "agentName": "requirements-analyst",
    "model": "glm-4.6",
    "temperature": 0.7,
    "maxTokens": 4000,
    "systemPrompt": "你是一个专业的需求分析工程师..."
  }
}
```

---

### 10.13 保存 Agent 配置

- **方法**：`POST`
- **路径**：`/api/agent/{agentName}/config`
- **请求体（JSON）**：与 10.12 响应的 `data` 结构相同

---

### 10.14 更新 Agent 配置（部分更新）

- **方法**：`PUT`
- **路径**：`/api/agent/{agentName}/config`
- **请求体（JSON）**：

```json
{
  "temperature": 0.5,
  "maxTokens": 8000
}
```

---

## 附录：统一响应格式

所有使用 `Result<T>` 包装的接口，响应结构如下：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段    | 类型   | 说明                              |
|---------|--------|-----------------------------------|
| code    | int    | 状态码，200 表示成功              |
| message | String | 提示信息                          |
| data    | Object | 响应数据（错误时为 null）         |

---

## 附录：模型名称参考

| 模型标识     | 厂商    | 路由规则                  |
|--------------|---------|---------------------------|
| glm-4.6      | 智谱AI  | 以 `glm` 开头 → 智谱AI    |
| glm-4-flash  | 智谱AI  | 以 `glm` 开头 → 智谱AI    |
| gpt-5.4-mini | 灵龙AI  | 以 `gpt` 开头 → CodeFlow  |
| linglong-*   | 灵龙AI  | 以 `linglong` 开头 → CodeFlow |
