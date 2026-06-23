import { ListResponse } from './common';

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

export interface EvaluationDatasetReq {
  name: string;
  description?: string;
  knowledgeBaseId?: string;
}

export interface EvaluationCaseReq {
  question: string;
  expectedAnswer?: string;
  expectedKeywords?: string[];
  expectedChunkIds?: string[];
  expectedStatus?: string;
  metadata?: string;
  enabled?: boolean;
}

export type EvaluationDatasetListResponse = ListResponse<EvaluationDataset>;
export type EvaluationCaseListResponse = ListResponse<EvaluationCase>;
export type EvaluationCaseResultListResponse = ListResponse<EvaluationCaseResult>;
