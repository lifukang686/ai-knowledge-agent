import { BaseEntity } from './common';

export interface Tool extends BaseEntity {
  name: string;
  description?: string;
  executor_type: 'http' | 'function' | 'script';
  executor_config: Record<string, any>;
  schema?: Record<string, any>;
  tags?: string[];
}

export interface CreateToolReq {
  name: string;
  description: string;
  executorType: 'HTTP' | 'SQL' | 'LOCAL_METHOD';
  executorConfig: string;
  parametersSchema: string;
}

export interface ToolItem {
  id: string;
  name: string;
  description: string;
  executorType: string;
  executorConfig: string;
  parametersSchema: string;
  enabled: boolean;
}

export interface UpdateToolReq {
  name: string;
  description: string;
  executorType: string;
  executorConfig: string;
  parametersSchema: string;
}
