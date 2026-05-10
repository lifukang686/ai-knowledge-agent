export interface ModelProvider extends BaseEntity {
  name: string;
  apiUrl: string;
  apiKey: string;
  description?: string;
  defaultParams?: Record<string, any>;
  status: 'active' | 'inactive';
}

export interface Model extends BaseEntity {
  providerId: string;
  name: string;
  modelType: 'chat' | 'embedding' | 'completion';
  description?: string;
  maxTokens?: number;
  status: 'active' | 'inactive';
}

export interface CreateModelProviderRequest {
  name: string;
  apiUrl: string;
  apiKey: string;
  description?: string;
  defaultParams?: Record<string, any>;
}

export interface UpdateModelProviderRequest {
  name?: string;
  apiUrl?: string;
  apiKey?: string;
  description?: string;
  defaultParams?: Record<string, any>;
  status?: 'active' | 'inactive';
}

export interface ModelProviderQuery {
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

import { BaseEntity } from './common';