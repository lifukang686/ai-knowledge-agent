export interface QaReq {
  question: string;
  knowledgeBaseId?: string;
  conversationId?: string;
}

export interface QaResp {
  answer: string;
  rewrittenQuery: string;
  status: 'success' | 'no_results' | 'error';
  conversationId?: string;
}

export interface QaStreamStage {
  stage: string;
  message: string;
}

export interface QaStreamToken {
  text: string;
}

export interface QaStreamHandlers {
  onStage?: (event: QaStreamStage) => void;
  onToken?: (event: QaStreamToken) => void;
  onDone?: (event: QaResp) => void;
  onError?: (event: { message: string }) => void;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  rewrittenQuery?: string;
  streaming?: boolean;
  streamStage?: string;
  streamStageMessage?: string;
}

export interface QaConversation {
  id: string;
  knowledgeBaseId?: string;
  title: string;
  status: string;
  messageCount: number;
  lastMessageAt?: string;
  createTime?: string;
  updateTime?: string;
}

export interface QaConversationMessage {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant';
  content: string;
  rewrittenQuery?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}
