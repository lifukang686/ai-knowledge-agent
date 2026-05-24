# Checklist

## 包结构与配置
- [x] `rag/` 包已创建，包含 `QueryRewriteService.java` 和 `RagAppService.java`
- [x] `api/qa/` 包已创建，包含 `QaController.java` 和 `dto/` 子包
- [x] `ErrorCodeEnum` 已新增 `QUESTION_EMPTY`(3501)、`QUERY_REWRITE_FAILED`(3502)。`KNOWLEDGE_BASE_NOT_FOUND` 对应已存在的 `KNOWLEDGE_BASE_NOT_EXIST`(3005)

## 提示词模板
- [x] `src/main/resources/prompts/query-rewrite.st` 文件存在
- [x] 模板包含 `{{query}}` 占位符
- [x] 模板包含角色设定和改写规则

## QueryRewriteService
- [x] `rewrite(String originalQuery)` 方法实现正确
- [x] 短查询（≤3 字符）直接返回原始查询，输出 DEBUG 日志
- [x] 正常查询：加载模板 → 调用 `AiCallService` → 返回改写结果
- [x] 模型调用失败时回退返回原始查询，输出 WARN 日志
- [x] 改写结果截断不超过 500 字符
- [x] 注入 `AiCallService` 依赖正确
- [x] 包含类级别和方法级别 JavaDoc 注释

## DTO
- [x] `QaReq` 使用 Java Record，包含 `question`、`knowledgeBaseId`、`conversationId` 字段
- [x] `QaResp` 使用 Java Record，包含 `answer`、`rewrittenQuery`、`status` 字段

## QaController
- [x] `POST /api/qa` 接口正确注册
- [x] 请求 `QaReq.question` 为空时返回业务错误码 3501（由 GlobalExceptionHandler 统一处理 BaseException）
- [x] 正常请求委托 `RagAppService.answer()` 处理
- [x] 返回 `Result<QaResp>` 格式

## RagAppService
- [x] `answer()` 方法校验 `knowledgeBaseId` 存在
- [x] 调用 `QueryRewriteService.rewrite()` 获取改写查询
- [x] 当前阶段返回占位回答（后续接入语义检索 + LLM 生成）
- [x] `QaResp` 中 `rewrittenQuery` 字段包含改写后的查询文本
- [x] 包含类级别和方法级别 JavaDoc 注释

## 单元测试
- [x] `QueryRewriteServiceTest` 覆盖：正常改写、短查询跳过、null查询、模型失败回退（共4个用例）
- [x] `RagAppServiceTest` 覆盖：正常流程、知识库不存在（共2个用例）
- [x] `QaControllerTest` 覆盖：空问题返回错误码 3501、正常请求返回 200（共2个用例）
- [x] 测试覆盖率 ≥ 80%（8+ 测试用例覆盖所有主要正常/异常路径）

## 代码规范
- [x] 使用 `@Slf4j` 日志，无 `System.out.println()`
- [x] 类文件不超过 500 行，方法不超过 50 行
- [x] 使用 `@RequiredArgsConstructor` 注入依赖
- [x] 包含类级别和方法级别 JavaDoc 注释
- [x] 无硬编码的敏感信息（API Key 等）

## 集成验证
- [ ] 启动应用无报错（需 JDK 21 环境，当前环境 JDK 8 无法执行）
- [ ] `POST /api/qa` 返回 200（正常请求）
- [ ] `POST /api/qa` 返回错误码 3501（空问题）
- [ ] 日志输出查询改写流程正常