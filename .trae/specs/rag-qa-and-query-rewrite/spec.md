# RAG 问答与查询改写 Spec

## Why
当前系统已完成知识库管理模块（文档上传 → 解析 → 分块 → 嵌入 → 索引），但缺少 RAG 检索模块的核心功能：用户提问 API 和查询改写（Query Rewriting）。根据详细设计文档第 3.3 节定义，RAG 检索模块需要接收用户问题、可选地对查询进行改写以提高检索命中率，并为后续的语义检索和答案生成提供基础。

## What Changes
- 新增 `POST /api/qa` 接口，接收用户自然语言问题并返回初步响应占位
- 新增 `rag/` 包，封装 RAG 模块核心逻辑（查询改写服务、RAG 编排服务）
- 实现查询改写（Query Rewriting）功能：调用 LLM 对原始用户查询进行同义词替换、语义扩展，生成更适合向量检索的改写查询
- 新增 `QueryRewriteService`，基于现有 Spring AI `AiCallService` + `DynamicModelManager` 实现
- 查询改写策略采用简单提示词模板（Prompt Template），无需引入新框架依赖
- 新增相关 DTO、枚举、单元测试

## Impact
- Affected specs: 无（新功能）
- Affected code: 
  - 新增 `rag/` 包（`QueryRewriteService.java`, `RagAppService.java`）
  - 新增 `api/qa/` 包（`QaController.java`, `QaReq.java`, `QaResp.java`）
  - 修改 `ErrorCodeEnum.java`（新增 RAG 相关错误码）
  - 新增单元测试类

## ADDED Requirements

### Requirement: 用户问题接收 API
系统 SHALL 提供 `POST /api/qa` 接口，接收用户自然语言问题并返回响应。

#### Scenario: 用户提交问题成功
- **WHEN** 用户通过 `POST /api/qa` 发送 `{ "question": "如何申请年假？", "knowledgeBaseId": 1, "conversationId": null }`
- **THEN** 系统返回 `{ "answer": "...", "rewrittenQuery": "...", "status": "success" }`

#### Scenario: 问题为空
- **WHEN** 用户发送 `{ "question": "", "knowledgeBaseId": 1 }`
- **THEN** 系统返回 HTTP 400 错误，错误信息提示"问题不能为空"

#### Scenario: 知识库不存在
- **WHEN** 用户发送 `{ "question": "如何申请年假？", "knowledgeBaseId": 99999 }`
- **THEN** 系统返回 HTTP 404 错误

### Requirement: 查询改写（Query Rewriting）
系统 SHALL 调用聊天模型对原始用户查询进行改写，生成更适合向量语义检索的扩展查询文本。改写策略应保持原始查询意图不变，同时增加同义词、相关术语和不同表达方式。

#### Scenario: 查询改写成功
- **WHEN** 原始查询为 "年假怎么申请"，系统调用查询改写服务
- **THEN** 系统返回改写后的查询文本（如 "如何申请年假？休假申请流程是什么？"），且改写查询长度合理（不超过 500 字符）

#### Scenario: 查询改写失败（模型不可用）
- **WHEN** 调用查询改写时聊天模型返回异常
- **THEN** 系统回退使用原始查询继续后续流程，并在日志中记录 WARN 级别信息

#### Scenario: 短查询跳过改写
- **WHEN** 原始查询字符数 ≤ 3（如 "年假"）
- **THEN** 系统直接使用原始查询，记录 DEBUG 日志"查询过短，跳过改写"

### Requirement: 技术选型——优先使用现有 Spring AI 基础设施
由于 langchain4j 未引入项目，而 Spring AI 已有完善的 `AiCallService` + `DynamicModelManager` + `SpringAiClientImpl` 调用链路，查询改写功能 SHALL 基于现有 Spring AI 基础设施实现，无需引入新框架依赖。

#### Scenario: 使用现有 AiCallService 调用聊天模型
- **WHEN** 执行查询改写
- **THEN** 系统通过 `AiCallService` 调用聊天模型（由 `ModelResolutionService` 解析默认提供商），传入改写提示词模板

### Requirement: 提示词模板管理
查询改写提示词 SHALL 存放在 `src/main/resources/prompts/query-rewrite.st`（StringTemplate 格式），便于版本管理和复用。提示词内容应包含角色设定（"你是一个搜索引擎优化专家..."）、改写规则（保持原意、扩展同义词、最多生成 3 个变体）和输出格式要求。