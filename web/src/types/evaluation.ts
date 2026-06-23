import { ListResponse } from './common';

/** RAG 评测集。 */
export interface EvaluationDataset {
  id: string;
  name: string;
  description?: string;
  knowledgeBaseId?: string;
  targetType: string;
  caseCount: number;
  lastRunId?: string;
  lastRunStatus?: string;
  lastAvgScore?: number;
  createTime?: string;
  updateTime?: string;
}

/** RAG 评测用例。 */
export interface EvaluationCase {
  id: string;
  datasetId: string;
  question: string;
  expectedAnswer?: string;
  expectedKeywords: string[];
  expectedChunkIds: string[];
  expectedStatus?: string;
  metadata?: string;
  enabled: boolean;
  createTime?: string;
  updateTime?: string;
}

/** RAG 评测运行汇总。 */
export interface EvaluationRun {
  id: string;
  datasetId: string;
  name: string;
  targetType: string;
  status: string;
  totalCount: number;
  passedCount: number;
  failedCount: number;
  avgScore?: number;
  avgLatencyMs?: number;
  startedAt?: string;
  endedAt?: string;
  errorMessage?: string;
  createTime?: string;
  updateTime?: string;
}

/** 评测结果中的召回片段。 */
export interface EvaluationChunk {
  chunkId: string;
  chunkText: string;
  similarity: number;
  metadata?: string;
  vectorScore?: number;
  bm25Score?: number;
  rrfScore?: number;
  rerankScore?: number;
}

/** 单条用例运行结果。 */
export interface EvaluationCaseResult {
  id: string;
  runId: string;
  caseId: string;
  question: string;
  expectedAnswer?: string;
  actualAnswer?: string;
  rewrittenQuery?: string;
  expectedStatus?: string;
  actualStatus?: string;
  expectedKeywords: string[];
  expectedChunkIds: string[];
  retrievedChunks: EvaluationChunk[];
  rerankedChunks: EvaluationChunk[];
  retrievalHitScore?: number;
  keywordScore?: number;
  statusScore?: number;
  totalScore: number;
  passed: boolean;
  metricDetail: Record<string, unknown>;
  latencyMs?: number;
  errorMessage?: string;
  createTime?: string;
}

/** 创建/更新评测集请求。 */
export interface EvaluationDatasetReq {
  name: string;
  description?: string;
  knowledgeBaseId?: string;
}

/** 创建/更新评测用例请求。 */
export interface EvaluationCaseReq {
  question: string;
  expectedAnswer?: string;
  expectedKeywords?: string[];
  expectedChunkIds?: string[];
  expectedStatus?: string;
  metadata?: string;
  enabled?: boolean;
}

/** 评测集分页响应。 */
export type EvaluationDatasetListResponse = ListResponse<EvaluationDataset>;
/** 评测用例分页响应。 */
export type EvaluationCaseListResponse = ListResponse<EvaluationCase>;
/** 单条评测结果分页响应。 */
export type EvaluationCaseResultListResponse = ListResponse<EvaluationCaseResult>;
