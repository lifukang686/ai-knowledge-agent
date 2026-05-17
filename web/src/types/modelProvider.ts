export interface ModelProvider {
  id: string;
  name: string;
  apiBaseUrl?: string;
  apiKey?: string;
  description?: string;
  isDefault?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface ModelConfig {
  id: string;
  providerId: string;
  modelName: string;
  modelType?: string;
  defaultParams?: string;
  createTime?: string;
  updateTime?: string;
}

export const MODEL_TYPE_LABELS: Record<string, string> = {
  CHAT: '对话模型',
  EMBEDDING: '嵌入模型',
  RERANK: '重排序模型',
  STT: '语音转文字模型',
};

export const MODEL_TYPE_OPTIONS = [
  { value: 'CHAT', label: '对话模型 (CHAT)' },
  { value: 'EMBEDDING', label: '嵌入模型 (EMBEDDING)' },
  { value: 'RERANK', label: '重排序模型 (RERANK)' },
  { value: 'STT', label: '语音转文字模型 (STT)' },
];

export interface CreateModelProviderRequest {
  name: string;
  apiBaseUrl?: string;
  apiKey?: string;
  description?: string;
}

export interface UpdateModelProviderRequest {
  name?: string;
  apiBaseUrl?: string;
  apiKey?: string;
  description?: string;
}

export interface CreateModelConfigRequest {
  providerId: string;
  modelName: string;
  modelType: string;
  defaultParams?: string;
}

export interface UpdateModelConfigRequest {
  modelName?: string;
  modelType?: string;
  defaultParams?: string;
}

export interface ModelProviderQuery {
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

import { BaseEntity } from './common';