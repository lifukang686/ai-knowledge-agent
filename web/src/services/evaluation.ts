import { request } from '@/utils/request';
import {
  EvaluationCase,
  EvaluationCaseListResponse,
  EvaluationCaseReq,
  EvaluationCaseResult,
  EvaluationCaseResultListResponse,
  EvaluationDataset,
  EvaluationDatasetListResponse,
  EvaluationDatasetReq,
  EvaluationRun,
} from '@/types/evaluation';

/** 将后端 Long/number ID 统一转成前端字符串 ID。 */
const toId = (value: unknown): string => (value != null ? String(value) : '');
/** 可空 ID 转换，空值保持 undefined。 */
const toOptionalId = (value: unknown): string | undefined => (value != null ? String(value) : undefined);

/** 评测集响应归一化。 */
const mapDataset = (api: any): EvaluationDataset => ({
  id: toId(api?.id),
  name: api?.name ?? '',
  description: api?.description ?? undefined,
  knowledgeBaseId: toOptionalId(api?.knowledgeBaseId),
  targetType: api?.targetType ?? 'RAG_QA',
  caseCount: api?.caseCount ?? 0,
  lastRunId: toOptionalId(api?.lastRunId),
  lastRunStatus: api?.lastRunStatus ?? undefined,
  lastAvgScore: api?.lastAvgScore ?? undefined,
  createTime: api?.createTime,
  updateTime: api?.updateTime,
});

/** 评测用例响应归一化。 */
const mapCase = (api: any): EvaluationCase => ({
  id: toId(api?.id),
  datasetId: toId(api?.datasetId),
  question: api?.question ?? '',
  expectedAnswer: api?.expectedAnswer ?? undefined,
  expectedKeywords: Array.isArray(api?.expectedKeywords) ? api.expectedKeywords : [],
  expectedChunkIds: Array.isArray(api?.expectedChunkIds) ? api.expectedChunkIds.map(String) : [],
  expectedStatus: api?.expectedStatus ?? undefined,
  metadata: api?.metadata ?? undefined,
  enabled: api?.enabled !== false,
  createTime: api?.createTime,
  updateTime: api?.updateTime,
});

/** 评测运行响应归一化。 */
const mapRun = (api: any): EvaluationRun => ({
  id: toId(api?.id),
  datasetId: toId(api?.datasetId),
  name: api?.name ?? '',
  targetType: api?.targetType ?? 'RAG_QA',
  status: api?.status ?? '',
  totalCount: api?.totalCount ?? 0,
  passedCount: api?.passedCount ?? 0,
  failedCount: api?.failedCount ?? 0,
  avgScore: api?.avgScore ?? undefined,
  avgLatencyMs: api?.avgLatencyMs ?? undefined,
  startedAt: api?.startedAt,
  endedAt: api?.endedAt,
  errorMessage: api?.errorMessage ?? undefined,
  createTime: api?.createTime,
  updateTime: api?.updateTime,
});

/** 单条评测结果响应归一化。 */
const mapCaseResult = (api: any): EvaluationCaseResult => ({
  id: toId(api?.id),
  runId: toId(api?.runId),
  caseId: toId(api?.caseId),
  question: api?.question ?? '',
  expectedAnswer: api?.expectedAnswer ?? undefined,
  actualAnswer: api?.actualAnswer ?? undefined,
  rewrittenQuery: api?.rewrittenQuery ?? undefined,
  expectedStatus: api?.expectedStatus ?? undefined,
  actualStatus: api?.actualStatus ?? undefined,
  expectedKeywords: Array.isArray(api?.expectedKeywords) ? api.expectedKeywords : [],
  expectedChunkIds: Array.isArray(api?.expectedChunkIds) ? api.expectedChunkIds.map(String) : [],
  retrievedChunks: Array.isArray(api?.retrievedChunks) ? api.retrievedChunks.map(mapChunk) : [],
  rerankedChunks: Array.isArray(api?.rerankedChunks) ? api.rerankedChunks.map(mapChunk) : [],
  retrievalHitScore: api?.retrievalHitScore ?? undefined,
  keywordScore: api?.keywordScore ?? undefined,
  statusScore: api?.statusScore ?? undefined,
  totalScore: api?.totalScore ?? 0,
  passed: api?.passed === true,
  metricDetail: api?.metricDetail ?? {},
  latencyMs: api?.latencyMs ?? undefined,
  errorMessage: api?.errorMessage ?? undefined,
  createTime: api?.createTime,
});

/** 召回片段响应归一化。 */
const mapChunk = (api: any) => ({
  chunkId: toId(api?.chunkId),
  chunkText: api?.chunkText ?? '',
  similarity: api?.similarity ?? 0,
  metadata: api?.metadata ?? undefined,
  vectorScore: api?.vectorScore ?? undefined,
  bm25Score: api?.bm25Score ?? undefined,
  rrfScore: api?.rrfScore ?? undefined,
  rerankScore: api?.rerankScore ?? undefined,
});

/** 创建/更新评测集请求归一化。 */
const normalizeDatasetReq = (data: EvaluationDatasetReq) => ({
  name: data.name,
  description: data.description || undefined,
  knowledgeBaseId: data.knowledgeBaseId ? Number(data.knowledgeBaseId) : undefined,
});

/** 创建/更新用例请求归一化。 */
const normalizeCaseReq = (data: EvaluationCaseReq) => ({
  question: data.question,
  expectedAnswer: data.expectedAnswer || undefined,
  expectedKeywords: data.expectedKeywords || [],
  expectedChunkIds: (data.expectedChunkIds || []).filter(Boolean).map(Number),
  expectedStatus: data.expectedStatus || undefined,
  metadata: data.metadata || undefined,
  enabled: data.enabled !== false,
});

/** RAG 评测中心接口封装。 */
export const evaluationService = {
  /** 分页查询评测集。 */
  async listDatasets(params: {
    page?: number;
    pageSize?: number;
    keyword?: string;
    knowledgeBaseId?: string;
  }): Promise<EvaluationDatasetListResponse> {
    const response = await request.get<any>('/evaluations/datasets', {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || 20,
        keyword: params.keyword || undefined,
        knowledgeBaseId: params.knowledgeBaseId || undefined,
      },
    });
    return {
      items: (response?.items || []).map(mapDataset),
      total: response?.total ?? 0,
      page: response?.page ?? 1,
      pageSize: response?.pageSize ?? 20,
    };
  },

  /** 创建评测集。 */
  async createDataset(data: EvaluationDatasetReq): Promise<string> {
    const id = await request.post('/evaluations/datasets', normalizeDatasetReq(data));
    return toId(id);
  },

  /** 查询评测集详情。 */
  async getDataset(id: string): Promise<EvaluationDataset> {
    return mapDataset(await request.get(`/evaluations/datasets/${id}`));
  },

  /** 更新评测集。 */
  async updateDataset(id: string, data: EvaluationDatasetReq): Promise<void> {
    await request.put(`/evaluations/datasets/${id}`, normalizeDatasetReq(data));
  },

  /** 删除评测集。 */
  async deleteDataset(id: string): Promise<void> {
    await request.delete(`/evaluations/datasets/${id}`);
  },

  /** 分页查询评测用例。 */
  async listCases(datasetId: string, params: { page?: number; pageSize?: number }): Promise<EvaluationCaseListResponse> {
    const response = await request.get<any>(`/evaluations/datasets/${datasetId}/cases`, {
      params: { page: params.page || 1, pageSize: params.pageSize || 20 },
    });
    return {
      items: (response?.items || []).map(mapCase),
      total: response?.total ?? 0,
      page: response?.page ?? 1,
      pageSize: response?.pageSize ?? 20,
    };
  },

  /** 创建评测用例。 */
  async createCase(datasetId: string, data: EvaluationCaseReq): Promise<string> {
    const id = await request.post(`/evaluations/datasets/${datasetId}/cases`, normalizeCaseReq(data));
    return toId(id);
  },

  /** 更新评测用例。 */
  async updateCase(id: string, data: EvaluationCaseReq): Promise<void> {
    await request.put(`/evaluations/cases/${id}`, normalizeCaseReq(data));
  },

  /** 删除评测用例。 */
  async deleteCase(id: string): Promise<void> {
    await request.delete(`/evaluations/cases/${id}`);
  },

  /** 手动运行评测集。 */
  async runDataset(datasetId: string): Promise<string> {
    const response = await request.post<any>(`/evaluations/datasets/${datasetId}/runs`);
    return toId(response?.runId);
  },

  /** 查询运行汇总。 */
  async getRun(runId: string): Promise<EvaluationRun> {
    return mapRun(await request.get(`/evaluations/runs/${runId}`));
  },

  /** 分页查询运行明细。 */
  async listRunResults(runId: string, params: { page?: number; pageSize?: number }): Promise<EvaluationCaseResultListResponse> {
    const response = await request.get<any>(`/evaluations/runs/${runId}/results`, {
      params: { page: params.page || 1, pageSize: params.pageSize || 20 },
    });
    return {
      items: (response?.items || []).map(mapCaseResult),
      total: response?.total ?? 0,
      page: response?.page ?? 1,
      pageSize: response?.pageSize ?? 20,
    };
  },
};
