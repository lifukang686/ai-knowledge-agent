export interface BaseEntity {
  id: string;
  created_at: string;
  updated_at: string;
}

export interface PaginationParams {
  page: number;
  pageSize: number;
  total?: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface ListResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export type StatusType = 'pending' | 'processing' | 'completed' | 'failed' | 'unknown';

export interface SearchParams {
  keyword?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}