import { BaseEntity } from './common';

export interface Agent extends BaseEntity {
  name: string;
  description?: string;
  toolIds: string[];
  modelProviderId: string;
  modelId: string;
  systemPrompt?: string;
  status: 'active' | 'inactive';
}

export interface AgentRun extends BaseEntity {
  agentId: string;
  task: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  result?: string;
  error?: string;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
}

export interface CreateAgentRequest {
  name: string;
  description?: string;
  toolIds: string[];
  modelProviderId: string;
  modelId: string;
  systemPrompt?: string;
}

export interface UpdateAgentRequest {
  name?: string;
  description?: string;
  toolIds?: string[];
  modelProviderId?: string;
  modelId?: string;
  systemPrompt?: string;
  status?: 'active' | 'inactive';
}

export interface RunAgentRequest {
  task: string;
}

export interface AgentQuery {
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface AgentRunQuery {
  agentId?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}