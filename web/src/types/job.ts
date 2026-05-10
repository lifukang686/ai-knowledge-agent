import { BaseEntity, PaginationParams } from './common';

export interface Job extends BaseEntity {
  name: string;
  description?: string;
  job_type: 'agent' | 'workflow' | 'document_processing';
  status: 'enabled' | 'disabled' | 'running';
  schedule: string; // Cron expression
  config: Record<string, any>;
  last_run_at?: string;
  next_run_at?: string;
  run_count: number;
  success_count: number;
  failure_count: number;
  created_by: string;
}

export interface JobRun extends BaseEntity {
  job_id: string;
  job_name: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
  start_time?: string;
  end_time?: string;
  duration?: number; // seconds
  logs?: string;
  error_message?: string;
  result?: Record<string, any>;
}

export interface JobCreateRequest {
  name: string;
  description?: string;
  job_type: 'agent' | 'workflow' | 'document_processing';
  schedule: string;
  config: Record<string, any>;
}

export interface JobUpdateRequest extends Partial<JobCreateRequest> {
  status?: 'enabled' | 'disabled';
}

export interface JobQueryParams extends PaginationParams {
  name?: string;
  job_type?: string;
  status?: string;
  created_by?: string;
}

export interface JobRunQueryParams extends PaginationParams {
  job_id?: string;
  status?: string;
  start_time_from?: string;
  start_time_to?: string;
}