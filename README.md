# AI Knowledge Agent

企业知识库 AI Agent 平台，提供用户认证、知识库管理、文档解析入库、RAG 问答、模型提供商配置、RAG 评测和企业服务台 Agent 能力。

## 技术栈

- 后端：Java 21、Spring Boot 3.2、MyBatis-Plus、Flyway、PostgreSQL/pgvector、Redis、MinIO、LangChain4j、Apache PDFBox、Apache POI
- 前端：React 18、TypeScript、Vite、Tailwind CSS、Zustand、Axios

## 后端目录结构

后端按业务模块优先组织代码。查找功能时先定位 `module/<业务>`，再看固定职责目录：

```text
src/main/java/com/fukang/knowledge/agent
├── common                 # 通用结果、异常、枚举、上下文
├── infrastructure          # 全局配置、拦截器、公共持久化支持
├── model                   # 跨模块基础模型，例如 BaseEntity
└── module
    ├── auth                # 登录、注册、会话
    ├── conversation        # RAG 会话和会话记忆
    ├── evaluation          # RAG 评测数据集、用例、运行结果
    ├── knowledge           # 知识库、文档、分块、解析、向量化入库
    ├── memory              # 用户长期记忆
    ├── model               # 模型提供商和模型配置管理
    ├── modelruntime        # 模型运行时客户端、动态模型创建、提示词模板
    ├── rag                 # RAG 问答、检索、重排、生成、流式输出
    └── servicedesk         # 企业服务台 Agent、工单、反馈
```

模块内目录约定：

```text
module/<业务>
├── controller   # Controller；只放 HTTP/SSE 入口类
├── service      # 业务编排、核心流程、接口/抽象
│   └── impl     # service 接口或策略抽象的实现类
├── model        # 模块内数据对象总入口，不再改名
│   ├── entity   # 继承 BaseEntity 的持久化实体
│   ├── dto      # 请求 DTO、业务命令对象
│   ├── resp     # Controller 对外响应对象
│   ├── vo       # 业务结果、只读值对象
│   ├── bo       # 有业务状态或业务行为的对象
│   ├── enums    # 模块内枚举
│   └── event    # 模块内业务事件
├── mapper       # MyBatis Mapper
└── integration  # 外部系统集成，例如 MinIO
```

命名约定：

- Java 类名不使用框架名前缀，例如不以 `LangChain4j`、`MyBatis` 开头。
- Controller 只放在 `controller` 包，`controller` 下不再放 DTO、Resp 或业务辅助类。
- Service 使用业务语义命名，例如 `DocumentService`、`RagService`、`ServiceDeskService`。
- Service 接口、策略抽象留在 `service`；对应实现放在 `service/impl` 或子领域 `service/<领域>/impl`。
- 不为了形式化分层给普通业务服务新增接口；只有确实有策略、多实现或外部客户端抽象时才拆接口和实现。
- `model` 保留为模块内数据对象的总目录；通过 `entity/dto/resp/vo/bo/event` 子包表达职责，不再额外改名。
- 继承 `BaseEntity` 的持久化模型统一使用 `Entity` 后缀，例如 `DocumentEntity`、`ServiceTicketEntity`，不再使用 `*DO` 后缀。
- Mapper 保留 `XxxMapper`，单表 CRUD 和简单查询都放在 Mapper，Service 直接依赖 Mapper。
- 不默认创建 Repository；需要跨 Mapper、外部存储或复杂持久化流程时，使用语义更明确的 `service/storage` 或 `integration`。
- 关键业务链路保留短注释，优先解释“为什么这样处理”和“流程阶段边界”。

## 核心功能入口

- 认证：`module/auth`
- 模型配置：`module/model`
- 模型运行时：`module/modelruntime`
- 知识库与文档入库：`module/knowledge`
- RAG 问答：`module/rag`
- RAG 评测：`module/evaluation`
- 服务台 Agent：`module/servicedesk`

## 本地环境

需要准备：

- JDK 21
- Maven 3.9+
- PostgreSQL 14+，并安装 pgvector
- Redis
- MinIO
- 可访问的 OpenAI 兼容模型服务

默认开发数据库：

```sql
CREATE SCHEMA IF NOT EXISTS knowledge_agent;
CREATE EXTENSION IF NOT EXISTS vector;
```

默认连接：

```text
jdbc:postgresql://127.0.0.1:5432/postgres?currentSchema=knowledge_agent
```

## 后端启动

```bash
mvn spring-boot:run
```

常用环境变量：

```text
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://127.0.0.1:5432/postgres?currentSchema=knowledge_agent
DB_USERNAME=postgres
DB_PASSWORD=root
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=knowledge-agent
VECTOR_STORE_DIMENSION=1024
RAG_TOP_K=8
RAG_SIMILARITY_THRESHOLD=0.6
```

## 前端启动

```bash
cd web
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:3000
```

## 常用验证命令

```bash
# 后端编译
mvn -DskipTests compile

# 后端测试
mvn test

# 前端检查
cd web
npm run check
npm run test
npm run build
```

当前完整后端测试需要本地 PostgreSQL 可连接，否则 Spring Boot 上下文测试会在 Flyway 初始化阶段失败。

## 主要 API

所有 `/api/**` 接口默认需要 `Authorization: Bearer <token>`，以下接口除外：

- `POST /api/auth/login`
- `POST /api/auth/register`

主要接口分组：

- `/api/auth`：登录、注册
- `/api/knowledge-bases`：知识库 CRUD
- `/api/documents`：文档上传、列表、详情、状态、删除
- `/api/models`：模型提供商和模型配置
- `/api/qa`：RAG 问答、会话、流式问答
- `/api/chunk-strategies`：分块策略配置
- `/api/evaluations`：RAG 评测
- `/api/service-desk`：服务台 Agent、工单、反馈
