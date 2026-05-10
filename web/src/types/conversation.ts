import { BaseEntity, PaginationParams } from './common';

export interface Conversation extends BaseEntity {
  title: string;
  knowledge_base_id: string;
  status: 'active' | 'completed' | 'archived';
  message_count: number;
  last_message_at?: string;
}

export interface Message extends BaseEntity {
  conversation_id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  metadata?: Record<string, any>;
  parent_message_id?: string;
  sources?: MessageSource[];
}

export interface MessageSource {
  document_id: string;
  document_name: string;
  chunk_id: string;
  content: string;
  score: number;
}

export interface ConversationCreateRequest {
  title: string;
  knowledge_base_id: string;
}

export interface ConversationQueryParams extends PaginationParams {
  knowledge_base_id?: string;
  status?: string;
  title?: string;
}

export interface MessageCreateRequest {
  conversation_id: string;
  content: string;
  role: 'user' | 'assistant';
  parent_message_id?: string;
}

export interface RAGQueryRequest {
  knowledge_base_id: string;
  query: string;
  conversation_id?: string;
  top_k?: number;
  threshold?: number;
}