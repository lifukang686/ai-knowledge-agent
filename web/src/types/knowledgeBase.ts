import { BaseEntity, StatusType } from './common';

export interface KnowledgeBase extends BaseEntity {
  name: string;
  description?: string;
  document_count?: number;
  status?: StatusType;
}

export interface Document extends BaseEntity {
  name: string;
  file_path: string;
  knowledge_base_id: string;
  status: StatusType;
  uploaded_by?: string;
  chunk_count?: number;
  file_size?: number;
}

export interface DocumentChunk {
  id: string;
  document_id: string;
  content: string;
  chunk_index: number;
  token_count: number;
  embedding_status: StatusType;
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

export interface UpdateKnowledgeBaseRequest {
  name?: string;
  description?: string;
}