export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

export const DEFAULT_PAGE_SIZE = 20;

export const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'completed', label: '已完成' },
  { value: 'failed', label: '失败' },
];

export const API_ENDPOINTS = {
  // 知识库相关
  KNOWLEDGE_BASES: '/knowledge-bases',
  KNOWLEDGE_BASE_DETAIL: (id: string) => `/knowledge-bases/${id}`,
  
  // 文档相关
  DOCUMENTS: '/documents',
  DOCUMENT_DETAIL: (id: string) => `/documents/${id}`,
  DOCUMENT_DETAIL_API: (id: string) => `/documents/${id}/detail`,
  DOCUMENT_STATUS: (id: string) => `/documents/${id}/status`,
  UPLOAD_DOCUMENT: '/documents/upload',
  
  // 模型提供商相关
  MODEL_PROVIDERS: '/model-providers',
  MODEL_PROVIDER_DETAIL: (id: string) => `/model-providers/${id}`,
  MODEL_PROVIDER_MODELS: (id: string) => `/model-providers/${id}/models`,
  
  // 工具相关
  TOOLS: '/tools',
  TOOL_DETAIL: (id: string) => `/tools/${id}`,
  
  // Agent相关
  AGENTS: '/agents',
  AGENT_DETAIL: (id: string) => `/agents/${id}`,
  AGENT_RUNS: (id: string) => `/agents/${id}/runs`,
  AGENT_RUN_DETAIL: (runId: string) => `/agents/runs/${runId}`,
  
  // 工作流相关
  WORKFLOWS: '/workflows',
  WORKFLOW_DETAIL: (id: string) => `/workflows/${id}`,
  WORKFLOW_RUNS: '/workflow-runs',
  WORKFLOW_RUN_DETAIL: (runId: string) => `/workflow-runs/${runId}`,
  
  // 调度任务相关
  SCHEDULER_JOBS: '/scheduler/jobs',
  SCHEDULER_JOB_DETAIL: (id: string) => `/scheduler/jobs/${id}`,
  
  // RAG问答相关
  QA_ASK: '/qa/ask',
  CONVERSATIONS: '/conversations',
  CONVERSATION_DETAIL: (id: string) => `/conversations/${id}`,
} as const;