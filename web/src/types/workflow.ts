import { BaseEntity } from './common';

export interface Workflow extends BaseEntity {
  name: string;
  description?: string;
  definition: WorkflowDefinition;
  status: 'active' | 'inactive';
  version: number;
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  startNodeId: string;
  endNodeIds: string[];
}

export interface WorkflowNode {
  id: string;
  type: 'start' | 'end' | 'agent' | 'tool' | 'condition' | 'parallel';
  name: string;
  config: Record<string, any>;
  nextNodeIds: string[];
  position?: {
    x: number;
    y: number;
  };
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  condition?: string;
}

export interface WorkflowRun extends BaseEntity {
  workflowId: string;
  workflowName: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
  input: Record<string, any>;
  output?: Record<string, any>;
  error?: string;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
  currentNodeId?: string;
  nodeExecutions: NodeExecution[];
}

export interface NodeExecution extends BaseEntity {
  workflowRunId: string;
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  input: Record<string, any>;
  output?: Record<string, any>;
  error?: string;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
}

export interface CreateWorkflowRequest {
  name: string;
  description?: string;
  definition: WorkflowDefinition;
}

export interface UpdateWorkflowRequest {
  name?: string;
  description?: string;
  definition?: WorkflowDefinition;
  status?: 'active' | 'inactive';
}

export interface RunWorkflowRequest {
  input: Record<string, any>;
}

export interface WorkflowQuery {
  name?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface WorkflowRunQuery {
  workflowId?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}