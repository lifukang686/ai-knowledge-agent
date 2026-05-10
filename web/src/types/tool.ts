export interface Tool extends BaseEntity {
  name: string;
  description?: string;
  executor_type: 'http' | 'function' | 'script';
  executor_config: Record<string, any>;
  schema?: Record<string, any>;
  tags?: string[];
}

export interface ToolCreateRequest {
  name: string;
  description?: string;
  executor_type: 'http' | 'function' | 'script';
  executor_config: Record<string, any>;
  schema?: Record<string, any>;
  tags?: string[];
}

export interface ToolUpdateRequest extends Partial<ToolCreateRequest> {}

export interface ToolQueryParams extends PaginationParams {
  name?: string;
  executor_type?: string;
  tags?: string[];
}

import { BaseEntity, PaginationParams } from './common';