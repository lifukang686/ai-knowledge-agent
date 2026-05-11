export interface ModelProvider {
  id: string;
  name: string;
  apiBaseUrl?: string;
  apiKey?: string;
  description?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ModelConfig {
  id: string;
  providerId: string;
  modelName: string;
  defaultParams?: string;
  createTime?: string;
  updateTime?: string;
}

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
  defaultParams?: string;
}

export interface UpdateModelConfigRequest {
  modelName?: string;
  defaultParams?: string;
}

export interface ModelProviderQuery {
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

import { BaseEntity } from './common';