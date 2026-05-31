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
