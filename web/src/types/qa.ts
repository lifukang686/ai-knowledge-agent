export interface QaReq {
  question: string;
  knowledgeBaseId?: string;
  conversationId?: string;
}

export interface QaResp {
  answer: string;
  rewrittenQuery: string;
  status: 'success' | 'no_results' | 'error';
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  rewrittenQuery?: string;
}