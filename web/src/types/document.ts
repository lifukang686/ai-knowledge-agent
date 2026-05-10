import { BaseEntity, PaginationParams } from './common';

export interface Document extends BaseEntity {
  name: string;
  file_path: string;
  file_size: number;
  file_type: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  knowledge_base_id: string;
  uploaded_by: string;
  processing_progress?: number;
  error_message?: string;
  chunks_count?: number;
  indexed_chunks_count?: number;
}

export interface DocumentChunk {
  id: string;
  document_id: string;
  content: string;
  chunk_index: number;
  token_count: number;
  embedding_status: 'pending' | 'processing' | 'completed' | 'failed';
  indexed_at?: string;
}

export interface DocumentCreateRequest {
  knowledgeBaseId: string;
  file: File;
}

export interface DocumentQueryParams extends PaginationParams {
  knowledge_base_id?: string;
  status?: string;
  name?: string;
}

export interface DocumentUploadResponse {
  documentId: string;
  status: string;
}