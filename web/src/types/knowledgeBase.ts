import { BaseEntity, StatusType, ListResponse } from './common';

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  document_count?: number;
  status?: StatusType;
  created_at: string;
  updated_at: string;
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

export interface DocumentDetail {
  id: string;
  title: string;
  content: string;
  file_path: string;
  knowledge_base_id: string;
  status: StatusType;
  uploaded_by?: string;
  chunk_count: number;
  file_size: number;
  created_at: string;
  updated_at: string;
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

export interface KnowledgeBaseApiResp {
  id: string;
  name: string;
  description: string | null;
  documentCount: number;
  status: string;
  createTime: string;
  updateTime: string;
}

export interface DocumentApiResp {
  id: string;
  name: string;
  filePath: string;
  knowledgeBaseId: string;
  status: string;
  uploadedBy: string | null;
  chunkCount: number;
  fileSize: number;
  createTime: string;
  updateTime: string;
}

export interface DocumentDetailApiResp {
  id: string;
  title: string;
  content: string;
  filePath: string;
  knowledgeBaseId: string;
  status: string;
  uploadedBy: string | null;
  chunkCount: number;
  fileSize: number;
  createTime: string;
  updateTime: string;
}

export interface KnowledgeBaseListResponse extends ListResponse<KnowledgeBase> {}